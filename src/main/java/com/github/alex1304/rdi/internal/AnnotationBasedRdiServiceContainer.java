package com.github.alex1304.rdi.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.alex1304.rdi.annotation.RdiInject;
import com.github.alex1304.rdi.annotation.RdiSingleton;

import reactor.core.Exceptions;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import reactor.util.Loggers;
import reactor.util.context.Context;

public class AnnotationBasedRdiServiceContainer extends AbstractRdiServiceContainer {
	
	private static final Lookup LOOKUP = MethodHandles.lookup();
	
	AnnotationBasedRdiServiceContainer(Map<Class<?>, Mono<Object>> serviceMonos) {
		super(serviceMonos);
	}

	public static AnnotationBasedRdiServiceContainer create(Class<?>[] annotatedClasses) {
		HashMap<Class<?>, Mono<Object>> serviceMonos = new HashMap<>();
		ArrayDeque<DependencyDescriptor> stack = new ArrayDeque<>();
		HashMap<Class<?>, DependencyDescriptor> descriptorCache = new HashMap<>();
		HashSet<Class<?>> circularFactoryDepDetector = new HashSet<>();
		HashMap<Class<?>, Object> singletons = new HashMap<>();
		Map<ClassWithParent, CircularInstantiationDetector> instHierarchy = new ConcurrentHashMap<>();
		for (Class<?> annotatedClass : new HashSet<>(Arrays.asList(annotatedClasses))) {
			DependencyDescriptor d = createDescriptor(annotatedClass, descriptorCache);
			stack.push(d);
		}
		while (!stack.isEmpty()) {
			DependencyDescriptor head = stack.pop();
			if (head.getMode() == DependencyMode.DONE) {
				continue;
			} else if (head.getMode() == DependencyMode.FACTORY) {
				stack.push(head);
				DependencyResolution<Mono<Object>, DependencyDescriptor> factoryResolution = DependencyResolution.resolve(
						head.getFactoryMH().type().parameterList().stream()
								.map(clazz -> createDescriptor(clazz, descriptorCache))
								.collect(Collectors.toList()),
						serviceMonos,
						DependencyDescriptor::getClazz);
				if (factoryResolution.isFullyResolved()) {
					circularFactoryDepDetector.remove(head.getClazz());
					Mono<Object> mono = Mono.deferWithContext(ctx -> {
						addInstantiation(head.getClazz(),
								ctx.getOrDefault("parent", null),
								ctx.getOrDefault("grandParent", null),
								instHierarchy);
						if (factoryResolution.hasNoDeps()) {
							return invokeMH(head.getFactoryMH());
						} else {
							return Mono.zip(factoryResolution.getResolved().stream()
											.map(r -> putParentInSubscriberContext(r, ctx, head.getClazz()))
											.collect(Collectors.toList()), Function.identity())
									.flatMap(deps -> invokeMH(head.getFactoryMH(), deps));
						}
					});
					if (head.isSingleton()) {
						mono = wrapSingleton(mono, head.getClazz(), singletons);
					}
					serviceMonos.put(head.getClazz(), mono);
					head.setMode(DependencyMode.SETTER);
				} else {
					factoryResolution.getUnresolved().forEach(u -> {
						if (!circularFactoryDepDetector.add(u.getClazz())) {
							String a = head.getClazz().getName();
							String b = u.getClazz().getName();
							throw new IllegalStateException("Circular dependency detected: " + a + " needs " + b
									+ " to be instantiated and " + b + " needs " + a + " to be instantiated.");
						}
						stack.push(u);
					});
				}
			} else if (head.getMode() == DependencyMode.SETTER) {
				DependencyResolution<Mono<Object>, DependencyDescriptor> setterResolution = DependencyResolution.resolve(
						head.getSetterMHs().stream()
								.map(mh -> mh.type().parameterArray()[1])
								.map(clazz -> createDescriptor(clazz, descriptorCache))
								.collect(Collectors.toList()),
						serviceMonos,
						DependencyDescriptor::getClazz);
				if (setterResolution.isFullyResolved()) {
					if (!setterResolution.hasNoDeps()) {
						serviceMonos.computeIfPresent(head.getClazz(), (k, v) -> v
								.flatMap(o -> Mono.deferWithContext(ctx -> {
											Mono<Void> setterMono = Mono.zip(setterResolution.getResolved().stream()
													.map(r -> putParentInSubscriberContext(r, ctx, head.getClazz()))
													.collect(Collectors.toList()), Function.identity())
													.doOnNext(deps -> {
														for (int i = 0 ; i < deps.length ; i++) {
															invokeVoidMH(head.getSetterMHs().get(i), o, deps[i]);
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
										.thenReturn(o)));
					}
					head.setMode(DependencyMode.DONE);
				} else {
					stack.push(head);
					setterResolution.getUnresolved().forEach(stack::push);
				}
			} else {
				throw new AssertionError();
			}
		}
		for (Class<?> key : new HashSet<>(serviceMonos.keySet())) {
			serviceMonos.computeIfPresent(key, (k, v) -> v
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
					})
					.log(k.getSimpleName()));
		}
		return new AnnotationBasedRdiServiceContainer(serviceMonos);
	}
	
	private static Mono<Object> putParentInSubscriberContext(Mono<Object> mono, Context oldContext, Class<?> parent) {
		return mono.subscriberContext(ctx -> {
			ctx = ctx.put("parent", parent);
			Optional<Class<?>> oldParent = oldContext.getOrEmpty("parent");
			if (oldParent.isPresent()) {
				ctx = ctx.put("grandParent", oldParent.get());
			}
			return ctx;
		});
	}
	
	private static void addInstantiation(Class<?> clazz, Class<?> parent, Class<?> grandParent,
			Map<ClassWithParent, CircularInstantiationDetector> instHierarchy) {
		ClassWithParent parentCWP = new ClassWithParent(grandParent, parent);
		ClassWithParent thisCWP = new ClassWithParent(parent, clazz);
		CircularInstantiationDetector cid = instHierarchy.getOrDefault(parentCWP, new CircularInstantiationDetector());
		Optional<CircularInstantiationDetector> nextCidOpt = cid.add(clazz);
		if (nextCidOpt.isPresent()) {
			instHierarchy.put(thisCWP, nextCidOpt.get());
		} else {
			throw new IllegalStateException("Circular instantiation detected: " + clazz.getName()
					+ " endlessly instantiates classes that instantiate this one in their turn. " + "Maybe declare "
					+ clazz.getName() + " as singleton?");
		}
		Loggers.getLogger(clazz.getSimpleName()).info(instHierarchy.toString());
	}

	private static Mono<Object> wrapSingleton(Mono<Object> toWrap, Class<?> clazz, Map<Class<?>, Object> singletons) {
		AtomicBoolean lock = new AtomicBoolean();
		ReplayProcessor<Long> lockNotifier = ReplayProcessor.cacheLastOrDefault(0L);
		FluxSink<Long> sink = lockNotifier.sink(FluxSink.OverflowStrategy.LATEST);
		return lockNotifier.filter(__ -> lock.compareAndSet(false, true))
				.next()
				.flatMap(__ -> {
					Object o = singletons.get(clazz);
					if (o != null) {
						return Mono.just(o);
					}
					return toWrap.doOnNext(newInstance -> singletons.put(clazz, newInstance));
				})
				.doFinally(__ -> {
					lock.set(false); // unlock
					sink.next(0L); // notify those waiting on lock
				});
	}

	private static DependencyDescriptor createDescriptor(Class<?> clazz, Map<Class<?>, DependencyDescriptor> cache) {
		return cache.computeIfAbsent(clazz, k -> {
			try {
				Method[] methods = clazz.getMethods();
				Constructor<?>[] constructors = clazz.getConstructors();
				MethodHandle factoryMH = null;
				List<MethodHandle> setterMHs = new ArrayList<>();
				for (Constructor<?> c : constructors) {
					c.setAccessible(true);
					if (!c.isAnnotationPresent(RdiInject.class)) {
						continue;
					}
					factoryMH = throwIfAlreadySet(factoryMH, LOOKUP.unreflectConstructor(c), clazz);
				}
				for (Method m : methods) {
					m.setAccessible(true);
					if (!m.isAnnotationPresent(RdiInject.class)) {
						continue;
					}
					MethodHandle mh = LOOKUP.unreflect(m);
					if (Modifier.isStatic(m.getModifiers())) {
						factoryMH = throwIfAlreadySet(factoryMH, mh, clazz);
					} else {
						if (mh.type().parameterCount() != 2) {
							throw new RuntimeException("Injection setters must have exactly 1 argument");
						}
						setterMHs.add(mh);
					}
				}
				if (factoryMH == null) {
					factoryMH = LOOKUP.findConstructor(clazz, MethodType.methodType(void.class));
				}
				return new DependencyDescriptor(
						clazz,
						Optional.ofNullable(clazz.getAnnotation(RdiSingleton.class))
								.map(RdiSingleton::value)
								.orElse(true),
						factoryMH,
						setterMHs);
			} catch (IllegalAccessException | NoSuchMethodException e) {
				throw new RuntimeException("Failed to read service injection methods on class " + clazz.getName(), e);
			}
		});
	}
	
	private static MethodHandle throwIfAlreadySet(MethodHandle old, MethodHandle mh, Class<?> clazz) {
		if (old != null) {
			throw new RuntimeException("The service class " + clazz.getName() + " has more than one "
					+ "method annotated with @" + RdiInject.class.getSimpleName());
		}
		return mh;
	}
	
	private static Mono<Object> invokeMH(MethodHandle mh, Object... args) {
		try {
			if (Mono.class.isAssignableFrom(mh.type().returnType())) {
				return (Mono<Object>) transformMH(mh, args).invoke(args);
			}
			return Mono.just(transformMH(mh, args).invoke(args));
		} catch (Throwable t) {
			throw Exceptions.propagate(t);
		}
	}
	
	private static void invokeVoidMH(MethodHandle mh, Object... args) {
		try {
			transformMH(mh, args).invoke(args);
		} catch (Throwable t) {
			throw Exceptions.propagate(t);
		}
	}
	
	private static MethodHandle transformMH(MethodHandle mh, Object[] args) {
		return mh.asType(mh.type().generic()).asSpreader(Object[].class, args.length);
	}
}
