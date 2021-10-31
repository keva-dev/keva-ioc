package dev.keva.ioc.core;

import dev.keva.ioc.exception.IoCException;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class BeanContainer {
    public final Map<Class<?>, Map<String, Object>> beans = new HashMap<>(10);

    public void putBean(Class<?> clazz, Object instance) {
        putBean(clazz, instance, clazz.getName());
    }

    public void putBean(Class<?> clazz, Object instance, String name) {
        Map<String, Object> map = beans.computeIfAbsent(clazz, k -> new TreeMap<>());
        map.put(name, instance);
    }

    public boolean containsBean(Class<?> clazz) {
        return containsBean(clazz, clazz.getName());
    }

    public boolean containsBean(Class<?> clazz, String name) {
        return beans.get(clazz) != null;
    }

    public Object getBean(Class<?> clazz) {
        return getBean(clazz, clazz.getName());
    }

    public Object getBean(Class<?> clazz, String name) {
        Map<String, Object> map = beans.get(clazz);

        if (map == null || map.size() == 0) {
            throw new IoCException("No bean found for class " + clazz);
        }

        if (map.size() == 1) {
            return map.values().iterator().next();
        }

        Object bean = map.get(name);
        if (bean == null) {
            String errorMessage = "There are " + map.size()
                    + " of bean " + name
                    + " Expected single implementation or make use of"
                    + " @Qualifier to resolve conflict";
            throw new IoCException(errorMessage);
        }

        return bean;
    }
}
