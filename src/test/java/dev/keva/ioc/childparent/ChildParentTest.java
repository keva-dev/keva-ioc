package dev.keva.ioc.childparent;

import dev.keva.ioc.KevaIoC;
import dev.keva.ioc.annotation.ComponentScan;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ComponentScan("dev.keva.ioc.childparent")
public class ChildParentTest {
    static KevaIoC kevaIoC;

    @BeforeAll
    static void init() {
        kevaIoC = KevaIoC.initBeans(ChildParentTest.class);
    }

    @Test
    void testChildParent() {
        Child child = kevaIoC.getBean(Child.class);
        String text = child.test();
        assertEquals("Dependency", text);
    }
}
