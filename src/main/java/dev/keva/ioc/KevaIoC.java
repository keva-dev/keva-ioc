package dev.keva.ioc;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.ioc.annotation.ComponentScan;
import dev.keva.ioc.annotation.Qualifier;
import dev.keva.ioc.exception.IoCException;
import dev.keva.ioc.utils.ClassLoaderUtil;
import org.reflections.Reflections;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class KevaIoC {
    private final Map<Class<?>, Class<?>> implementationsMap;
    private final Map<Class<?>, Object> beansMap;
    private final Map<Class<?>, Integer> circularDetectMap;

    private static KevaIoC kevaIoc;

    private KevaIoC() {
        implementationsMap = new HashMap<>();
        beansMap = new HashMap<>();
        circularDetectMap = new HashMap<>();
    }

    public static KevaIoC initBeans(Class<?> mainClass) {
        try {
            synchronized (KevaIoC.class) {
                if (kevaIoc == null) {
                    kevaIoc = new KevaIoC();
                    kevaIoc.initWrapper(mainClass);
                }
            }
            return kevaIoc;
        } catch (IOException | ClassNotFoundException | InstantiationException |
                IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IoCException(e);
        }
    }

    public <T> T getBean(Class<T> clazz) {
        try {
            return kevaIoc._getBean(clazz);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IoCException(e);
        }
    }

    private void initWrapper(Class<?> mainClass) throws IOException, ClassNotFoundException, InstantiationException,
            IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        ComponentScan scan = mainClass.getAnnotation(ComponentScan.class);
        if (scan != null) {
            String[] packages = scan.value();
            System.out.println(Arrays.toString(packages));
            for (String packageName : packages) {
                init(packageName);
            }
        } else {
            init(mainClass.getPackage().getName());
        }
    }

    private void init(String packageName) throws IOException, ClassNotFoundException, InstantiationException,
            IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        List<Class<?>> classes = ClassLoaderUtil.getClasses(packageName);
        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> types = reflections.getTypesAnnotatedWith(Component.class);
        for (Class<?> implementationClass : types) {
            Class<?>[] interfaces = implementationClass.getInterfaces();
            if (interfaces.length == 0) {
                implementationsMap.put(implementationClass, implementationClass);
            } else {
                for (Class<?> interfaceClass : interfaces) {
                    implementationsMap.put(implementationClass, interfaceClass);
                }
            }
        }

        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Component.class)) {
                newInstanceWrapper(clazz);
            }
        }
    }

    private Object newInstanceWrapper(Class<?> clazz) throws InvocationTargetException,
            IllegalAccessException, InstantiationException, NoSuchMethodException {
        if (beansMap.containsKey(clazz)) {
            return beansMap.get(clazz);
        }

        int circular = circularDetectMap.getOrDefault(clazz, 0);
        circularDetectMap.put(clazz, circular + 1);
        // Need to be changed
        if (circular > 50) {
            throw new IoCException("Circular dependency detected when loading class " + clazz.getName());
        }

        Object instance = newInstance(clazz);
        beansMap.put(clazz, instance);
        fieldInject(clazz, instance);
        setterInject(clazz, instance);
        return instance;
    }

    private Object newInstance(Class<?> clazz) throws IllegalAccessException,
            InstantiationException, InvocationTargetException, NoSuchMethodException {
        Constructor<?> annotatedConstructor = findAnnotatedConstructor(clazz);
        Object instance;
        if (annotatedConstructor == null) {
            try {
                Constructor<?> defaultConstructor = clazz.getConstructor();
                defaultConstructor.setAccessible(true);
                instance = defaultConstructor.newInstance();
                return instance;
            } catch (NoSuchMethodException e) {
                throw new IoCException("There is no default constructor in class " + clazz.getName());
            }
        } else {
            Object[] parameters = new Object[annotatedConstructor.getParameterCount()];
            for (int i = 0; i < parameters.length; i++) {
                String qualifier = annotatedConstructor.getParameters()[i].isAnnotationPresent(Qualifier.class) ?
                        annotatedConstructor.getParameters()[i].getAnnotation(Qualifier.class).value() : null;
                Object depInstance = _getBean(annotatedConstructor.getParameterTypes()[i], annotatedConstructor.getParameterTypes()[i].getName(), qualifier);
                parameters[i] = depInstance;
            }
            instance = annotatedConstructor.newInstance(parameters);
        }
        return instance;
    }

    private Constructor<?> findAnnotatedConstructor(Class<?> clazz) {
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (constructor.isAnnotationPresent(Autowired.class)) {
                constructor.setAccessible(true);
                return constructor;
            }
        }
        return null;
    }

    private void setterInject(Class<?> clazz, Object classInstance) throws IllegalAccessException,
            InvocationTargetException, NoSuchMethodException, InstantiationException {
        Set<Method> methods = findMethods(clazz);
        for (Method method : methods) {
            if (method.isAnnotationPresent(Autowired.class)) {
                method.setAccessible(true);
                Object[] parameters = new Object[method.getParameterCount()];
                for (int i = 0; i < parameters.length; i++) {
                    String qualifier = method.getParameters()[i].isAnnotationPresent(Qualifier.class) ?
                            method.getParameters()[i].getAnnotation(Qualifier.class).value() : null;
                    Object instance = _getBean(method.getParameterTypes()[i], method.getParameterTypes()[i].getName(), qualifier);
                    parameters[i] = instance;
                }
                method.invoke(classInstance, parameters);
            }
        }
    }

    private Set<Method> findMethods(Class<?> clazz) {
        Set<Method> set = new HashSet<>();
        while (clazz != null) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Autowired.class)) {
                    method.setAccessible(true);
                    set.add(method);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return set;
    }

    private void fieldInject(Class<?> clazz, Object classInstance) throws IllegalAccessException,
            InstantiationException, InvocationTargetException, NoSuchMethodException {
        Set<Field> fields = findFields(clazz);
        for (Field field : fields) {
            String qualifier = field.isAnnotationPresent(Qualifier.class) ? field.getAnnotation(Qualifier.class).value() : null;
            Object fieldInstance = _getBean(field.getType(), field.getName(), qualifier);
            field.set(classInstance, fieldInstance);
        }
    }

    private Set<Field> findFields(Class<?> clazz) {
        Set<Field> set = new HashSet<>();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    field.setAccessible(true);
                    set.add(field);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return set;
    }

    @SuppressWarnings("unchecked")
    private <T> T _getBean(Class<T> interfaceClass) throws InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException {
        return (T) _getBean(interfaceClass, null, null);
    }

    private <T> Object _getBean(Class<T> interfaceClass, String fieldName, String qualifier) throws
            InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> implementationClass = interfaceClass.isInterface() ?
                getImplementationClass(interfaceClass, fieldName, qualifier) : interfaceClass;
        if (beansMap.containsKey(implementationClass)) {
            return beansMap.get(implementationClass);
        }
        synchronized (beansMap) {
            return newInstanceWrapper(implementationClass);
        }
    }

    private Class<?> getImplementationClass(Class<?> interfaceClass, final String fieldName, final String qualifier) {
        Set<Map.Entry<Class<?>, Class<?>>> implementationClasses =
                implementationsMap.entrySet().stream().filter(entry ->
                        entry.getValue() == interfaceClass).collect(Collectors.toSet());
        String errorMessage;
        if (implementationClasses.isEmpty()) {
            errorMessage = "No implementation found for interface " + interfaceClass.getName();
        } else if (implementationClasses.size() == 1) {
            Optional<Map.Entry<Class<?>, Class<?>>> optional = implementationClasses.stream().findFirst();
            return optional.get().getKey();
        } else {
            final String findBy = (qualifier == null || qualifier.trim().length() == 0) ? fieldName : qualifier;
            Optional<Map.Entry<Class<?>, Class<?>>> optional =
                    implementationClasses.stream()
                            .filter(entry ->
                                    entry.getKey().getSimpleName()
                                            .equalsIgnoreCase(findBy)).findAny();
            if (optional.isPresent()) {
                return optional.get().getKey();
            } else {
                errorMessage = "There are " + implementationClasses.size()
                        + " of interface " + interfaceClass.getName()
                        + " Expected single implementation or make use of"
                        + " @Qualifier to resolve conflict";
            }
        }
        throw new IoCException(errorMessage);
    }
}
