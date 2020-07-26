package com.github.alex1304.rdi.config;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import reactor.core.publisher.Mono;

public interface FactoryMethod {
	
	Mono<Object> invoke(Object... args);
	
	List<Injectable> getInjectableParameters();
	
	static Function<Class<?>, FactoryMethod> constructor(Injectable... params) {
		return owner -> new ConstructorFactoryMethod(owner, Arrays.asList(params));
	}
	static Function<Class<?>, FactoryMethod> staticFactory(String methodName, Class<?> returnType, Injectable... params) {
		return owner -> new StaticFactoryMethod(owner, methodName, returnType, Arrays.asList(params));
	}
}
