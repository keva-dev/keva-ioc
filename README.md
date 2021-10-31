# Keva IoC

Fast, lightweight, Spring like, annotation driven IoC framework.

Designed specifically for small applications, that has to have small memory footprint, small jar size and fast startup time,
for example plugins, (embedded) standalone application, integration tests, jobs, Android applications, etc.

While Spring IoC is great, but even with absolute minimal number of dependencies has a large size (in jars), it also takes long to start it up.
Spring is very opinionated, it's easy to be locked-in Spring ecosystem. Also, it has fairly large memory footprint that is not good for embedded applications.

## Features

- Spring-like annotation-support, no XML
- Fast startup time, small memory footprint (see performance section soon)
- Pocket-sized, only basic features (no bean's lifecycle, no "Spring's magic")

## Supported Annotations

- `@ComponentScan`
- `@Component`
- `@Configuration`
- `@Bean`
- `@Autowired` (supports field injection, setter injection and constructor injection)
- `@Qualifier`

## Install

`build.gradle`

```groovy
repositories {
    // ...
    maven { url "https://s01.oss.sonatype.org/content/repositories/snapshots" }
}

dependencies {
    implementation 'dev.keva:keva-ioc:0.1.0-SNAPSHOT'
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
    private String version;
    
    private final Engine engine;
    private final BrowserRenderer renderer;

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
