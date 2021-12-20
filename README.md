# Keva IoC

Fast, lightweight, Spring like, annotation driven IoC framework.

Designed specifically for small applications, that has to have small memory footprint, small jar size and fast startup time,
for example plugins, (embedded) standalone application, integration tests, jobs, Android applications, etc.

While Spring IoC is great, but even with absolute minimal number of dependencies has a large size (in jars), it also takes long to start it up.
Spring is very opinionated, it's easy to be locked-in Spring ecosystem. Also, it has fairly large memory footprint that is not good for embedded applications.

![GitHub Workflow Status (branch)](https://img.shields.io/github/workflow/status/keva-dev/keva-ioc/Build/master?label=build&style=flat-square)
![Lines of code](https://img.shields.io/tokei/lines/github/keva-dev/keva-ioc?style=flat-square)
![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/keva-dev/keva-ioc?style=flat-square)
![GitHub](https://img.shields.io/github/license/keva-dev/keva-ioc?style=flat-square)
![Maven Central](https://img.shields.io/maven-central/v/dev.keva/keva-ioc?style=flat-square)

## Features

- Spring-like annotation-support, no XML
- Fast startup time, small memory footprint (see performance section soon)
- Pocket-sized, only basic features (no bean's lifecycle, no "Spring's magic")
- Less opinionated, support mount existing beans (means can integrate well with other IoC/DI frameworks)

## Supported Annotations

- `@ComponentScan`
- `@Component`
- `@Configuration`
- `@Bean`
- `@Autowired` (supports field injection, setter injection and constructor injection)
- `@Qualifier`
- Support mount existing beans via `.initBeans(Main.class, beanOne, beanTwo...)` static method

## Install

`build.gradle`

```groovy
dependencies {
    implementation 'dev.keva:keva-ioc:1.0.1'
}
```

## Usages

Engine.java

```java
public interface Engine {
    String getName();
}
```

V8Engine.java

```java
@Component
public class V8Engine implements Engine {
    public String getName() {
        return "V8";
    }
}
```

SpiderMonkeyEngine.java

```java
@Component
public class SpiderMonkeyEngine implements Engine {
    public String getName() {
        return "SpiderMonkey";
    }
}
```

Config.java

```java
@Configuration
public class Configuration {
    @Bean("version")
    public String version() {
        return "1.0";
    }
}
```

Browser.java

```java
@Component
public class Browser {
    @Autowired
    String version;
    
    Engine engine;
    BrowserRenderer renderer;

    @Autowired
    public Browser(@Qualifier("v8Engine") Engine engine, BrowserRenderer renderer) {
        this.engine = engine;
        this.renderer = renderer;
    }

    public String run() {
        return renderer.render("This browser run on " + engine.getName());
    }
    
    public String getVersion() {
        return renderer.render("Browser version: " + version);
    }
}
```

Main.java

```java
public class Main {
    public static void main(String[] args) {
        KevaIoC context = KevaIoC.initBeans(Main.class);
        Browser browser = context.getBean(Browser.class);
        System.out.println(browser.run());
    }
}
```
