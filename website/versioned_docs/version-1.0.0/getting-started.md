---
id: version-1.0.0-getting-started
title: Getting Started
original_id: getting-started
---

## Prerequisites

To add RDI to your project, you must be using a dependency management tool such as [Maven](https://maven.apache.org) or [Gradle](https://gradle.org). You also need the **JDK 8 or above**.

## Install using Maven

Here is the dependency to add in your `pom.xml`:

```xml
<dependency>
    <groupId>com.github.alex1304</groupId>
    <artifactId>rdi</artifactId>
    <version>[VERSION]</version>
</dependency>
```
Replace `[VERSION]` with the latest version available on Maven Central, as shown here: [![Maven Central](https://img.shields.io/maven-central/v/com.github.alex1304/rdi)](https://search.maven.org/artifact/com.github.alex1304/rdi)

## Install using Gradle

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

RDI should now be downloaded by your IDE and you are now ready to use it. In the next section we will focus on the core features of the library.
