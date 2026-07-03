package framework.routing;

import java.lang.reflect.Method;

public class RouteDefinition {
    private final String path;
    private final String httpMethod;
    private final Class<?> controllerClass;
    private final Method method;

    public RouteDefinition(String path, Class<?> controllerClass, Method method) {
        this(path, "ANY", controllerClass, method);
    }

    public RouteDefinition(String path, String httpMethod, Class<?> controllerClass, Method method) {
        this.path = path;
        this.httpMethod = httpMethod;
        this.controllerClass = controllerClass;
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public Class<?> getControllerClass() {
        return controllerClass;
    }

    public Method getMethod() {
        return method;
    }
}
