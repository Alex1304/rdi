---
id: annotation-based-configuration
title: Annotation-based Configuration
---

So far we have been defining the RDI configuration manually with `RdiConfig.builder()`. In this section we will see how it is possible to build the configuration in a more descriptive way to save some time writing boilerplate code.

## Configuring a service with annotations

When creating the RDI configuration manually, the configuration part is effectively decoupled from the service classes themselves. The configuration may be constructed in a separate class, isolated from the rest of your application logic. The tradeoff is that you will have to manually specify where are the constructors, where are the setters, what are the values or references to inject, etc.

If configuration decoupling is not a requirement for your use-case, you may be interested in the **annotation-based approach**. By decorating your service classes with annotations, you are able to generate the RDI configuration in a much more concise and intuitive way. Here is how it works:

```java
@RdiService
public class A {
    private final B b;

    @RdiFactory
    public A(B b) {
        this.b = b;
    }
}

@RdiService
public class B {
}
```

Without the annotations, you would have to construct your configuration object like this:

```java
RdiConfig config = RdiConfig.builder()
        .registerService(ServiceDescriptor.builder(A_REF)
                .setFactoryMethod(FactoryMethod.constructor(Injectable.ref(B_REF)))
                .build())
        .registerService(ServiceDescriptor.standalone(B_REF))
        .build();
```

With the annotations, all you have to do is the following:
```java
Set<Class<?>> classes = new HashSet<>();
classes.add(A.class);
classes.add(B.class);
RdiConfig config = RdiConfig.fromServiceFinder(AnnotationServiceFinder.create(classes));
```

What happened? You simply create a `Set` containing all your annotated classes, and pass it to an `AnnotationServiceFinder`. Easy, isn't it?

But we could push that even further. Indeed, you may still be annoyed by the fact that you still have to manually specify the set of classes. So instead you may use a library that scans your classpath and discover your annotated classes automatically. The most popular solution for that is the [Reflections library](https://github.com/ronmamo/reflections):

```java
Reflections reflections = new Reflections("my.package");
Set<Class<?>> classes = reflections.getTypesAnnotatedWith(RdiService.class);
RdiConfig config = RdiConfig.fromServiceFinder(AnnotationServiceFinder.create(classes));
```

With only these 3 lines of code, any class that you add to your project with a `@RdiService` annotation will automatically be registered as a service!

## Annotations more in detail

There are currently 5 different annotations you can use:

* `@RdiService` - this is placed on the class itself, it indicates that this class represents a service. It accepts an optional value parameter if you want to specify a custom name.
* `@RdiFactory` - this annotation indicates which constructor or static factory to use. There may be only one method with this annotation. If there are several of them, the first one in the order of declaration is taken into account, knowing that static factories always have priority over constructors. If this annotation is completely absent, then the class is expected to have a public no-arg constructor.
* `@RdiSetter` - if you are using setters to inject your dependencies, you must decorate each setter with this annotation.
* `@RdiRef` and `@RdiVal` - those are placed on the parameters of the constructor/factory annotated with `@RdiFactory`. The use of `@RdiRef` is required if the service to inject has a custom name, it can be omitted otherwise. `@RdiVal` is used to inject values, which can be either primitive (`int`, `double`...), a `String` or a supertype of `String` (such as `CharSequence` or even `Object`).

## Limitations

Even if annotations are very useful and can save you a lot of time, it does not fit all use cases. As said previously, by using the annotations you are abandoning the decoupling between your application's classes and the config. In essence, this means that you can only declare your own classes as a service: you cannot declare a class external to your application (e.g a library) as a service using annotations.

Fortunately. RDI supports combining both annotation-based and manual approaches. In the builder of `RdiConfig`, you can simply do this:
```java
RdiConfig config = RdiConfig.builder()
        .fromServiceFinder(AnnotationServiceFinder.create(classes))
        .registerService(/* register an extra service manually here */)
        // ...
        .build();
```

On another note, while giving custom names to annotated services are supported, you will still be limited to 1 service per class. If you want to register 2 or more services of the same class, you would need to manually add service references with different names in the config.
