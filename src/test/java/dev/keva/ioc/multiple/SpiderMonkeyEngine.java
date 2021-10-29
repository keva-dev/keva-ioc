package dev.keva.ioc.multiple;

import dev.keva.ioc.annotation.Component;

@Component
public class SpiderMonkeyEngine implements Engine {
    public String getName() {
        return "SpiderMonkey";
    }
}
