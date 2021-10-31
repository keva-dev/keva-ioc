package dev.keva.ioc.configbean;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.ioc.configbean.entity.DB;

@Component
public class AnotherClientClass {
    @Autowired
    DB db;

    public String scan() {
        return "Scanning for " + db.getDB();
    }
}
