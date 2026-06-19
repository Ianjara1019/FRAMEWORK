package framework.utils;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarFile;

public class AnnotationScanner {

    public static List<Class<?>> findAnnotatedClasses(String pkg, Class<? extends Annotation> ann, ElementType level) {
        List<Class<?>> result = new ArrayList<>();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL res = cl.getResource(pkg.replace('.', '/'));
        if (res == null)
            return result;
        try {
            if ("file".equals(res.getProtocol())) {
                Path dir = Paths.get(res.toURI());
                Files.walk(dir)
                        .filter(f -> f.toString().endsWith(".class"))
                        .map(f -> pkg + '.'
                                + dir.relativize(f).toString().replace(java.io.File.separatorChar, '.')
                                        .replaceAll("\\.class$", ""))
                        .forEach(name -> addIfAnnotated(name, ann, level, result, cl));
            } else if ("jar".equals(res.getProtocol())) {
                try (JarFile jar = ((JarURLConnection) res.openConnection()).getJarFile()) {
                    Collections.list(jar.entries()).stream()
                            .map(e -> e.getName())
                            .filter(n -> n.startsWith(pkg.replace('.', '/') + "/") && n.endsWith(".class"))
                            .map(n -> n.replace('/', '.').replaceAll("\\.class$", ""))
                            .forEach(name -> addIfAnnotated(name, ann, level, result, cl));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static void addIfAnnotated(String className, Class<? extends Annotation> ann, ElementType level,
            List<Class<?>> result, ClassLoader cl) {
        try {
            Class<?> cls = Class.forName(className, true, cl);
            boolean match = switch (level) {
                case TYPE -> cls.isAnnotationPresent(ann);
                case METHOD -> Arrays.stream(cls.getDeclaredMethods()).anyMatch(m -> m.isAnnotationPresent(ann));
                case FIELD -> Arrays.stream(cls.getDeclaredFields()).anyMatch(f -> f.isAnnotationPresent(ann));
                default -> false;
            };
            if (match)
                result.add(cls);
        } catch (ClassNotFoundException ignored) {
        }
    }
}