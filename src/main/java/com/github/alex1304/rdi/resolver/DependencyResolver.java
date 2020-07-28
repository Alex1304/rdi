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

import com.github.alex1304.rdi.RdiException;
import com.github.alex1304.rdi.ServiceReference;
import com.github.alex1304.rdi.config.ServiceDescriptor;
import com.github.alex1304.rdi.config.SetterMethod;

import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.context.Context;

/**
 * Helper class to assemble the reactive chains to instantiate the services and
 * perform the dependency injection.
 * 
 * @see DependencyResolver#resolve(Set)
 */
public class DependencyResolver {
	
	private static final Logger LOGGER_ASSEMBLY = Loggers.getLogger("rdi.resolver.assembly");
	private static final Logger LOGGER_SUBSCRIPTION = Loggers.getLogger("rdi.resolver.subscription");
	
	private DependencyResolver() {
		throw new AssertionError();
	}
	
	/**
	 * Resolves the dependency tree described by the given set of service
	 * descriptors. The result of the resolution is stored in a {@link Map} with the
	 * {@link ServiceReference} as key and the {@link Mono} assembled with all the
	 * dependency injection logic as value.
	 * 
	 * @param serviceDescriptors the set of service descriptors to use to perform
	 *                           the dependency resolution
	 * @return a Map containing the results of the dependency resolution
	 */
	public static Map<ServiceReference<?>, Mono<Object>> resolve(Set<ServiceDescriptor> serviceDescriptors) {
		Map<ServiceReference<?>, ResolutionContext> resolutionContextByRef = serviceDescriptors.stream()
				.map(ResolutionContext::new)
				.collect(Collectors.toMap(rctx -> rctx.getReference(), Function.identity()));
		Queue<ServiceReference<?>> stack = Collections.asLifoQueue(new ArrayDeque<>());
		HashSet<ServiceReference<?>> circularFactoryDepDetector = new HashSet<>();
		Map<RefWithParent, CircularInstantiationDetector> instantiationChains = new ConcurrentHashMap<>();
		stack.addAll(resolutionContextByRef.keySet());
		while (!stack.isEmpty()) {
			ResolutionContext rctx = resolutionContextByRef.get(stack.remove());
			switch (rctx.getStep()) {
				case DONE: continue;
				case RESOLVING_FACTORY: {
					stack.add(rctx.getReference());
					ResolutionResult factoryResolution = ResolutionResult.compute(
							rctx.getReference(),
							rctx.getDescriptor().getFactoryMethod().getInjectableParameters().stream()
									.flatMap(inj -> inj.getReference().map(Stream::of).orElse(Stream.empty()))
									.collect(Collectors.toList()),
							resolutionContextByRef);
					if (factoryResolution.isFullyResolved()) {
						if (LOGGER_ASSEMBLY.isDebugEnabled()) {
							LOGGER_ASSEMBLY.debug("Resolved factory dependencies for {}: {}", rctx.getReference(), factoryResolution.getResolved()
									.stream()
									.map(ResolutionContext::getReference)
									.collect(Collectors.toList()));
						}
						circularFactoryDepDetector.remove(rctx.getReference());
						createMono(rctx, factoryResolution, instantiationChains);
						rctx.setStep(ResolutionStep.RESOLVING_SETTERS);
					} else {
						LOGGER_ASSEMBLY.debug("Discovered factory dependencies for {}: {}", rctx.getReference(), factoryResolution.getUnresolved());
						factoryResolution.getUnresolved().forEach(u -> {
							if (!circularFactoryDepDetector.add(u)) {
								String a = rctx.getReference().toString();
								String b = u.toString();
								throw new RdiException("Circular dependency detected: service '" + a + "' needs '" + b
										+ "' to be instantiated and '" + b + "' needs '" + a + "' to be instantiated.");
							}
							stack.add(u);
						});
					}
					break;
				}
				case RESOLVING_SETTERS: {
					ResolutionResult setterResolution = ResolutionResult.compute(
							rctx.getReference(),
							rctx.getDescriptor().getSetterMethods().stream()
									.map(SetterMethod::getInjectableParameter)
									.flatMap(inj -> inj.getReference().map(Stream::of).orElse(Stream.empty()))
									.collect(Collectors.toList()),
							resolutionContextByRef);
					if (setterResolution.isFullyResolved()) {
						if (LOGGER_ASSEMBLY.isDebugEnabled()) {
							LOGGER_ASSEMBLY.debug("Resolved setter dependencies for {}: {}", rctx.getReference(), setterResolution.getResolved()
									.stream()
									.map(ResolutionContext::getReference)
									.collect(Collectors.toList()));
						}
						if (!rctx.getDescriptor().getSetterMethods().isEmpty()) {
							enrichMonoWithSetterResolution(rctx, setterResolution);
						}
						rctx.setStep(ResolutionStep.DONE);
					} else {
						LOGGER_ASSEMBLY.debug("Discovered setter dependencies for {}: {}", rctx.getReference(), setterResolution.getUnresolved());
						stack.add(rctx.getReference());
						stack.addAll(setterResolution.getUnresolved());
					}
					break;
				}
				default: throw new AssertionError();
			}
		}
		finalizeMonoAssembly(resolutionContextByRef);
		return resolutionContextByRef.entrySet().stream()
				.collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getMono()));
	}
	
	private static void createMono(ResolutionContext rctx, ResolutionResult factoryResolution,
			Map<RefWithParent, CircularInstantiationDetector> instantiationChains) {
		rctx.setMono(
				Mono.deferWithContext(ctx -> {
					checkCircularInstantiation(rctx.getReference(),
							ctx.getOrDefault("parent", null),
							ctx.getOrDefault("grandParent", null),
							instantiationChains);
					if (factoryResolution.hasNoDeps()) {
						return rctx.getDescriptor().getFactoryMethod().invoke();
					} else {
						return Mono.zip(factoryResolution.getResolved().stream()
										.map(rctx0 -> putParentInSubscriberContext(rctx0.getMono(), ctx, rctx.getReference()))
										.collect(Collectors.toList()), Function.identity())
								.flatMap(rctx.getDescriptor().getFactoryMethod()::invoke);
					}
				})
				.doOnNext(o -> LOGGER_SUBSCRIPTION.debug("New instance of {} created: {}", rctx.getReference())));
		if (rctx.getDescriptor().isSingleton()) {
			rctx.setMono(wrapSingleton(rctx));
		}
	}

	private static Mono<Object> wrapSingleton(ResolutionContext rctx) {
		LOGGER_ASSEMBLY.debug("Wrapping {} in a singleton", rctx.getReference());
		AtomicBoolean lock = new AtomicBoolean();
		ReplayProcessor<Long> lockNotifier = ReplayProcessor.cacheLastOrDefault(0L);
		FluxSink<Long> sink = lockNotifier.sink(FluxSink.OverflowStrategy.LATEST);
		Mono<Object> factoryMono = rctx.getMono();
		return Mono.deferWithContext(ctx -> lockNotifier.filter(__ -> lock.compareAndSet(false, true))
				.next()
				.flatMap(__ -> {
					LOGGER_SUBSCRIPTION.debug("Acquired singleton lock: {}", rctx.getReference());
					Object o = rctx.getSingleton();
					if (o != null) {
						LOGGER_SUBSCRIPTION.debug("Cached singleton instance found for {}: {}", rctx.getReference(), o);
						AtomicBoolean isFreshInstance = ctx.get("isFreshInstance");
						isFreshInstance.set(false);
						return Mono.just(o);
					}
					return factoryMono.doOnNext(rctx::setSingleton).doOnNext(newInstance -> LOGGER_SUBSCRIPTION
							.debug("Instantiated singleton for {}, now caching: {}", rctx.getReference(), o));
				})
				.doFinally(__ -> {
					LOGGER_SUBSCRIPTION.debug("Released singleton lock: {}", rctx.getReference());
					lock.set(false); // unlock
					sink.next(0L); // notify those waiting on lock
				}));
	}
	
	private static void enrichMonoWithSetterResolution(ResolutionContext rctx, ResolutionResult setterResolution) {
		rctx.setMono(rctx.getMono()
				.flatMap(o -> Mono.deferWithContext(ctx -> {
							AtomicBoolean isFreshInstance = ctx.get("isFreshInstance");
							if (!isFreshInstance.get()) {
								LOGGER_SUBSCRIPTION.debug("Skipping setters for {} on {}: "
												+ "instance was obtained from cache and setters were already executed",
										rctx.getReference(), o);
								return Mono.empty();
							}
							Mono<Void> setterMono = Mono.zip(setterResolution.getResolved().stream()
									.map(rctx0 -> putParentInSubscriberContext(rctx0.getMono(), ctx, rctx.getReference()))
									.collect(Collectors.toList()), Function.identity())
									.switchIfEmpty(Mono.fromCallable(() -> new Object[0]))
									.doOnNext(deps -> {
										int refI = 0;
										for (SetterMethod setter : rctx.getDescriptor().getSetterMethods()) {
											if (setter.getInjectableParameter().getValue().isPresent()) {
												setter.invoke(o);
											} else if (setter.getInjectableParameter().getReference().isPresent()) {
												setter.invoke(o, deps[refI++]);
											} else {
												throw new AssertionError("Injectable.getValue() and "
														+ "Injectable.getReference() were both empty");
											}
										}
									})
									.then(Mono.fromRunnable(() -> LOGGER_SUBSCRIPTION.debug("Invoked setters for {} on {}",
											rctx.getReference(), o)));
							ArrayDeque<List<Mono<Void>>> setterDelegate = ctx.get("setterDelegate");
							setterDelegate.element().add(setterMono); // The deque cannot be empty at this stage
							LOGGER_SUBSCRIPTION.debug("Found setters for {} on {}: deferring execution until all "
									+ "dependencies are instantiated", rctx.getReference(), o);
							return Mono.empty();
						})
						.thenReturn(o)));
	}
	
	private static void finalizeMonoAssembly(Map<ServiceReference<?>, ResolutionContext> resolutionContextByRef) {
		for (ResolutionContext rctx : resolutionContextByRef.values()) {
			rctx.setMono(rctx.getMono()
					// Assemble code to execute all setter delegates
					.flatMap(o -> Mono.deferWithContext(ctx -> {
								ArrayDeque<List<Mono<Void>>> setterDelegate = ctx.get("setterDelegate");
								List<Mono<Void>> setterMonos = setterDelegate.pop();
								if (setterDelegate.isEmpty()) {
									return Mono.when(setterMonos)
											.then(Mono.fromRunnable(() -> LOGGER_SUBSCRIPTION
													.debug("Executed all setters for {} and all its dependencies on {}",
															rctx.getReference(), o)));
								}
								LOGGER_SUBSCRIPTION.debug("Delegating setter execution to parent service for {} on {}",
										rctx.getReference(), o);
								setterDelegate.element().addAll(setterMonos);
								return Mono.empty();
							})
							.thenReturn(o))
					.doOnNext(o -> LOGGER_SUBSCRIPTION.debug("Returning instance for requested service {}: {}",
							rctx.getReference(), o))
					// Initialize subscriber context
					.subscriberContext(ctx -> {
						LOGGER_SUBSCRIPTION.debug("Subscription triggered for {}", rctx.getReference());
						ArrayDeque<List<Mono<Void>>> setterDelegate = ctx.getOrDefault("setterDelegate", new ArrayDeque<>());
						setterDelegate.push(new ArrayList<>());
						return ctx.put("setterDelegate", setterDelegate)
								.put("isFreshInstance", new AtomicBoolean(true));
					}));
			LOGGER_ASSEMBLY.debug("Finalized reactive chain assembly for {}", rctx.getReference());
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
			Map<RefWithParent, CircularInstantiationDetector> instantiationChains) {
		RefWithParent parentRWP = new RefWithParent(grandParent, parent);
		RefWithParent thisRWP = new RefWithParent(parent, ref);
		CircularInstantiationDetector cid = instantiationChains.getOrDefault(parentRWP, new CircularInstantiationDetector());
		Optional<CircularInstantiationDetector> nextCidOpt = cid.add(ref);
		if (nextCidOpt.isPresent()) {
			instantiationChains.put(thisRWP, nextCidOpt.get());
			LOGGER_SUBSCRIPTION.debug("Checking circular instantiation for {}, current instantiation chain: {}", ref, nextCidOpt.get());
		} else {
			throw new RdiException("Circular instantiation detected: " + ref
					+ " endlessly instantiates other services that instantiate this one in their turn. "
					+ "Maybe declare " + ref + " as singleton?");
		}
	}
}
