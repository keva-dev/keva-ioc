package dev.keva.ioc;

import dev.keva.ioc.annotation.*;
import dev.keva.ioc.core.BeanContainer;
import dev.keva.ioc.core.CircularDetector;
import dev.keva.ioc.core.ImplementationContainer;
import dev.keva.ioc.exception.IoCBeanNotFound;
import dev.keva.ioc.exception.IoCCircularDepException;
import dev.keva.ioc.exception.IoCException;
import dev.keva.ioc.utils.ClassLoaderUtil;
import dev.keva.ioc.utils.FinderUtil;
import org.reflections.Reflections;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class KevaIoC {
    private final BeanContainer beanContainer = new BeanContainer();
    private final ImplementationContainer implementationContainer = new ImplementationContainer();
    private final CircularDetector circularDetector = new CircularDetector();

    private KevaIoC() {
    }

    public static KevaIoC initBeans(Class<?> mainClass) {
        try {
            KevaIoC instance = new KevaIoC();
            instance.initWrapper(mainClass);
            return instance;
        } catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException |
                InvocationTargetException | NoSuchMethodException | IoCBeanNotFound | IoCCircularDepException e) {
            throw new IoCException(e);
        }
    }

    public <T> T getBean(Class<T> clazz) {
        try {
            return _getBean(clazz);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                NoSuchMethodException | IoCBeanNotFound | IoCCircularDepException e) {
            throw new IoCException(e);
        }
    }

    private void initWrapper(Class<?> mainClass) throws IOException, ClassNotFoundException, InstantiationException,
            IllegalAccessException, NoSuchMethodException, InvocationTargetException, IoCBeanNotFound, IoCCircularDepException {
        ComponentScan scan = mainClass.getAnnotation(ComponentScan.class);
        if (scan != null) {
            String[] packages = scan.value();
            for (String packageName : packages) {
                init(packageName);
            }
        } else {
            init(mainClass.getPackage().getName());
        }
    }

    private void init(String packageName) throws IOException, ClassNotFoundException, InstantiationException,
            IllegalAccessException, NoSuchMethodException, InvocationTargetException, IoCBeanNotFound, IoCCircularDepException {
        beanContainer.putBean(KevaIoC.class, this);
        implementationContainer.putImplementationClass(KevaIoC.class, KevaIoC.class);
        List<Class<?>> classes = ClassLoaderUtil.getClasses(packageName);
        scanImplementations(packageName);
        scanConfigurationClass(classes);
        scanComponentClasses(classes);
    }

    private void scanImplementations(String packageName) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> componentClasses = reflections.getTypesAnnotatedWith(Component.class);
        for (Class<?> implementationClass : componentClasses) {
            Class<?>[] interfaces = implementationClass.getInterfaces();
            if (interfaces.length == 0) {
                implementationContainer.putImplementationClass(implementationClass, implementationClass);
            } else {
                for (Class<?> interfaceClass : interfaces) {
                    implementationContainer.putImplementationClass(implementationClass, interfaceClass);
                }
            }
        }
        Set<Class<?>> configurationClasses = reflections.getTypesAnnotatedWith(Configuration.class);
        for (Class<?> configurationClass : configurationClasses) {
            Set<Method> methods = FinderUtil.findMethods(configurationClass, Bean.class);
            for (Method method : methods) {
                Class<?> returnType = method.getReturnType();
                implementationContainer.putImplementationClass(returnType, returnType);
            }
        }
    }

    private void scanConfigurationClass(List<Class<?>> classes) throws IoCCircularDepException, InvocationTargetException,
            IllegalAccessException, InstantiationException, NoSuchMethodException {
        Deque<Class<?>> configurationClassesQ = new ArrayDeque<>(5);
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Configuration.class)) {
                configurationClassesQ.add(clazz);
            }
        }
        while (!configurationClassesQ.isEmpty()) {
            Class<?> configurationClass = configurationClassesQ.removeFirst();
            try {
                Object instance = configurationClass.getConstructor().newInstance();
                circularDetector.detect(configurationClass);
                scanConfigurationBeans(configurationClass, instance);
            } catch (IoCBeanNotFound e) {
                configurationClassesQ.addLast(configurationClass);
            }
        }
    }

    private void scanComponentClasses(List<Class<?>> classes) throws IoCCircularDepException, InvocationTargetException,
            IllegalAccessException, InstantiationException, NoSuchMethodException, IoCBeanNotFound {
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Component.class)) {
                newInstanceWrapper(clazz);
            }
        }
    }

    private void scanConfigurationBeans(Class<?> clazz, Object classInstance) throws InvocationTargetException, IllegalAccessException,
            InstantiationException, NoSuchMethodException, IoCBeanNotFound, IoCCircularDepException {
        Set<Method> methods = FinderUtil.findMethods(clazz, Bean.class);
        Set<Field> fields = FinderUtil.findFields(clazz, Autowired.class);

        for (Field field : fields) {
            String qualifier = field.isAnnotationPresent(Qualifier.class) ? field.getAnnotation(Qualifier.class).value() : null;
            Object fieldInstance = _getBean(field.getType(), field.getName(), qualifier, false);
            field.set(classInstance, fieldInstance);
        }

        for (Method method : methods) {
            Class<?> beanType = method.getReturnType();
            Object beanInstance = method.invoke(classInstance);
            String name = method.getAnnotation(Bean.class).value() != null ?
                    method.getAnnotation(Bean.class).value() : beanType.getName();
            beanContainer.putBean(beanType, beanInstance, name);
        }
    }

    private Object newInstanceWrapper(Class<?> clazz) throws InvocationTargetException,
            IllegalAccessException, InstantiationException, NoSuchMethodException, IoCBeanNotFound, IoCCircularDepException {
        if (beanContainer.containsBean(clazz)) {
            return beanContainer.getBean(clazz);
        }

        circularDetector.detect(clazz);

        Object instance = newInstance(clazz);
        beanContainer.putBean(clazz, instance);
        fieldInject(clazz, instance);
        setterInject(clazz, instance);
        return instance;
    }

    private Object newInstance(Class<?> clazz) throws IllegalAccessException,
            InstantiationException, InvocationTargetException, NoSuchMethodException,
            IoCBeanNotFound, IoCCircularDepException {
        Constructor<?> annotatedConstructor = FinderUtil.findAnnotatedConstructor(clazz);
        Object instance;
        if (annotatedConstructor == null) {
            try {
                Constructor<?> defaultConstructor = clazz.getConstructor();
                defaultConstructor.setAccessible(true);
                instance = clazz.newInstance();
                return instance;
            } catch (NoSuchMethodException e) {
                throw new IoCException("There is no default constructor in class " + clazz.getName());
            }
        } else {
            Object[] parameters = new Object[annotatedConstructor.getParameterCount()];
            for (int i = 0; i < parameters.length; i++) {
                String qualifier = annotatedConstructor.getParameters()[i].isAnnotationPresent(Qualifier.class) ?
                        annotatedConstructor.getParameters()[i].getAnnotation(Qualifier.class).value() : null;
                Object depInstance = _getBean(annotatedConstructor.getParameterTypes()[i], annotatedConstructor.getParameterTypes()[i].getName(), qualifier, true);
                parameters[i] = depInstance;
            }
            instance = annotatedConstructor.newInstance(parameters);
        }
        return instance;
    }

    private void setterInject(Class<?> clazz, Object classInstance) throws IllegalAccessException,
            InvocationTargetException, NoSuchMethodException, InstantiationException, IoCBeanNotFound, IoCCircularDepException {
        Set<Method> methods = FinderUtil.findMethods(clazz, Autowired.class);
        for (Method method : methods) {
            Object[] parameters = new Object[method.getParameterCount()];
            for (int i = 0; i < parameters.length; i++) {
                String qualifier = method.getParameters()[i].isAnnotationPresent(Qualifier.class) ?
                        method.getParameters()[i].getAnnotation(Qualifier.class).value() : null;
                Object instance = _getBean(method.getParameterTypes()[i], method.getParameterTypes()[i].getName(), qualifier, true);
                parameters[i] = instance;
            }
            method.invoke(classInstance, parameters);
        }
    }

    private void fieldInject(Class<?> clazz, Object classInstance) throws IllegalAccessException,
            InstantiationException, InvocationTargetException, NoSuchMethodException, IoCBeanNotFound, IoCCircularDepException {
        Set<Field> fields = FinderUtil.findFields(clazz, Autowired.class);
        for (Field field : fields) {
            String qualifier = field.isAnnotationPresent(Qualifier.class) ? field.getAnnotation(Qualifier.class).value() : null;
            Object fieldInstance = _getBean(field.getType(), field.getName(), qualifier, true);
            field.set(classInstance, fieldInstance);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T _getBean(Class<T> interfaceClass) throws InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException, IoCBeanNotFound, IoCCircularDepException {
        return (T) _getBean(interfaceClass, null, null, false);
    }

    private <T> Object _getBean(Class<T> interfaceClass, String fieldName, String qualifier, boolean createIfNotFound) throws
            InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException,
            IoCBeanNotFound, IoCCircularDepException {
        Class<?> implementationClass = interfaceClass.isInterface() ?
                implementationContainer.getImplementationClass(interfaceClass, fieldName, qualifier) : interfaceClass;
        if (beanContainer.containsBean(implementationClass)) {
            if (qualifier != null) {
                return beanContainer.getBean(implementationClass, qualifier);
            }
            return beanContainer.getBean(implementationClass);
        }
        if (createIfNotFound) {
            synchronized (beanContainer) {
                return newInstanceWrapper(implementationClass);
            }
        } else {
             throw new IoCBeanNotFound("Cannot found bean for " + interfaceClass.getName());
        }
    }
}
