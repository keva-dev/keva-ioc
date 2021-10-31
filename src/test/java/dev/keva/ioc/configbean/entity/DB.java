package dev.keva.ioc.configbean.entity;

public class DB {
    private final String DB;

    public DB(App config) {
        DB = "DB for app version " + config.getApp();
    }

    public String getDB() {
        return DB;
    }
}
