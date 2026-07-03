package framework.servlet;

import java.io.IOException;
import java.lang.reflect.Method;
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
        String httpMethod = request.getMethod();
        RouteDefinition handler = routeRegistry.find(url, httpMethod);

        if (handler == null) {
            writeUnknownRoute(response, url, httpMethod);
            return;
        }

        invokeMethod(response, handler);
    }

    private void writeUnknownRoute(HttpServletResponse response, String url, String httpMethod) throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType("text/plain; charset=UTF-8");

        StringBuilder builder = new StringBuilder();
        builder.append("Aucune methode associee a l'URL : ").append(httpMethod).append(" ").append(url).append("\n\n");
        builder.append("Routes disponibles :\n");
        for (String route : routeRegistry.describeRoutes()) {
            builder.append("- ").append(route).append('\n');
        }

        response.getWriter().print(builder.toString());
    }

    private void invokeMethod(HttpServletResponse response, RouteDefinition handler) throws IOException {
        try {
            response.setContentType("text/plain; charset=UTF-8");

            Object controller = handler.getControllerClass().getDeclaredConstructor().newInstance();

            Method method = handler.getMethod();
            method.setAccessible(true);

            Object result = method.invoke(controller);

            response.getWriter().println("Methode invoquee avec succes!");
            response.getWriter().println("HTTP Method : " + handler.getHttpMethod());
            response.getWriter().println("URL : " + handler.getPath());
            response.getWriter().println("Controller : " + handler.getControllerClass().getName());
            response.getWriter().println("Methode : " + handler.getMethod().getName());
            if (result != null) {
                response.getWriter().println("Resultat : " + result.toString());
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("Erreur lors de l'invocation de la methode : " + e.getMessage());
            e.printStackTrace(response.getWriter());
        }
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
