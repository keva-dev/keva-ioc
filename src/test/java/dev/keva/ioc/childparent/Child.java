package dev.keva.ioc.childparent;

import dev.keva.ioc.annotation.Component;

@Component
public class Child extends Parent {
    public String test() {
        return dependency.getName();
    }
}
