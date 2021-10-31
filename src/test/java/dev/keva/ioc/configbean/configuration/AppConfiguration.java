package dev.keva.ioc.configbean.configuration;

import dev.keva.ioc.annotation.Bean;
import dev.keva.ioc.annotation.Configuration;
import dev.keva.ioc.configbean.entity.App;

@Configuration
public class AppConfiguration {
    @Bean
    public App getApp() {
        return new App();
    }
}
