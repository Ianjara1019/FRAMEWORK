package framework.listener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import framework.routing.RouteRegistry;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class ApplicationListener implements ServletContextListener {
    private static final String DEFAULT_SESSION_AUTH_KEY = "authentifie";
    private static final String DEFAULT_SESSION_ROLE_KEY = "role";
    private static final String PROPERTIES_FILE = "/WEB-INF/application.properties";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            ServletContext context = sce.getServletContext();

            String packageToScan = context.getInitParameter("scanPackage");
            if (packageToScan == null || packageToScan.isBlank()) {
                throw new IllegalStateException("Le paramètre 'scanPackage' est obligatoire dans web.xml");
            }

            RouteRegistry registry = new RouteRegistry(packageToScan);
            Properties properties = loadProperties(context);

            context.setAttribute("routeRegistry", registry);
            context.setAttribute("uploadStoragePath", properties.getProperty("upload.storage.path"));
            context.setAttribute("sessionAuthKey", properties.getProperty("session.auth.key"));
            context.setAttribute("sessionRoleKey", properties.getProperty("session.role.key"));

            System.out.println("Routes chargées avec succès.");
            System.out.println("  scanPackage       = " + packageToScan);
            System.out.println("  uploadStoragePath = " + properties.getProperty("upload.storage.path"));
            System.out.println("  sessionAuthKey    = " + properties.getProperty("session.auth.key"));
            System.out.println("  sessionRoleKey    = " + properties.getProperty("session.role.key"));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du chargement des routes", e);
        }
    }

    private Properties loadProperties(ServletContext context) throws IOException {
        Properties properties = new Properties();

        try (InputStream input = context.getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                System.err.println("ATTENTION: " + PROPERTIES_FILE
                        + " introuvable, utilisation des valeurs par défaut.");
            } else {
                properties.load(input);
            }
        }

        String storagePath = properties.getProperty("upload.storage.path");
        if (storagePath == null || storagePath.isBlank()) {
            storagePath = System.getProperty("java.io.tmpdir") + "/framework-uploads";
        }
        properties.setProperty("upload.storage.path", storagePath.trim());

        String authKey = properties.getProperty("session.auth.key");
        if (authKey == null || authKey.isBlank()) {
            authKey = DEFAULT_SESSION_AUTH_KEY;
        }
        properties.setProperty("session.auth.key", authKey.trim());

        String roleKey = properties.getProperty("session.role.key");
        if (roleKey == null || roleKey.isBlank()) {
            roleKey = DEFAULT_SESSION_ROLE_KEY;
        }
        properties.setProperty("session.role.key", roleKey.trim());

        return properties;
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("Application arrêtée.");
    }
}