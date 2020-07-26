package com.github.alex1304.rdi.config;

import java.util.Arrays;
import java.util.List;

import reactor.core.publisher.Mono;

public interface FactoryMethod {
	
	Mono<Object> invoke(Object... args);
	
	List<Injectable> getInjectables();
	
	static FactoryMethod constructor(Class<?> owner, Injectable... params) {
		return new ConstructorFactoryMethod(owner, Arrays.asList(params));
	}
	static FactoryMethod staticFactory(Class<?> owner, String methodName, Class<?> returnType, Injectable... params) {
		return new StaticFactoryMethod(owner, methodName, returnType, Arrays.asList(params));
	}
}
