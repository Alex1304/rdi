package com.github.alex1304.rdi.config;

import com.github.alex1304.rdi.RdiException;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Represents a method that can create a new instance of a service. It can be either a constructor or a static factory.
 * Since the factory method will be inside the same class as the service itself most of the time, {@link FactoryMethod}
 * instances are created via a {@link Function} parameterized with the owner class, so that you only need to specify the
 * injectable parameters.
 *
 * <p>
 * Although not required, the static methods in this interface are designed for use with an <code>import static</code>
 * statement for better code readability.
 *
 * @see FactoryMethod#constructor(Injectable...)
 * @see FactoryMethod#staticFactory(String, Class, Injectable...)
 * @see FactoryMethod#externalStaticFactory(Class, String, Class, Injectable...)
 */
public interface FactoryMethod {

    /**
     * Creates a {@link FactoryMethod} representing a constructor. The constructor must be public, and the type of the
     * provided injectables must match strictly the signature of the constructor.
     *
     * @param params describes what to inject in the constructor parameters
     * @return a new {@link FactoryMethod}
     * @throws RdiException if the constructor is not public, does not exist or has a signature that doesn't match with
     *                      the given injectable parameters.
     */
    static Function<Class<?>, FactoryMethod> constructor(Injectable... params) {
        return owner -> new ConstructorFactoryMethod(owner, Arrays.asList(params));
    }

    /**
     * Creates a {@link FactoryMethod} representing a static factory method. The method must be public, and the name,
     * return type, and type of the provided injectables must match strictly the signature of the method.
     *
     * <p>
     * This assumes that the static factory is located in the same class as the service to instantiate. If it isn't the
     * case, use {@link #externalStaticFactory(Class, String, Class, Injectable...)} instead.
     *
     * @param methodName the name of the method
     * @param returnType the return type of the method
     * @param params     describes what to inject in the method parameters
     * @return a new {@link FactoryMethod}
     * @throws RdiException if the method is not public, does not exist or has a signature that doesn't match with the
     *                      given injectable parameters.
     */
    static Function<Class<?>, FactoryMethod> staticFactory(String methodName, Class<?> returnType,
                                                           Injectable... params) {
        return owner -> new StaticFactoryMethod(owner, methodName, returnType, Arrays.asList(params));
    }

    /**
     * Creates a {@link FactoryMethod} representing a static factory method. The method must be public, and the name,
     * return type, and type of the provided injectables must match strictly the signature of the method.
     *
     * <p>
     * If the static factory method is located in the same class as the service to instantiate, you may prefer using
     * {@link #staticFactory(String, Class, Injectable...)} which doesn't require you to specify the owner.
     *
     * @param owner      the class that owns the target factory method
     * @param methodName the name of the method
     * @param returnType the return type of the method
     * @param params     describes what to inject in the method parameters
     * @return a new {@link FactoryMethod}
     * @throws RdiException if the method is not public, does not exist or has a signature that doesn't match with the
     *                      given injectable parameters.
     */
    static Function<Class<?>, FactoryMethod> externalStaticFactory(Class<?> owner, String methodName,
                                                                   Class<?> returnType, Injectable... params) {
        return __ -> new StaticFactoryMethod(owner, methodName, returnType, Arrays.asList(params));
    }

    Mono<Object> invoke(Object... args);

    List<Injectable> getInjectableParameters();
}
