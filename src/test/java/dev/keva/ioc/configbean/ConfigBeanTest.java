package dev.keva.ioc.configbean;

import dev.keva.ioc.KevaIoC;
import dev.keva.ioc.annotation.ComponentScan;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ComponentScan("dev.keva.ioc.configbean")
public class ConfigBeanTest {
    static KevaIoC kevaIoC;

    @BeforeAll
    static void init() {
        kevaIoC = KevaIoC.initBeans(ConfigBeanTest.class);
    }

    @Test
    void testSingleClassConstruct() {
        ClientClass clientClass = kevaIoC.getBean(ClientClass.class);
        String run = clientClass.run();
        assertEquals("0 | DB for app version 0", run);
        String scan = clientClass.runScan();
        assertEquals("Scanning for DB for app version 0", scan);
    }
}
