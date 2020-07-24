package com.github.alex1304.rservice.internal;

import java.util.Map;

import com.github.alex1304.rservice.RdiServiceContainer;
import com.github.alex1304.rservice.RdiServiceNotFoundException;

import reactor.core.publisher.Mono;

abstract class AbstractRdiServiceContainer implements RdiServiceContainer {
	
	private final Map<Class<?>, Mono<Object>> serviceMonos;
	
	AbstractRdiServiceContainer(Map<Class<?>, Mono<Object>> serviceMonos) {
		this.serviceMonos = serviceMonos;
	}

	@Override
	public <S> Mono<S> getService(Class<S> serviceClass) {
		return Mono.justOrEmpty(serviceMonos.get(serviceClass))
				.flatMap(mono -> mono.cast(serviceClass))
				.switchIfEmpty(Mono.error(() -> new RdiServiceNotFoundException("Service " + serviceClass.getName() + " not found")));
	}

}
