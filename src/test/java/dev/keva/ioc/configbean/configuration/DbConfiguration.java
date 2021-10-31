package dev.keva.ioc.configbean.configuration;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Bean;
import dev.keva.ioc.annotation.Configuration;
import dev.keva.ioc.configbean.entity.App;
import dev.keva.ioc.configbean.entity.DB;

@Configuration
public class DbConfiguration {
    @Autowired App app;

    @Bean
    public DB getDB() {
        return new DB(app);
    }
}
