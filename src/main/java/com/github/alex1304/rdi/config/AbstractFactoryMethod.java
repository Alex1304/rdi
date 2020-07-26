package com.github.alex1304.rdi.config;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

abstract class AbstractFactoryMethod implements FactoryMethod {
	
	private final Class<?> owner;
	private final String methodName;
	private final Class<?> returnType;
	private final List<Injectable> params;
	private final MethodHandle methodHandle;

	AbstractFactoryMethod(Class<?> owner, String methodName, Class<?> returnType, List<Injectable> params) {
		this.owner = owner;
		this.methodName = methodName;
		this.returnType = returnType;
		this.params = params;
		this.methodHandle = prepareMethodHandle();
	}
	
	@Override
	public Mono<Object> invoke(Object... args) {
		try {
			if (Mono.class.isAssignableFrom(methodHandle.type().returnType())) {
				return (Mono<Object>) asGenericSpreader(methodHandle, args.length)
						.invoke(args);
			}
			return Mono.just(asGenericSpreader(methodHandle, args.length)
					.invoke(args));
		} catch (Throwable t) {
			throw Exceptions.propagate(t);
		}
	}
	
	@Override
	public List<Injectable> getInjectables() {
		return Collections.unmodifiableList(params);
	}

	abstract MethodHandle findMethodHandle(Class<?> owner, String methodName, Class<?> returnType,
			List<Class<?>> paramTypes) throws IllegalAccessException, NoSuchMethodException;

	private MethodHandle prepareMethodHandle() {
		List<Class<?>> paramTypes = new ArrayList<>();
		Map<Integer, Object> values = new HashMap<>();
		int i = 0;
		for (Injectable inj : params) {
			paramTypes.add(inj.getType());
			final int captureOfI = i;
			inj.getValue().ifPresent(v -> values.put(captureOfI, v));
			i++;
		}
		try {
			MethodHandle mh = findMethodHandle(owner, methodName, returnType, paramTypes);
			for (Entry<Integer, Object> entry : values.entrySet()) {
				mh = MethodHandles.insertArguments(mh, entry.getKey(), entry.getValue());
			}
			return mh;
		} catch (IllegalAccessException | NoSuchMethodException e) {
			throw new RuntimeException("Error when acquiring factory method handle for class " + owner.getName(), e);
		}
	}
	
	private static MethodHandle asGenericSpreader(MethodHandle mh, int argsCount) {
		return mh.asType(mh.type().generic()).asSpreader(Object[].class, argsCount);
	}
}
