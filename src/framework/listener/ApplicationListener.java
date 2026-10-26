package framework.listener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import framework.routing.RouteRegistry;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

public class ApplicationListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            ServletContext context = sce.getServletContext();

            String packageToScan = context.getInitParameter("scanPackage");

            RouteRegistry registry = new RouteRegistry(packageToScan);
            Properties properties = loadProperties(context);

            // Stocker dans le contexte
            context.setAttribute("routeRegistry", registry);
            context.setAttribute("uploadStoragePath", properties.getProperty("upload.storage.path"));

            System.out.println("Routes chargées avec succès.");
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du chargement des routes", e);
        }
    }

    private Properties loadProperties(ServletContext context) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = context.getResourceAsStream("/WEB-INF/application.properties")) {
            if (input == null) {
                throw new IOException("Fichier /WEB-INF/application.properties introuvable");
            }
            properties.load(input);
        }

        String storagePath = properties.getProperty("upload.storage.path");
        if (storagePath == null || storagePath.isBlank()) {
            throw new IOException("La propriété upload.storage.path est obligatoire");
        }
        properties.setProperty("upload.storage.path", storagePath.trim());
        return properties;
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