package framework.listener;

import framework.routing.RouteRegistry;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class ApplicationListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            ServletContext context = sce.getServletContext();

            String packageToScan = context.getInitParameter("scanPackage");

            RouteRegistry registry = new RouteRegistry(packageToScan);

            // Stocker dans le contexte
            context.setAttribute("routeRegistry", registry);

            System.out.println("Routes chargées avec succès.");
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du chargement des routes", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("Application arrêtée.");
    }
}