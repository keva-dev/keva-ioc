package dev.keva.ioc.circular;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;

@Component
public class ClassA {
    private final ClassB classB;

    @Autowired
    public ClassA(ClassB classB) {
        this.classB = classB;
    }

    public void methodA() {
        classB.methodB();
    }
}
