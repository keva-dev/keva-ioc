package dev.keva.ioc.multiple;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;

@Component
public class V8Engine implements Engine {
    private final CircularDependency circularDependency;

    @Autowired
    public V8Engine(CircularDependency circularDependency) {
        this.circularDependency = circularDependency;
    }

    public String getName() {
        return "V8 and " + circularDependency.getName();
    }
}
