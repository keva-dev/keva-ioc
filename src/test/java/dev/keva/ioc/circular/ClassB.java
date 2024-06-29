package dev.keva.ioc.circular;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;

@Component
public class ClassB {
    private final ClassA classA;

    @Autowired
    public ClassB(ClassA classA) {
        this.classA = classA;
    }

    public void methodB() {
        classA.methodA();
    }
}
