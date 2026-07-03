package framework.init;

import framework.routing.RouteRegistry;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;

public class Initializer {

    private final RouteRegistry routeRegistry;

    public Initializer(ServletConfig config) throws ServletException {
        String packageToScan = config.getInitParameter("scanPackage");

        if (packageToScan == null || packageToScan.trim().isEmpty()) {
            throw new ServletException(
                "Le paramètre 'scanPackage' est manquant dans le web.xml"
            );
        }

        try {
            this.routeRegistry = new RouteRegistry(packageToScan);
        } catch (Exception e) {
            throw new ServletException(
                "Erreur lors du chargement des routes", e
            );
        }
    }

    public RouteRegistry getRouteRegistry() {
        return routeRegistry;
    }
}