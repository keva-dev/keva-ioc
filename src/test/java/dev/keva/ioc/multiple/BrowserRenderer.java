package dev.keva.ioc.multiple;

import dev.keva.ioc.annotation.Component;

@Component
public class BrowserRenderer {
    public String render(String content) {
        return content;
    }
}
