package framework.servlet;

import java.io.IOException;
import framework.routing.RouteDefinition;
import framework.routing.RouteRegistry;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

public class FrontControllerServlet extends HttpServlet {
    private RouteRegistry routeRegistry;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String packageToScan = config.getInitParameter("scanPackage");

        if (packageToScan == null || packageToScan.trim().isEmpty()) {
            throw new ServletException("Le paramètre 'scanPackage' est manquant dans le web.xml");
        }
        try {
            this.routeRegistry = new RouteRegistry(packageToScan);
        } catch (Exception e) {
            throw new ServletException("Erreur lors du chargement des routes", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String url = normalizeUrl(request);
        RouteDefinition handler = routeRegistry.find(url);

        if (handler == null) {
            writeUnknownRoute(response, url);
            return;
        }

        writeRouteInfo(response, handler);
    }

    private void writeUnknownRoute(HttpServletResponse response, String url) throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType("text/plain; charset=UTF-8");

        StringBuilder builder = new StringBuilder();
        builder.append("Aucune methode associee a l'URL : ").append(url).append("\n\n");
        builder.append("Routes disponibles :\n");
        for (String route : routeRegistry.describeRoutes()) {
            builder.append("- ").append(route).append('\n');
        }

        response.getWriter().print(builder.toString());
    }

    private void writeRouteInfo(HttpServletResponse response, RouteDefinition handler) throws IOException {
        response.setContentType("text/plain; charset=UTF-8");
        response.getWriter().println("URL : " + handler.getPath());
        response.getWriter().println("Controller : " + handler.getControllerClass().getName());
        response.getWriter().println("Methode : " + handler.getMethod().getName());
    }

    private String normalizeUrl(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String url = uri;

        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            url = uri.substring(contextPath.length());
        }

        return url == null || url.isBlank() ? "/" : url.trim();
    }
}
