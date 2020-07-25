package com.github.alex1304.rdi;

import com.github.alex1304.rdi.internal.AnnotationBasedRdiServiceContainer;

import reactor.core.publisher.Mono;

public interface RdiServiceContainer {

	/**
	 * Gets the service for the given class.
	 * 
	 * @param <S>          the type of service
	 * @param serviceClass the service class
	 * @return a Mono emitting the Service instance
	 */
	<S> Mono<S> getService(Class<S> serviceClass);
	
	static RdiServiceContainer annotationBased(Class<?>... annotatedClasses) {
		return AnnotationBasedRdiServiceContainer.create(annotatedClasses);
	}
}
