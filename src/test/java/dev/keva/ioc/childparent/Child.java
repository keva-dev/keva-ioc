package dev.keva.ioc.childparent;

public class Child extends Parent {
    public String test() {
        return dependency.getName();
    }
}
