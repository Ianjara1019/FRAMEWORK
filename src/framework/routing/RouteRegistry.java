package framework.routing;

import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import framework.annotation.Controller;
import framework.annotation.UrlMapping;
import framework.utils.AnnotationScanner;

public class RouteRegistry {
    private final Map<String, RouteDefinition> routes = new LinkedHashMap<>();

    public RouteRegistry(String packageToScan) {
        List<Class<?>> controllers = AnnotationScanner.findAnnotatedClasses(packageToScan, Controller.class,
                ElementType.TYPE);
        registerRoutes(controllers);
    }

    public RouteDefinition find(String url) {
        return routes.get(normalizePath(url));
    }

    public List<String> describeRoutes() {
        List<String> descriptions = new ArrayList<>();
        for (RouteDefinition route : routes.values()) {
            descriptions.add(describeRoute(route));
        }
        return descriptions;
    }

    public String describeRoute(RouteDefinition route) {
        return route.getPath() + " -> " + route.getControllerClass().getName() + "#"
                + route.getMethod().getName();
    }

    private void registerRoutes(List<Class<?>> controllers) {
        for (Class<?> controllerClass : controllers) {
            for (Method method : controllerClass.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(UrlMapping.class)) {
                    continue;
                }

                String path = normalizePath(method.getAnnotation(UrlMapping.class).value());
                if (routes.containsKey(path)) {
                    RouteDefinition existing = routes.get(path);
                    throw new IllegalStateException("Doublon de mapping URL : " + path + " ("
                            + existing.getControllerClass().getName() + "#" + existing.getMethod().getName() + " et "
                            + controllerClass.getName() + "#" + method.getName() + ")");
                }

                routes.put(path, new RouteDefinition(path, controllerClass, method));
            }
        }
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }

        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
