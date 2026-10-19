package framework.listener;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

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
            configureUploadStorage(context);

            System.out.println("Routes chargées avec succès.");
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du chargement des routes", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("Application arrêtée.");
    }

    private void configureUploadStorage(ServletContext context) throws IOException {
        Properties properties = new Properties();

        try (InputStream input = context.getResourceAsStream("/WEB-INF/application.properties")) {
            if (input != null) {
                properties.load(input);
            }
        }

        String configuredPath = properties.getProperty("upload.storage.path");
        String storagePath = configuredPath == null || configuredPath.isBlank()
                ? Paths.get(System.getProperty("java.io.tmpdir"), "framework-uploads").toString()
                : configuredPath.trim();

        Path uploadDirectory = Paths.get(storagePath).toAbsolutePath().normalize();
        Files.createDirectories(uploadDirectory);
        context.setAttribute("uploadStoragePath", uploadDirectory.toString());
    }
}