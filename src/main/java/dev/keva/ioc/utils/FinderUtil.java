package dev.keva.ioc.utils;

import dev.keva.ioc.annotation.Autowired;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class FinderUtil {
    public static Constructor<?> findAnnotatedConstructor(Class<?> clazz) {
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (constructor.isAnnotationPresent(Autowired.class)) {
                constructor.setAccessible(true);
                return constructor;
            }
        }
        return null;
    }

    public static Set<Field> findFields(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        Set<Field> set = new HashSet<>();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(annotationClass)) {
                    field.setAccessible(true);
                    set.add(field);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return set;
    }

    public static Set<Method> findMethods(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        Set<Method> set = new HashSet<>();
        while (clazz != null) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(annotationClass)) {
                    method.setAccessible(true);
                    set.add(method);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return set;
    }
}
