package dev.keva.ioc;

import dev.keva.ioc.annotation.ComponentScan;
import dev.keva.ioc.single.TestComponent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ComponentScan("dev.keva.ioc.single")
public class PackageSingleTest {
    static KevaIoC kevaIoC;

    @BeforeAll
    static void init() {
        kevaIoC = KevaIoC.initBeans(PackageMultipleTest.class);
    }

    @Test
    void testSingleClassConstruct() {
        TestComponent testComponent = kevaIoC.getBean(TestComponent.class);
        String name = testComponent.getName();
        assertEquals("Test", name);
    }
}
