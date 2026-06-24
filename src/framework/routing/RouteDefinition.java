package framework.routing;

import java.lang.reflect.Method;

public class RouteDefinition {
    private final String path;
    private final Class<?> controllerClass;
    private final Method method;

    public RouteDefinition(String path, Class<?> controllerClass, Method method) {
        this.path = path;
        this.controllerClass = controllerClass;
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public Class<?> getControllerClass() {
        return controllerClass;
    }

    public Method getMethod() {
        return method;
    }
}
