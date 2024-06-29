package dev.keva.ioc.core;

import dev.keva.ioc.exception.IoCCircularDepException;

import java.util.*;

public class CircularDependencyDetector {
    private final Set<Class<?>> instantiationInProgress = new HashSet<>();

    public void startInstantiation(Class<?> clazz) throws IoCCircularDepException {
        if (instantiationInProgress.contains(clazz)) {
            throw new IoCCircularDepException("Circular dependency detected while instantiating " + clazz.getName());
        }
        instantiationInProgress.add(clazz);
    }

    public void finishInstantiation(Class<?> clazz) {
        instantiationInProgress.remove(clazz);
    }
}
