package dev.keva.ioc.core;

import dev.keva.ioc.exception.IoCException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ImplementationContainer {
    private final Map<Class<?>, Class<?>> implementationsMap = new HashMap<>(10);

    public void putImplementationClass(Class<?> implementationClass, Class<?> interfaceClass) {
        implementationsMap.put(implementationClass, interfaceClass);
    }

    public Class<?> getImplementationClass(Class<?> interfaceClass, final String fieldName, final String qualifier) {
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
