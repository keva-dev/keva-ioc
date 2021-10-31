package dev.keva.ioc.configbean;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.ioc.configbean.entity.App;
import dev.keva.ioc.configbean.entity.DB;

@Component
public class ClientClass {
    @Autowired
    private DB db;

    @Autowired
    private App app;

    @Autowired
    AnotherClientClass anotherClientClass;

    public String run() {
        return app.getApp() + " | " + db.getDB();
    }

    public String runScan() {
        return anotherClientClass.scan();
    }
}
