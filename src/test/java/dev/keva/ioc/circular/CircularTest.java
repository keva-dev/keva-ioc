package dev.keva.ioc.circular;

import dev.keva.ioc.KevaIoC;
import dev.keva.ioc.annotation.ComponentScan;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentScan("dev.keva.ioc.circular")
public class CircularTest {

    @Test
    void testCircular() {
        assertThrows(dev.keva.ioc.exception.IoCException.class, () -> {
            KevaIoC kevaIoC = KevaIoC.initBeans(CircularTest.class);
        });
    }
}
