package dev.keva.ioc;

import dev.keva.ioc.annotation.ComponentScan;
import dev.keva.ioc.multiple.Browser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ComponentScan("dev.keva.ioc.multiple")
class PackageMultipleTest {
    static KevaIoC kevaIoC;

    @BeforeAll static void init() {
        kevaIoC = KevaIoC.initBeans(PackageMultipleTest.class);
    }

    @Test void testMultipleClassConstruct() {
        Browser browser = kevaIoC.getBean(Browser.class);
        String text = browser.run();
        assertEquals("This browser run on V8 and CircularDependencyOfV8Engine", text);
    }
}
