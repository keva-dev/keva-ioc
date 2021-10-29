# Keva IoC

Fast, lightweight, Spring like, annotation driven IoC framework.

Designed specifically for small applications, that has to have small memory footprint, small jar size and fast startup time,
for example plugins, (embedded) standalone application, integration tests, jobs, Android applications, etc.

Supported annotations:

- `@ComponentScan`
- `@Component`
- `@Autowired` (field injection, setter injection and constructor injection)
- `@Qualifier`

## Usage

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

Browser.java

```java
@Component
public class Browser {
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
