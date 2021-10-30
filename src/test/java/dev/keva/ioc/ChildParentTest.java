package dev.keva.ioc;

import dev.keva.ioc.annotation.ComponentScan;
import dev.keva.ioc.childparent.Child;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ComponentScan("dev.keva.ioc.childparent")
public class ChildParentTest {
    static KevaIoC kevaIoC;

    @BeforeAll
    static void init() {
        kevaIoC = KevaIoC.initBeans(PackageMultipleTest.class);
    }

    @Test
    void testChildParent() {
        Child child = kevaIoC.getBean(Child.class);
        String text = child.test();
        assertEquals("Dependency", text);
    }
}
