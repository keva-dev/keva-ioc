package dev.keva.ioc.utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class ClassLoaderUtil {
    public static List<Class<?>> getClasses(String packageName) throws IOException, URISyntaxException, ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URI pkg = Objects.requireNonNull(classLoader.getResource(path)).toURI();
        if (pkg.toString().startsWith("jar:")) {
            Path root;
            try {
                root = FileSystems.getFileSystem(pkg).getPath(path);
            } catch (FileSystemNotFoundException e) {
                root = FileSystems.newFileSystem(pkg, Collections.emptyMap()).getPath(path);
            }

            String extension = ".class";
            try (Stream<Path> allPaths = Files.walk(root)) {
                allPaths.filter(Files::isRegularFile).forEach(file -> {
                    try {
                        String filePath = file.toString().replace('/', '.');
                        String fileName = filePath.substring(filePath.indexOf(packageName), filePath.length() - extension.length());
                        classes.add(Class.forName(fileName));
                    } catch (ClassNotFoundException | StringIndexOutOfBoundsException ignored) {
                    }
                });
            }
        } else {
            Enumeration<URL> resources = classLoader.getResources(path);
            List<File> dirs = new ArrayList<>();
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                dirs.add(new File(resource.getFile()));
            }
            for (File directory : dirs) {
                classes.addAll(findClasses(directory, packageName));
            }
        }
        return classes;
    }

    private static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        assert files != null;
        for (File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                classes.add(Class.forName(className));
            }
        }
        return classes;
    }
}
