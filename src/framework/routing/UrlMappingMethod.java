package framework.routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class UrlMappingMethod {
    private final String path;
    private RouteDefinition defaultRoute;
    private final Map<String, RouteDefinition> methods = new LinkedHashMap<>();

    public UrlMappingMethod(String path) {
        this.path = normalizePath(path);
    }

    public String getPath() {
        return path;
    }

    public void add(RouteDefinition routeDefinition) {
        String httpMethod = normalizeMethod(routeDefinition.getHttpMethod());
        if ("ANY".equals(httpMethod)) {
            defaultRoute = routeDefinition;
            return;
        }
        methods.put(httpMethod, routeDefinition);
    }

    public RouteDefinition find(String method) {
        String normalizedMethod = normalizeMethod(method);
        RouteDefinition route = methods.get(normalizedMethod);
        if (route != null) {
            return route;
        }
        return defaultRoute;
    }

    public RouteDefinition findAny() {
        if (defaultRoute != null) {
            return defaultRoute;
        }
        return methods.values().stream().findFirst().orElse(null);
    }

    public boolean containsMethod(String method) {
        String normalizedMethod = normalizeMethod(method);
        if ("ANY".equals(normalizedMethod)) {
            return defaultRoute != null;
        }
        return methods.containsKey(normalizedMethod);
    }

    public Collection<RouteDefinition> getRouteDefinitions() {
        ArrayList<RouteDefinition> definitions = new ArrayList<>();
        if (defaultRoute != null) {
            definitions.add(defaultRoute);
        }
        definitions.addAll(methods.values());
        return Collections.unmodifiableCollection(definitions);
    }

    private String normalizePath(String value) {
        if (value == null || value.isBlank()) {
            return "/";
        }

        String normalized = value.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeMethod(String value) {
        if (value == null || value.isBlank()) {
            return "ANY";
        }
        return value.trim().toUpperCase();
    }
}
