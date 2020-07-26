package com.github.alex1304.rdi.resolver;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.alex1304.rdi.ServiceReference;
import com.github.alex1304.rdi.config.ServiceDescriptor;
import com.github.alex1304.rdi.config.SetterMethod;

import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import reactor.util.context.Context;

public class DependencyResolver {
	
	private DependencyResolver() {
		throw new AssertionError();
	}
	
	public static Map<ServiceReference<?>, Mono<Object>> resolve(Set<ServiceDescriptor> serviceDescriptors) {
		Map<ServiceReference<?>, ResolutionContext> resolutionContextByRef = serviceDescriptors.stream()
				.map(ResolutionContext::new)
				.collect(Collectors.toMap(rctx -> rctx.getReference(), Function.identity()));
		Queue<ServiceReference<?>> stack = Collections.asLifoQueue(new ArrayDeque<>());
		HashSet<ServiceReference<?>> circularFactoryDepDetector = new HashSet<>();
		Map<RefWithParent, CircularInstantiationDetector> instHierarchy = new ConcurrentHashMap<>();
		stack.addAll(resolutionContextByRef.keySet());
		while (!stack.isEmpty()) {
			ResolutionContext rctx = resolutionContextByRef.get(stack.remove());
			switch (rctx.step) {
				case DONE: continue;
				case RESOLVING_FACTORY: {
					stack.add(rctx.getReference());
					ResolutionResult factoryResolution = ResolutionResult.compute(
							rctx.descriptor.getFactoryMethod().getInjectables().stream()
									.flatMap(inj -> inj.getReference().map(Stream::of).orElse(Stream.empty()))
									.collect(Collectors.toList()),
							resolutionContextByRef);
					if (factoryResolution.isFullyResolved()) {
						circularFactoryDepDetector.remove(rctx.getReference());
						createMono(rctx, factoryResolution, instHierarchy);
						rctx.step = Step.RESOLVING_SETTERS;
					} else {
						factoryResolution.getUnresolved().forEach(u -> {
							if (!circularFactoryDepDetector.add(u)) {
								String a = rctx.getReference().getServiceName();
								String b = u.getServiceName();
								throw new IllegalStateException("Circular dependency detected: service '" + a + "' needs '" + b
										+ "' to be instantiated and '" + b + "' needs '" + a + "' to be instantiated.");
							}
							stack.add(u);
						});
					}
					break;
				}
				case RESOLVING_SETTERS: {
					ResolutionResult setterResolution = ResolutionResult.compute(
							rctx.descriptor.getSetterMethods().stream()
									.map(SetterMethod::getInjectable)
									.flatMap(inj -> inj.getReference().map(Stream::of).orElse(Stream.empty()))
									.collect(Collectors.toList()),
							resolutionContextByRef);
					if (setterResolution.isFullyResolved()) {
						if (!setterResolution.hasNoDeps()) {
							enrichMonoWithSetterResolution(rctx, setterResolution);
						}
						rctx.step = Step.DONE;
					} else {
						stack.add(rctx.getReference());
						stack.addAll(setterResolution.getUnresolved());
					}
					break;
				}
				default: throw new AssertionError();
			}
		}
		enrichMonoWithSetterDelegation(resolutionContextByRef);
		return resolutionContextByRef.entrySet().stream()
				.collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().mono));
	}
	
	private static void createMono(ResolutionContext rctx, ResolutionResult factoryResolution,
			Map<RefWithParent, CircularInstantiationDetector> instHierarchy) {
		rctx.mono = Mono.deferWithContext(ctx -> {
			checkCircularInstantiation(rctx.getReference(),
					ctx.getOrDefault("parent", null),
					ctx.getOrDefault("grandParent", null),
					instHierarchy);
			if (factoryResolution.hasNoDeps()) {
				return rctx.descriptor.getFactoryMethod().invoke();
			} else {
				return Mono.zip(factoryResolution.getResolved().stream()
								.map(rctx0 -> putParentInSubscriberContext(rctx0.mono, ctx, rctx.getReference()))
								.collect(Collectors.toList()), Function.identity())
						.flatMap(rctx.descriptor.getFactoryMethod()::invoke);
			}
		});
		if (rctx.descriptor.isSingleton()) {
			rctx.mono = wrapSingleton(rctx);
		}
	}

	private static Mono<Object> wrapSingleton(ResolutionContext rctx) {
		AtomicBoolean lock = new AtomicBoolean();
		ReplayProcessor<Long> lockNotifier = ReplayProcessor.cacheLastOrDefault(0L);
		FluxSink<Long> sink = lockNotifier.sink(FluxSink.OverflowStrategy.LATEST);
		return lockNotifier.filter(__ -> lock.compareAndSet(false, true))
				.next()
				.flatMap(__ -> {
					Object o = rctx.singleton;
					if (o != null) {
						return Mono.just(o);
					}
					return rctx.mono.doOnNext(newInstance -> rctx.singleton = newInstance);
				})
				.doFinally(__ -> {
					lock.set(false); // unlock
					sink.next(0L); // notify those waiting on lock
				});
	}
	
	private static void enrichMonoWithSetterResolution(ResolutionContext rctx, ResolutionResult setterResolution) {
		rctx.mono = rctx.mono
				.flatMap(o -> Mono.deferWithContext(ctx -> {
							Mono<Void> setterMono = Mono.zip(setterResolution.getResolved().stream()
									.map(rctx0 -> putParentInSubscriberContext(rctx0.mono, ctx, rctx.getReference()))
									.collect(Collectors.toList()), Function.identity())
									.doOnNext(deps -> {
										int refI = 0;
										for (SetterMethod setter : rctx.descriptor.getSetterMethods()) {
											if (setter.getInjectable().getValue().isPresent()) {
												setter.invoke(o, setter.getInjectable().getValue().get());
											} else { // Assumes that ref is present if value is not
												setter.invoke(o, deps[refI++]);
											}
										}
									})
									.then();
							ArrayDeque<List<Mono<Void>>> setterDelegate = ctx.get("setterDelegate");
							if (!setterDelegate.isEmpty()) {
								setterDelegate.element().add(setterMono);
								return Mono.empty();
							}
							return setterMono;
						})
						.thenReturn(o));
	}
	
	private static void enrichMonoWithSetterDelegation(Map<ServiceReference<?>, ResolutionContext> resolutionContextByRef) {
		for (ResolutionContext rctx : resolutionContextByRef.values()) {
			rctx.mono = rctx.mono
					.flatMap(o -> Mono.deferWithContext(ctx -> {
								ArrayDeque<List<Mono<Void>>> setterDelegate = ctx.get("setterDelegate");
								List<Mono<Void>> setterMonos = setterDelegate.pop();
								if (setterDelegate.isEmpty()) {
									return Mono.when(setterMonos);
								}
								setterDelegate.element().addAll(setterMonos);
								return Mono.empty();
							})
							.thenReturn(o))
					.subscriberContext(ctx -> {
						ArrayDeque<List<Mono<Void>>> setterDelegate = ctx.getOrDefault("setterDelegate", new ArrayDeque<>());
						setterDelegate.push(new ArrayList<>());
						return ctx.put("setterDelegate", setterDelegate);
					});
		}
	}
	
	private static Mono<Object> putParentInSubscriberContext(Mono<Object> mono, Context oldContext, ServiceReference<?> parent) {
		return mono.subscriberContext(ctx -> {
			ctx = ctx.put("parent", parent);
			Optional<ServiceReference<?>> oldParent = oldContext.getOrEmpty("parent");
			if (oldParent.isPresent()) {
				ctx = ctx.put("grandParent", oldParent.get());
			}
			return ctx;
		});
	}
	
	private static void checkCircularInstantiation(ServiceReference<?> ref, ServiceReference<?> parent, ServiceReference<?> grandParent,
			Map<RefWithParent, CircularInstantiationDetector> instHierarchy) {
		RefWithParent parentRWP = new RefWithParent(grandParent, parent);
		RefWithParent thisRWP = new RefWithParent(parent, ref);
		CircularInstantiationDetector cid = instHierarchy.getOrDefault(parentRWP, new CircularInstantiationDetector());
		Optional<CircularInstantiationDetector> nextCidOpt = cid.add(ref);
		if (nextCidOpt.isPresent()) {
			instHierarchy.put(thisRWP, nextCidOpt.get());
		} else {
			throw new IllegalStateException("Circular instantiation detected: " + ref
					+ " endlessly instantiates other services that instantiate this one in their turn. "
					+ "Maybe declare " + ref + " as singleton?");
		}
	}
	
	static class ResolutionContext {
		
		private final ServiceDescriptor descriptor;
		private Mono<Object> mono;
		private Object singleton;
		private Step step = Step.RESOLVING_FACTORY;
		
		public ResolutionContext(ServiceDescriptor descriptor) {
			this.descriptor = descriptor;
		}
		
		ServiceReference<?> getReference() {
			return descriptor.getServiceReference();
		}
	}
	
	private static enum Step {
		RESOLVING_FACTORY, RESOLVING_SETTERS, DONE;
	}
}
