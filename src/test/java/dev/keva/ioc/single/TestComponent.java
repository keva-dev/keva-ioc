package dev.keva.ioc.single;

import dev.keva.ioc.annotation.Component;

@Component
public class TestComponent {
    public String getName() {
        return "Test";
    }
}
