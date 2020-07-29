---
id: dynamic-configuration
title: Dynamic Configuration
---

So far we have been defining the RDI configuration directly in the Java code. In this section we will see how it is possible to build the configuration dynamically, to make it more flexible and fit more use cases.

## Building the configuration using a description language

The `RdiConfig` class is instantiated using a builder pattern, meaning that there is an intermediate mutable object that is mutated step-by-step until the configuration is complete and the `build()` method is called. This approach paves the way for the use of description languages to perform this step-by-step build, to save even more time when configuring services.

Other dependency injection frameworks do that, with Spring you can declare your beans in **XML files** as well as via **Java annotations**.

Unfortunately, RDI does not yet natively provide a way to achieve configuration with such description languages, but it is worth to mention it here because the builder pattern really can pave the way for many possibilities.

## Building the configuration in a multi module/plugin project

If there is one thing that is already possible to do however, it's the ability to declare service descriptors in different modules (which may even be in different JARs), and collect all of them in your main module where the container is supposed to live. You can do that by exposing an interface that would look like this:

```java
public interface ServiceDescriptorProvider {

    ServiceDescriptor provide(); // you may as well put some sort of Context class as parameter, as you see fit
}
```

In your different JARs you can give various implementations of this interface, and declare them as a service by creating a file in the `/META-INF/services/` folder of your JAR:

```
META-INF/
   |
   |__ services/
          |
          |__ com.myapp.ServiceDescriptorProvider
```

with `com.myapp.ServiceDescriptorProvider` being a new file named after the fully qualified name of the interface above, with the following contents:

```
com.myapp.MyServiceDescriptorProvider
```
which corresponds to the fully qualified name of the implementation class.

Once you have this configured you may use `ServiceLoader` in your main module and assemble the configuration:

```java
RdiConfig.Builder builder = RdiConfig.builder();
for (ServiceDescriptorProvider provider : ServiceLoader.load(ServiceDescriptorProvider.class)) {
    builder.registerService(provider.provide());
}
RdiConfig config = builder.build();
```

> **Note:** This method is outdated since Java 9 with the introduction of Java modules. Instead of exporting services via the `/META-INF/services/` directory, they are declared in the module-info.java with `provides` and `uses` directives. Learn more about it [here](https://blog.frankel.ch/migrating-serviceloader-java-9-module-system/).
