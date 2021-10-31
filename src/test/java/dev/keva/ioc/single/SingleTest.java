package dev.keva.ioc.single;

import dev.keva.ioc.KevaIoC;
import dev.keva.ioc.annotation.ComponentScan;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ComponentScan("dev.keva.ioc.single")
public class SingleTest {
    static KevaIoC kevaIoC;

    @BeforeAll
    static void init() {
        kevaIoC = KevaIoC.initBeans(SingleTest.class);
    }

    @Test
    void testSingleClassConstruct() {
        TestComponent testComponent = kevaIoC.getBean(TestComponent.class);
        String name = testComponent.getName();
        assertEquals("Test", name);
    }
}
