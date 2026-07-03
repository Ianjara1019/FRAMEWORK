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
    private final Map<UrlMethod, UrlMappingMethod> routes = new LinkedHashMap<>();

    public RouteRegistry(String packageToScan) {
        List<Class<?>> controllers = AnnotationScanner.findAnnotatedClasses(packageToScan, Controller.class,
                ElementType.TYPE);
        registerRoutes(controllers);
    }

    public RouteDefinition find(String url) {
        UrlMappingMethod mapping = routes.get(new UrlMethod(normalizePath(url), "ANY"));
        if (mapping == null) {
            return null;
        }
        return mapping.findAny();
    }

    public RouteDefinition find(String url, String method) {
        UrlMappingMethod mapping = routes.get(new UrlMethod(normalizePath(url), method));
        return mapping == null ? null : mapping.find(method);
    }

    public List<String> describeRoutes() {
        List<String> descriptions = new ArrayList<>();
        for (UrlMappingMethod mapping : routes.values()) {
            for (RouteDefinition route : mapping.getRouteDefinitions()) {
                descriptions.add(describeRoute(route));
            }
        }
        return descriptions;
    }

    public String describeRoute(RouteDefinition route) {
        return route.getHttpMethod() + " " + route.getPath() + " -> " + route.getControllerClass().getName() + "#"
                + route.getMethod().getName();
    }

    private void registerRoutes(List<Class<?>> controllers) {
        for (Class<?> controllerClass : controllers) {
            for (Method method : controllerClass.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(UrlMapping.class)) {
                    continue;
                }

                UrlMapping mapping = method.getAnnotation(UrlMapping.class);
                String path = normalizePath(resolveUrl(mapping));
                String httpMethod = normalizeMethod(mapping.method());
                UrlMethod key = new UrlMethod(path, httpMethod);
                UrlMappingMethod mappingMethod = routes.computeIfAbsent(key, ignored -> new UrlMappingMethod(path));
                if (mappingMethod.containsMethod(httpMethod)) {
                    RouteDefinition existing = mappingMethod.find(httpMethod);
                    throw new IllegalStateException("Doublon de mapping URL : " + path + " ("
                            + existing.getControllerClass().getName() + "#" + existing.getMethod().getName() + " et "
                            + controllerClass.getName() + "#" + method.getName() + ")");
                }

                mappingMethod.add(new RouteDefinition(path, httpMethod, controllerClass, method));
            }
        }
    }

    private String resolveUrl(UrlMapping mapping) {
        if (mapping == null) {
            return "/";
        }

        if (mapping.url() != null && !mapping.url().isBlank()) {
            return mapping.url();
        }
        if (mapping.value() != null && !mapping.value().isBlank()) {
            return mapping.value();
        }
        return "/";
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

    private String normalizeMethod(String method) {
        if (method == null || method.isBlank()) {
            return "ANY";
        }
        return method.trim().toUpperCase();
    }
}
