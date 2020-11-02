package com.github.alex1304.rdi.resolver;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.alex1304.rdi.RdiException;
import com.github.alex1304.rdi.ServiceReference;
import com.github.alex1304.rdi.config.ServiceDescriptor;
import com.github.alex1304.rdi.config.SetterMethod;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.context.ContextView;

/**
 * Helper class to assemble the reactive chains instantiating the services and
 * performing the dependency injection.
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
		Queue<RefWithParent> stack = Collections.asLifoQueue(new ArrayDeque<>());
		Map<RefWithParent, CycleDetector> dependencyChains = new ConcurrentHashMap<>();
		Map<RefWithParent, CycleDetector> instantiationChains = new ConcurrentHashMap<>();
		stack.addAll(resolutionContextByRef.keySet().stream()
				.peek(ref -> throwIfCycleDetected(ref, null, null, dependencyChains, null)) // Should never throw, just initialize the chains
				.map(ref -> new RefWithParent(null, ref))
				.collect(Collectors.toList()));
		while (!stack.isEmpty()) {
			RefWithParent refWithParent = stack.remove();
			ResolutionContext rctx = resolutionContextByRef.get(refWithParent.getElement());
			switch (rctx.getStep()) {
				case DONE: continue;
				case RESOLVING_FACTORY: {
					stack.add(refWithParent);
					ResolutionResult factoryResolution = ResolutionResult.compute(
							rctx.getReference(),
							rctx.getDescriptor().getFactoryMethod().getInjectableParameters().stream()
									.flatMap(inj -> inj.getReference().map(Stream::of).orElse(Stream.empty()))
									.collect(Collectors.toList()),
							resolutionContextByRef);
					if (factoryResolution.isFullyResolved()) {
						if (LOGGER_ASSEMBLY.isDebugEnabled()) {
							logAssembly(rctx.getReference(), "Resolved factory dependencies: " + factoryResolution.getResolved()
									.stream()
									.map(ResolutionContext::getReference)
									.collect(Collectors.toList()));
						}
						createMono(rctx, factoryResolution, instantiationChains);
						rctx.setStep(ResolutionStep.RESOLVING_SETTERS);
					} else {
						if (LOGGER_ASSEMBLY.isDebugEnabled()) {
							logAssembly(rctx.getReference(), "Discovered factory dependencies: " + factoryResolution.getUnresolved());
						}
						factoryResolution.getUnresolved().forEach(u -> {
							throwIfCycleDetected(u,
									refWithParent.getElement(),
									refWithParent.getParent(),
									dependencyChains,
									() -> "Circular dependency detected.");
							stack.add(new RefWithParent(refWithParent.getElement(), u));
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
							logAssembly(rctx.getReference(), "Resolved setter dependencies: " + setterResolution.getResolved()
									.stream()
									.map(ResolutionContext::getReference)
									.collect(Collectors.toList()));
						}
						if (!rctx.getDescriptor().getSetterMethods().isEmpty()) {
							enrichMonoWithSetterResolution(rctx, setterResolution);
						}
						rctx.setStep(ResolutionStep.DONE);
					} else {
						if (LOGGER_ASSEMBLY.isDebugEnabled()) {
							logAssembly(rctx.getReference(), "Discovered setter dependencies: " + setterResolution.getUnresolved());
						}
						stack.add(refWithParent);
						setterResolution.getUnresolved().forEach(u -> stack.add(new RefWithParent(null /*ignored*/, u)));
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
			Map<RefWithParent, CycleDetector> instantiationChains) {
		rctx.setMono(
				Mono.deferContextual(ctx -> {
					throwIfCycleDetected(rctx.getReference(),
							ctx.getOrDefault("parent", null),
							ctx.getOrDefault("grandParent", null),
							instantiationChains,
							() -> "Circular instantiation detected involving " + rctx.getReference()
									+ ". Maybe mark " + rctx.getReference() + " as singleton?");
					if (factoryResolution.hasNoDeps()) {
						return rctx.getDescriptor().getFactoryMethod().invoke();
					} else {
						return Mono.zip(factoryResolution.getResolved().stream()
										.map(rctx0 -> putParentInSubscriberContext(rctx0.getMono(), ctx, rctx.getReference()))
										.collect(Collectors.toList()), Function.identity())
								.flatMap(rctx.getDescriptor().getFactoryMethod()::invoke);
					}
				})
				.doOnNext(o -> logSubscription(rctx.getReference(), o, "New instance created")));
		if (rctx.getDescriptor().isSingleton()) {
			rctx.setMono(wrapSingleton(rctx));
		}
	}

	private static Mono<Object> wrapSingleton(ResolutionContext rctx) {
		logAssembly(rctx.getReference(), "Wrapping in singleton");
		AtomicBoolean lock = new AtomicBoolean();
		Sinks.Many<Long> lockNotifier = Sinks.many().replay().latestOrDefault(0L);
		Mono<Object> factoryMono = rctx.getMono();
		return Mono.deferContextual(ctx -> lockNotifier.asFlux().filter(__ -> lock.compareAndSet(false, true))
				.next()
				.doOnSubscribe(s -> logSubscription(rctx.getReference(), null, "Waiting on singleton lock"))
				.flatMap(__ -> {
					logSubscription(rctx.getReference(), null, "Acquired singleton lock");
					Object o = rctx.getSingleton();
					if (o != null) {
						logSubscription(rctx.getReference(), o, "Obtained cached singleton instance");
						AtomicBoolean isFreshInstance = ctx.get("isFreshInstance");
						isFreshInstance.set(false);
						return Mono.just(o);
					}
					return factoryMono.doOnNext(rctx::setSingleton).doOnNext(newInstance -> 
							logSubscription(rctx.getReference(), newInstance, "Instantiated singleton, now caching"));
				})
				.doFinally(__ -> {
					logSubscription(rctx.getReference(), null, "Released singleton lock");
					lock.set(false); // unlock
					lockNotifier.tryEmitNext(0L); // notify those waiting on lock
				}));
	}
	
	private static void enrichMonoWithSetterResolution(ResolutionContext rctx, ResolutionResult setterResolution) {
		rctx.setMono(rctx.getMono()
				.flatMap(o -> Mono.deferContextual(ctx -> {
							AtomicBoolean isFreshInstance = ctx.get("isFreshInstance");
							if (rctx.getDescriptor().getSetterMethods().isEmpty()) {
								logSubscription(rctx.getReference(), o, "No setters found");
								return Mono.empty();
							}
							if (!isFreshInstance.get()) {
								logSubscription(rctx.getReference(), o, "Ignoring setters as instance was obtained from cache");
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
									.then(Mono.fromRunnable(() -> logSubscription(rctx.getReference(), o, "Successfully invoked setters")));
							ArrayDeque<List<Mono<Void>>> setterDelegate = ctx.get("setterDelegate");
							setterDelegate.element().add(setterMono); // The deque cannot be empty at this stage
							logSubscription(rctx.getReference(), o, "Setters found: their invocation will be deferred until all "
									+ "dependency instances are available");
							return Mono.empty();
						})
						.thenReturn(o)));
	}
	
	private static void finalizeMonoAssembly(Map<ServiceReference<?>, ResolutionContext> resolutionContextByRef) {
		for (ResolutionContext rctx : resolutionContextByRef.values()) {
			rctx.setMono(rctx.getMono()
					// Assemble code to execute all setter delegates
					.flatMap(o -> Mono.deferContextual(ctx -> {
								ArrayDeque<List<Mono<Void>>> setterDelegate = ctx.get("setterDelegate");
								List<Mono<Void>> setterMonos = setterDelegate.pop();
								if (setterDelegate.isEmpty()) {
									return Mono.when(setterMonos);
								}
								setterDelegate.element().addAll(setterMonos);
								return Mono.empty();
							})
							.thenReturn(o))
					.doOnNext(o -> logSubscription(rctx.getReference(), o, "Returning instance"))
					// Initialize subscriber context
					.contextWrite(ctx -> {
						logSubscription(rctx.getReference(), null, "Subscription triggered");
						ArrayDeque<List<Mono<Void>>> setterDelegate = ctx.getOrDefault("setterDelegate", new ArrayDeque<>());
						setterDelegate.push(new ArrayList<>());
						return ctx.put("setterDelegate", setterDelegate)
								.put("isFreshInstance", new AtomicBoolean(true));
					}));
			logAssembly(rctx.getReference(), "Finalized reactive chain assembly");
		}
	}
	
	private static Mono<Object> putParentInSubscriberContext(Mono<Object> mono, ContextView oldContext, ServiceReference<?> parent) {
		return mono.contextWrite(ctx -> {
			ctx = ctx.put("parent", parent);
			Optional<ServiceReference<?>> oldParent = oldContext.getOrEmpty("parent");
			if (oldParent.isPresent()) {
				ctx = ctx.put("grandParent", oldParent.get());
			}
			return ctx;
		});
	}
	
	private static void throwIfCycleDetected(ServiceReference<?> ref, ServiceReference<?> parent, ServiceReference<?> grandParent,
			Map<RefWithParent, CycleDetector> chains, Supplier<String> errorMessage) {
		RefWithParent parentRWP = new RefWithParent(grandParent, parent);
		RefWithParent thisRWP = new RefWithParent(parent, ref);
		CycleDetector cycleDetector = chains.getOrDefault(parentRWP, new CycleDetector());
		CycleDetector nextCycleDetector = cycleDetector.next(ref);
		if (!nextCycleDetector.hasCycle()) {
			chains.put(thisRWP, nextCycleDetector);
		} else {
			throw new RdiException(errorMessage.get() + " Chain: " + nextCycleDetector);
		}
	}
	
	private static void logSubscription(ServiceReference<?> ref, Object instance, String message) {
		if (instance != null) {
			LOGGER_SUBSCRIPTION.debug("[serviceRef={}, instance={}] {}", ref, instance, message);
		} else {
			LOGGER_SUBSCRIPTION.debug("[serviceRef={}] {}", ref, message);
		}
	}
	
	private static void logAssembly(ServiceReference<?> ref, String message) {
		LOGGER_ASSEMBLY.debug("[serviceRef={}] {}", ref, message);
	}
}
