# RDI

Dependency Injection library with reactive capabilities, powered by Reactor.

![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/Alex1304/rdi?sort=semver)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.alex1304/rdi)](https://search.maven.org/artifact/com.github.alex1304/rdi)
![License](https://img.shields.io/github/license/Alex1304/rdi)
[![javadoc](https://javadoc.io/badge2/com.github.alex1304/rdi/javadoc.svg)](https://javadoc.io/doc/com.github.alex1304/rdi) 

<img align="right" src="https://user-images.githubusercontent.com/10291761/88860458-11decf80-d1fc-11ea-8fea-d90cf90ee399.png" width=20% />

## What is RDI?

RDI stands for Reactive Dependency Injection. It is a library allowing to manage the instantiation of beans, services, and any kind of Java object living in your application simply by defining their dependencies. The specificity of RDI is that it fully supports the reactive programming paradigm, as defined by the [Reactive Streams specification](https://www.reactive-streams.org/), allowing to make efficient and non-blocking applications with backpressure handling.

You can check the full RDI documentation [here](https://alex1304.github.io/rdi/docs/intro).

## Dependency injection in a nutshell

The principle of dependency injection isn't new. The way it works is similar to [Spring's IoC Container](https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#beans), as well as some other frameworks featuring a such mechanism like Quarkus or JSF. Unlike these examples however, RDI isn't a full-fledged framework, but rather a lightweight library 100% focused on dependency injection.

To illustrate how dependency injection operates, consider the following basic classes:

```java
public class A {
    private final B b;
    public A(B b) {
        this.b = b;
    }
}

public class B {
}
```

You can see that A needs an instance of B in the constructor. Normally you would do something along these lines:

```java
B b = new B();
A a = new A(b);
// use A to do stuff
```

We are here instantiating A and B in the same place, but you can imagine B is instantiated elsewhere in the application. As a result, it can turn quite difficult to keep track of the lifecycle and the scope of each object, especially in large apps. Dependency injection naturally comes up as a solution when you want to decouple the initialization part from the core logic of your objects.

With a dependency injection library, getting an instance of A would be like this:
```java
A a = container.get(A.class);
```

`container` here would be an object that is aware of the existence of A and B class, as well as the details about constructor arguments. With all that information, all you need to do is to ask the container "Hey, can I get an instance of A please?" and it will execute. This approach has many advantages other than centralizing instantiation of objects into one place. You can for example tell the container whether it should always return the same instance of A or instantiate a new one each time the object is requested. If it should return the same instance, that's something you would normally do with a singleton pattern, which can cause issues especially in a case of concurrent access by multiple threads. A container would be able to handle that thread safety aspect for you, preferably in a lock-free manner so that it doesn't degrade the performances of your application.

## Overview of how RDI works

The way RDI implements this principle does not differ much from what already exists in terms of dependency injection. As such, RDI comes with the concept of *container*, the main object that will manage the initialization of all objects in your app and that will take care of injecting the necessary dependencies. Dependencies may be specified in constructor arguments, in setter arguments or in factory methods. For the previous example, configuring a container with RDI would look like this:

```java
// Define service references as constants
public static final ServiceReference<A> A_REF = ServiceReference.ofType(A.class);
public static final ServiceReference<B> B_REF = ServiceReference.ofType(B.class);

// Create the config
// We register both A and B, and specify that we should inject B in the constructor of A
RdiConfig config = RdiConfig.builder()
        .registerService(ServiceDescriptor.builder(A_REF)
                .setFactoryMethod(FactoryMethod.constructor(Injectable.ref(B_REF)))
                .build())
        .registerService(ServiceDescriptor.standalone(B_REF))
        .build();
// Create the container by passing the config
RdiServiceContainer container = RdiServiceContainer.create(config);
// Get an instance of A. The dependency injection will operate and A will be ready to use!
A a = container.getService(A_REF).block(); // Remember RDI is reactive. Here we block until A is fully created.
```

Here is some quick explanation of this code:
* Objects managed by the RDI container are called **services**. Services are referred to by their **reference**, defining their type and optionally their name (if you want to define more than one service of the same type you may give them unique names, by default it uses the name of the class).
* Before creating the container, we need a way to tell it what are the different services available and how to inject the dependencies they may have. This is done via the `RdiConfig` object, where you register a **descriptor** for each service defining the dependencies to inject.
* You then pass the config to create the **container**. Dependency resolution and circular dependency checks are performed at creation time.
* Once your container is ready, you may call `getService` with the reference of the service you want, and it will prepare the instance with all the dependencies injected for you. The particularity here is that it doesn't return the instance directly due to the reactive nature of RDI. To get the instance you must **subscribe** to the reactive stream returned by `getService` (in this case, it is a `Mono` from [Reactor Core](https://projectreactor.io), a popular implementation of the [Reactive Streams specification](https://www.reactive-streams.org/)). Subscribing is done either via `.subscribe(Consumer)` (which does not block and invokes the consumer when the object is ready), or via `.block()` (blocks the program and wait until the object is ready).

In the above example we only instantiated objects via public constructors. The most interesting part of RDI is when a service needs to be instantiated with a factory method returning a publisher of it. Let's enrich the `B` class with the following:

```java
public class B {
    private B() {}

    public static Mono<B> create() {
        return Mono.fromCallable(B::new);
    }
}
```

The `create` method is a static factory that returns a publisher of `B` (in this case a `Mono`, but can be anything implementing `org.reactivestreams.Publisher`). You can imagine that the factory method performs some webservice call or other reactive task before creating the actual instance of B. RDI is able to handle this kind of factory out of the box. The `RdiConfig` would now look like this:

```java
RdiConfig config = RdiConfig.builder()
        .registerService(ServiceDescriptor.builder(A_REF)
                .setFactoryMethod(FactoryMethod.constructor(Injectable.ref(B_REF)))
                .build())
        .registerService(ServiceDescriptor.builder(B_REF)
                .setFactoryMethod(FactoryMethod.staticFactory("create", Mono.class))
                .build())
        .build();
```

When requesting an instance of A, the container will first subscribe to the Mono returned by `B.create()`, and then it will inject the obtained B instance into A before returning A. If you have many services like this in you application, RDI may save you a lot of time assembling the reactive chains to get your service instances.


**Note:** Since version `1.1.0`, RDI also supports [annotation-based configuration](https://alex1304.github.io/rdi/docs/annotation-based-configuration) of the container to save even more time.

## Getting Started

### Prerequisites

To add RDI to your project, you must be using a dependency management tool such as [Maven](https://maven.apache.org) or [Gradle](https://gradle.org). You also need the **JDK 8 or above**.

### Install using Maven

Here is the dependency to add in your `pom.xml`:

```xml
<dependency>
    <groupId>com.github.alex1304</groupId>
    <artifactId>rdi</artifactId>
    <version>[VERSION]</version>
</dependency>
```
Replace `[VERSION]` with the latest version available on Maven Central, as shown here: [![Maven Central](https://img.shields.io/maven-central/v/com.github.alex1304/rdi)](https://search.maven.org/artifact/com.github.alex1304/rdi)

### Install using Gradle

If you are using Gradle, here is what to put in `build.gradle`:

```groovy
repositories {
      mavenCentral()
}

dependencies {
      implementation 'com.github.alex1304:rdi:[VERSION]'
}
```

Replace `[VERSION]` with the latest version as explained above.

RDI should now be downloaded by your IDE and you are now ready to use it.

## Useful links

* [Full RDI documentation](https://alex1304.github.io/rdi/docs/intro)
* [Javadoc](https://www.javadoc.io/doc/com.github.alex1304/rdi/latest/index.html)
* [Reactor Documentation](https://projectreactor.io/docs/core/release/reference/)

## License

This project is licensed under the MIT license.

## Contributions

Have a feature to suggest or a bug to report ? Issues and pull requests are more than welcome! Make sure to follow the template and share your ideas.

## Contact

If you wish to contact me directly, you can DM me on Discord (Alex1304#9704) or send an email to mirandaa1304@gmail.com. Depending on how this project turns out, a community Discord server can be considered for the future.
