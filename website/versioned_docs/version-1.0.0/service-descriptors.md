---
id: version-1.0.0-service-descriptors
title: Service Descriptors
original_id: service-descriptors
---

A **service descriptor** is what allows to define the dependencies to inject in a specific service. A service descriptor may only apply to a single [service reference](service-references.md). If you want to define more than one way to instantiate a class, you are supposed to create as many service references with the same type and with a different name to distinguish them.

## The `Injectable` interface

The `Injectable` interface is typically used to represent a dependency to inject. It may be a reference to another service, but it can also be a value which is known in advance.

### Injecting another service

This is what you will use most of the time. Injecting another service is done via `Injectable#ref(ServiceReference)`:
```java
ServiceReference<A> ref = ServiceReference.ofType(A.class);

Injectable toInject = Injectable.ref(ref);
```

> **Important**: The other service must be registered in the container as well. Injecting a reference to a service that is not registered will cause an exception upon creating the container.

### Injecting values known in advance

If you need to fill a constructor parameter or a setter with a value known in advance, you can use `Injectable#value(...)` (overloads exist for object and primitive types):
```java
int x = 1304;

Injectable toInject = Injectable.value(x);
```

## Defining dependencies
### Constructor dependencies

Defining dependencies in a constructor is done with `FactoryMethod#constructor(Injectable...)`. Given the following constructor:
```java
public class C {
    // ...
    public C(D d, int value) {
        this.d = d;
        this.value = value;
    }
}
```
The corresponding service descriptor would be the following (`C_REF` and `D_REF` being service references to C and D respectively, assuming they are defined as constants):
```java
ServiceDescriptor descriptor = ServiceDescriptor.builder(C_REF)
        .setFactoryMethod(FactoryMethod.constructor(Injectable.ref(D_REF), Injectable.value(123456)))
        .build();
```

> **Tip:** For better code readability, it is recommended to use static imports for `FactoryMethod` and `Injectable` static methods:
> ```java
> import static com.github.alex1304.rdi.config.FactoryMethod.*;
> import static com.github.alex1304.rdi.config.Injectable.*;
>
> // ...
>
> ServiceDescriptor descriptor = ServiceDescriptor.builder(C_REF)
>         .setFactoryMethod(constructor(ref(D_REF), value(123456)))
>         .build();
> ```
> Code examples in this document will make use of static imports going forward.

### Static factory dependencies

Static factories work the same way as constructors, except that they have a name and a return type. Below is an example static factory and the corresponding service descriptor:

```java
public class E {
    // ...
    public static E create(F f, long value) {
        return new E(f, value);
    }
}

// ...
ServiceDescriptor descriptor = ServiceDescriptor.builder(E_REF)
        .setFactoryMethod(staticFactory("create", E.class, ref(F_REF), value(1200L)))
        .build();
```

> **Note:** Static factories might as well be located in a different class than the service itself, for example in an utility class. In that case, use `FactoryMethod#externalStaticFactory` instead which takes the owner class as first argument, the other arguments are the same as for the regular `staticFactory`.

### Setter dependencies

RDI also supports injection via setters. Useful for mutable classes, it works as follows:

```java
public class G {
    // ...
    public void setH(H h) {
        this.h = h;
    }
    public void setCount(int count) {
        this.count = count;
    }
}
// ...
ServiceDescriptor descriptor = ServiceDescriptor.builder(G_REF)
        .addSetterMethod("setH", ref(H_REF))
        .addSetterMethod("setCount", value(1234))
        .build();
```

Unlike constructors and factories, you cannot inject more than 1 dependency per setter, but you can add as many setters as you want. You may even specify the same setter multiple times, useful if the setter performs a "add" operation.
> **Note:** if the return type of the setter is not `void`, which is typically the case for objects that return themselves for chaining purposes, there is an overload that takes the return type as last argument.

## Considerations regarding method lookup

Because RDI uses the Method Handles API behind the scenes to find the injection methods, there are a few considerations to take into account in order not to be surprised with some exceptions:

1. **All injection methods must be public**. This is a requirement to always follow, otherwise the `build()` method on `ServiceDescriptor` will throw an exception.
2. **The injectable parameters must have strictly the same type as the target method's parameters**. The Method Handles lookup API is sensitive to sub-typing and boxing of types. It means that if the method expects an `Object`, the `Injectable` must be an `Object`. If it expects an `int`, you cannot specify an `Integer`. In these cases, you must specify the actual type via `Injectable#ref(ServiceReference<T>, Class<? super T>)` or `Injectable#value(T, Class<? super T>)` to tell RDI that it should look for a supertype. Here's an example to illustrate:
    ```java
    public class A {
        // ...
        public A(Object o) {
            this.o = o;
        }
        public A(B b) {
            throw new RuntimeException("Use the other constructor, thanks");
        }
    }

    // ...
    ServiceDescriptor descriptor = ServiceDescriptor.builder(A_REF)
            .setFactoryMethod(constructor(ref(B_REF, Object.class))) // Add Object.class to specify that you want the first constructor and not the second
            .build();
    ```
    > **Note:** This is also true for return types of static factories and setters, they must exactly match.

## Singletons

The service descriptor builder lets you set whether the service should be a singleton or not. If a service is defined as a singleton, the container will always re-use the same instance when the service is requested or injected. Otherwise, it will always create a new instance and the same instance will never be shared.

By default, all services will be singletons. If you don't want a service to be a singleton, you can specify it like this:

```java
ServiceDescriptor descriptor = ServiceDescriptor.builder(ref)
        .setSingleton(false)
        .build();
```

## Circular dependencies

If you have let's say `A` that depends on itself, or `A` that depends on `B` and `B` that depends on `A`, it is called a **circular dependency**. If RDI detects a circular dependency in a constructor or a static factory, an exception will be thrown when **creating the container**. For setters, it is not an issue, as they are invoked *after* instantiating the object. However, if none of the services involved in the cycle are declared as singleton, it will lead to an exception **at subscription time**, as the container would endlessly create new instance for each service. RDI is able to detect that and throw an exception before it turns into an `OutOfMemoryError`, but keep in mind it can only be detected at subscription time. To avoid the issue, declare your services as singleton whenever possible (which should already be by default), or find an alternative to remove the cycle.

## Convenience methods

The `ServiceDescriptor` has a few convenience methods to create a new service descriptor for common scenarios. The first one is if your service does not require any dependency. In this situation you can use `ServiceDescriptor#standalone(ServiceReference)` to create the descriptor directly without using a builder. The `standalone` method exists in another variant with a `boolean` as second argument if you want it not to be a singleton.
