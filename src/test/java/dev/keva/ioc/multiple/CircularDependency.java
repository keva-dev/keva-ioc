package dev.keva.ioc.multiple;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;

@Component
public class CircularDependency {
    @Autowired
    private V8Engine v8Engine;

    public String getName() {
        return "CircularDependency" + "Of" + v8Engine.getClass().getSimpleName();
    }
}
