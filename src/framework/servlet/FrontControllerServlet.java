package framework.servlet;

import java.io.IOException;
import java.lang.reflect.Method;

import framework.routing.RouteDefinition;
import framework.routing.RouteRegistry;
import framework.view.ModelAndView;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

public class FrontControllerServlet extends HttpServlet {
    private RouteRegistry routeRegistry;
    private String viewPrefix;
    private String viewSuffix;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        this.viewPrefix = resolveInitParam(config, "prefix", "/WEB-INF/views/");
        this.viewSuffix = resolveInitParam(config, "suffix", ".jsp");

        this.routeRegistry = (RouteRegistry)
                config.getServletContext().getAttribute("routeRegistry");

        if (routeRegistry == null) {
            throw new ServletException("RouteRegistry introuvable.");
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

        invokeMethod(request, response, handler);
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

    private void invokeMethod(HttpServletRequest request, HttpServletResponse response, RouteDefinition handler)
            throws IOException, ServletException {
        try {
            Object controller = handler.getControllerClass().getDeclaredConstructor().newInstance();

            Method method = handler.getMethod();
            method.setAccessible(true);

            Object result = method.invoke(controller);

            if (result instanceof ModelAndView modelAndView) {
                renderView(request, response, modelAndView);
                return;
            }

            if (result instanceof String viewName) {
                renderView(request, response, new ModelAndView(viewName));
                return;
            }

            response.setContentType("text/plain; charset=UTF-8");
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

    private void renderView(HttpServletRequest request, HttpServletResponse response, ModelAndView modelAndView)
            throws ServletException, IOException {
        modelAndView.getModel().forEach(request::setAttribute);
        String viewPath = buildViewPath(modelAndView.getViewName());
        request.getRequestDispatcher(viewPath).forward(request, response);
    }

    private String buildViewPath(String viewName) {
        String normalizedViewName = viewName == null ? "" : viewName.trim();
        if (normalizedViewName.startsWith("/")) {
            normalizedViewName = normalizedViewName.substring(1);
        }
        return viewPrefix + normalizedViewName + viewSuffix;
    }

    private String resolveInitParam(ServletConfig config, String name, String defaultValue) {
        String value = config.getInitParameter(name);
        return value == null || value.isBlank() ? defaultValue : value.trim();
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
