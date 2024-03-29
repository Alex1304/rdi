package com.github.alex1304.rdi;

import com.github.alex1304.rdi.config.RdiConfig;
import com.github.alex1304.rdi.resolver.DependencyResolver;
import reactor.core.publisher.Mono;

import static java.util.Objects.requireNonNull;

/**
 * Service container that is capable of managing services in a reactive fashion. Services are initialized at
 * subscription time, and instances may or may not be cached for subsequent accesses depending on the provided
 * configuration. The container is able to execute the dependency resolution and injection when a service requiring
 * dependencies is requested. It is also in charge of checking for circular dependencies and infinite instantiation
 * loops.
 *
 * @see RdiServiceContainer#create(RdiConfig)
 */
public interface RdiServiceContainer {

    /**
     * Creates a new {@link RdiServiceContainer}.
     *
     * @param config the RDI configuration containing all info about services and their dependencies
     * @return a new {@link RdiServiceContainer}
     */
    static RdiServiceContainer create(RdiConfig config) {
        requireNonNull(config);
        return new DefaultRdiServiceContainer(DependencyResolver.resolve(config.getServiceDescriptors()));
    }

    /**
     * Gets the service for the given reference. Dependency injection is performed upon subscribing to the returned
     * Mono. Given that no errors happen, the returned Mono is guaranteed to emit the service with all dependencies
     * properly injected. Subscribing multiple times may either return a new instance each time or a singleton,
     * depending on how the service was configured. In the latter case, the singleton creation is safe to trigger by
     * concurrent subscriptions to the mono.
     *
     * @param <S>        the type of service
     * @param serviceRef the service reference
     * @return a Mono emitting the Service instance. If the instantiation of the service fails, it will error with
     * {@link ServiceInstantiationException}. If a circular dependency is found when injecting setter dependencies, it
     * will error with {@link RdiException}.
     */
    <S> Mono<S> getService(ServiceReference<S> serviceRef);

    /**
     * Checks if this container contains a service with the given reference. It does not check whether the service is
     * initialized or not, it only checks for its existence in the configuration.
     *
     * @param serviceRef the reference to test
     * @return true if present, else false
     */
    boolean hasService(ServiceReference<?> serviceRef);
}
