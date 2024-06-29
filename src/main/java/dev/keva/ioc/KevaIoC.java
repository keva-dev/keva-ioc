package dev.keva.ioc;

import dev.keva.ioc.annotation.*;
import dev.keva.ioc.core.BeanContainer;
import dev.keva.ioc.core.CircularDependencyDetector;
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
import java.net.URISyntaxException;
import java.util.*;

/**
 * KevaIoC - A lightweight dependency injection and inversion of control container.
 * The class provides mechanisms for scanning, registering, and resolving dependencies among components
 * marked with specific annotations within a Java application. It supports component lifecycle management
 * through dependency injection patterns such as constructor injection, setter injection, and field injection.
 *
 * Key Features:
 * - Component scanning based on package names and annotations.
 * - Registration of components, either predefined or discovered via component scanning.
 * - Resolution of components ensuring dependencies are satisfied and handling lifecycle appropriately.
 * - Detection and handling of circular dependencies among components.
 * - Exception handling to manage errors related to bean instantiation, circular dependencies, and missing beans.
 *
 * Usage:
 * - To use this container, the user must call the static method `initBeans` with the main class and any predefined beans.
 * - Beans can be fetched using the `getBean` method by providing the class type.
 *
 * Method Execution Flow:
 * 1. `initBeans`: Initializes the IoC container with an optional list of predefined beans. This method handles all
 *    initial setup including scanning components, registering beans, and handling any exceptions that occur during initialization.
 * 2. `initWrapper`: Called by `initBeans`, this method performs the detailed initialization sequence including:
 *    a. Registering predefined beans if provided.
 *    b. Component scanning using annotations to discover and register additional beans.
 * 3. `init`: Helper method called by `initWrapper` to perform scanning within a specific package, it handles:
 *    a. Registering the container itself as a bean.
 *    b. Scanning for implementations, configurations, and components within the package.
 * 4. `scanImplementations`, `scanConfigurationClass`, `scanComponentClasses`: These methods are used to find and register beans based on different criteria like annotated classes or methods within classes.
 * 5. `newInstanceWrapper`: Handles instantiation of a class, managing circular dependencies and injecting necessary dependencies.
 * 6. `_getBean`: Overloaded methods that resolve and return beans by type, handling instantiation if not already present in the container.
 * 7. `fieldInject`, `setterInject`: Methods for performing dependency injection into annotated fields and setter methods respectively.
 *
 * Exceptions:
 * - The class handles various exceptions that may arise during the operation of the IoC container such as `IoCException`,
 *   `IoCBeanNotFound`, and `IoCCircularDepException`. These are typically re-thrown as `IoCException` with appropriate error messages.
 *
 * This class is part of the `dev.keva.ioc` package and depends on various other classes within the same package and third-party
 * libraries such as `org.reflections.Reflections` for component scanning based on annotations.
 */
public class KevaIoC {
    private final BeanContainer beanContainer = new BeanContainer();
    private final ImplementationContainer implementationContainer = new ImplementationContainer();
    private final CircularDependencyDetector circularDependencyDetector = new CircularDependencyDetector();

    private KevaIoC() {
    }

    public static KevaIoC initBeans(Class<?> mainClass, Object... predefinedBeans) {
        try {
            KevaIoC instance = new KevaIoC();
            instance.initWrapper(mainClass, predefinedBeans);
            return instance;
        } catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException |
                InvocationTargetException | NoSuchMethodException | IoCBeanNotFound | IoCCircularDepException | URISyntaxException e) {
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

    private void initWrapper(Class<?> mainClass, Object[] predefinedBeans) throws IOException, ClassNotFoundException,
            InstantiationException, IllegalAccessException, NoSuchMethodException,
            InvocationTargetException, IoCBeanNotFound, IoCCircularDepException, URISyntaxException {
        if (predefinedBeans != null && predefinedBeans.length > 0) {
           for (Object bean : predefinedBeans) {
               Class<?>[] interfaces = bean.getClass().getInterfaces();
               if (interfaces.length == 0) {
                   implementationContainer.putImplementationClass(bean.getClass(), bean.getClass());
               } else {
                   for (Class<?> interfaceClass : interfaces) {
                       implementationContainer.putImplementationClass(bean.getClass(), interfaceClass);
                   }
               }
               beanContainer.putBean(bean.getClass(), bean);
           }
        }

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

    private void init(String packageName) throws IOException, InstantiationException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException, IoCBeanNotFound, IoCCircularDepException, URISyntaxException,
            ClassNotFoundException {
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
        circularDependencyDetector.startInstantiation(clazz);

        try {
            if (beanContainer.containsBean(clazz)) {
                return beanContainer.getBean(clazz);
            }

            Object instance = newInstance(clazz);
            beanContainer.putBean(clazz, instance);
            fieldInject(clazz, instance);
            setterInject(clazz, instance);
            return instance;
        } finally {
            circularDependencyDetector.finishInstantiation(clazz);
        }
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
                Object depInstance = _getBean(annotatedConstructor.getParameterTypes()[i],
                        annotatedConstructor.getParameterTypes()[i].getName(), qualifier, true);
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
                Object instance = _getBean(method.getParameterTypes()[i],
                        method.getParameterTypes()[i].getName(), qualifier, true);
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
