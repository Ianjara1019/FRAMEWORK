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
            context.setAttribute("sessionAuthKey", properties.getProperty("session.auth.key")); // ← NEW

            System.out.println("Routes chargées avec succès.");
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du chargement des routes", e);
        }
    }

    private Properties loadProperties(ServletContext context) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = context.getResourceAsStream("/WEB-INF/application.properties")) {
            if (input != null) {
                properties.load(input);
            }
        }
        catch (IOException e) {
            throw new IOException("Erreur lors du chargement du fichier de propriétés", e);
        }

        // upload
        String storagePath = properties.getProperty("upload.storage.path");
        if (storagePath == null || storagePath.isBlank()) {
            storagePath = Paths.get(System.getProperty("java.io.tmpdir"), "framework-uploads").toString();
        }
        properties.setProperty("upload.storage.path", storagePath.trim());

        // session auth
        String authKey = properties.getProperty("session.auth.key");
        if (authKey == null || authKey.isBlank()) {
            authKey = "authentifie"; // valeur par défaut
        }
        properties.setProperty("session.auth.key", authKey.trim());

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