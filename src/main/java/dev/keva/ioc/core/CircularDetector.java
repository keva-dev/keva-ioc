package dev.keva.ioc.core;

import dev.keva.ioc.exception.IoCCircularDepException;

import java.util.HashMap;
import java.util.Map;

public class CircularDetector {
    private final Map<Class<?>, Integer> circularDetectMap = new HashMap<>(30);

    public void detect(Class<?> clazz) throws IoCCircularDepException {
        int circular = circularDetectMap.getOrDefault(clazz, 0);
        circularDetectMap.put(clazz, circular + 1);
        // Need to be changed
        if (circular > 50) {
            throw new IoCCircularDepException("Circular dependency detected when loading class " + clazz.getName());
        }
    }
}
