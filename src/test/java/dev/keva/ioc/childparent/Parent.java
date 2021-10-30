package dev.keva.ioc.childparent;

import dev.keva.ioc.annotation.Autowired;

public class Parent {
    protected Dependency dependency;

    @Autowired
    public void setDependency(Dependency dependency) {
        this.dependency = dependency;
    }
}
