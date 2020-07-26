package com.github.alex1304.rdi;

import static java.util.Objects.requireNonNull;

import java.util.Map;

import reactor.core.publisher.Mono;

public class DefaultRdiServiceContainer implements RdiServiceContainer {

	private final Map<ServiceReference<?>, Mono<Object>> serviceMonos;
	
	DefaultRdiServiceContainer(Map<ServiceReference<?>, Mono<Object>> serviceMonos) {
		this.serviceMonos = serviceMonos;
	}

	@Override
	public <S> Mono<S> getService(ServiceReference<S> serviceRef) {
		requireNonNull(serviceRef);
		return Mono.justOrEmpty(serviceMonos.get(serviceRef))
				.flatMap(mono -> mono.cast(serviceRef.getServiceClass()))
				.switchIfEmpty(Mono.error(() -> new RdiException("Service '" + serviceRef + "' not found")));
	}

}
