---
id: the-container
title: The Container
---

Once you have all your [service descriptors](service-descriptors.md) set up, it is time to create the most important object in your application: the container.

## The `RdiConfig` class

To pass your service descriptors to the container, you have to do it via the `RdiConfig` class. `RdiConfig` is nothing more than a wrapper around a `Set<ServiceDescriptor>`. The difference is that it only has a "add" method, and it will throw an exception if you add two service descriptors that targets the same service reference.

To create a `RdiConfig` you simply need to create a builder and call `registerService` for each descriptor:

```java
RdiConfig config = RdiConfig.builder()
        .registerService(descriptor1)
        .registerService(descriptor2)
        .build();
```

## Creating the container

The container is represented by the `RdiServiceContainer` interface. Since this is a public interface, any third party may provide a custom implementation, but the library comes with a built-in one accessible via `RdiServiceContainer#create(RdiConfig)`.

```java
RdiServiceContainer container = RdiServiceContainer.create(config);
```

When calling `create`, a lot of things are happening behind the scenes. Even if it may change from an implementation to another, the creation of a container goes through the following steps:

1. The descriptors in the config are read, and a dependency graph is constructed.
2. With that dependency graph, it will detect any cycles in the factory methods of the services, throwing an exception and interrupting everything if the check fails.
3. The reactive chains to instantiate services and their dependencies will be assembled. The cache for singletons will be prepared.
4. A mapping between service references and the newly created reactive chains will be established
5. The container is created with that mapping info stored internally, in a way or another.

## Using the container

Once your container is created, you can use it to request service instances. That is done via the `RdiServiceContainer#getService(ServiceReference)` method:

```java
// Somewhere defined as constant
public static final ServiceReference<A> A_REF = ServiceReference.ofType(A.class);
// ...

Mono<A> monoA = container.getService(A_REF);
```

In order to get the actual instance of A, you need to integrate that mono in your own reactive chain, or block on it if you do not want to use the reactive features. If you are reading this document you are most probably in the former case, so you will probably want something like this:

```java
monoA.subscribe(a -> {
    // Do something with A, or use an intermediate operator
    // such as flatMap to let it flow downstream.
})
```

It is important to note that **nothing will happen until the returned Mono is subscribed to**. The dependency injection is performed only at subscription time. If you didn't get any errors when creating the config, it is unlikely that your Mono will error, unless a constructor or a setter explicitly throws an exception or a circular instantiation is detected, as explained in the previous page.

> If your service hierarchy is well designed, you should resort to the `getService` method only once in your application. If you define one root service that depends on all others, you should be able to access all other services via the injections.

## Singleton thread safety

Subscribing to the `Mono` returned by `getService` may be done by multiple threads. The default implementation is fully thread-safe, a singleton service is guaranteed to be instantiated once and only once.
