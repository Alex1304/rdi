package com.github.alex1304.rdi;

import static java.util.Objects.requireNonNull;

import com.github.alex1304.rdi.config.RdiConfig;
import com.github.alex1304.rdi.resolver.DependencyResolver;

import reactor.core.publisher.Mono;

public interface RdiServiceContainer {

	/**
	 * Gets the service for the given reference.
	 * 
	 * @param <S>          the type of service
	 * @param serviceRef the service reference
	 * @return a Mono emitting the Service instance
	 */
	<S> Mono<S> getService(ServiceReference<S> serviceRef);
	
	public static RdiServiceContainer create(RdiConfig config) {
		requireNonNull(config);
		return new DefaultRdiServiceContainer(
				DependencyResolver.resolve(
						config.getServiceDescriptors()));
	}
}
