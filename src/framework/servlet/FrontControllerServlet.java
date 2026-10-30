package framework.servlet;

import java.io.IOException;
import java.lang.reflect.Method;

import framework.annotation.ApiRest;
import framework.annotation.Authentified;
import framework.annotation.Role;
import framework.routing.RouteDefinition;
import framework.routing.RouteRegistry;
import framework.session.MySession;
import framework.utils.ParamBinder;
import framework.view.ModelAndView;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tools.jackson.databind.ObjectMapper;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class FrontControllerServlet extends HttpServlet {

    private RouteRegistry routeRegistry;
    private String viewPrefix;
    private String viewSuffix;
    private String sessionAuthKey;
    private String sessionRoleKey;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<Class<?>, Object> controllers = new ConcurrentHashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        this.viewPrefix = resolveInitParam(config, "prefix", "/WEB-INF/views/");
        this.viewSuffix = resolveInitParam(config, "suffix", ".jsp");

        this.routeRegistry = (RouteRegistry) config.getServletContext().getAttribute("routeRegistry");
        if (routeRegistry == null) {
            throw new ServletException("RouteRegistry introuvable.");
        }

        Object authKey = config.getServletContext().getAttribute("sessionAuthKey");
        this.sessionAuthKey = (authKey == null || authKey.toString().isBlank())
                ? "authentifie"
                : authKey.toString();

        Object roleKey = config.getServletContext().getAttribute("sessionRoleKey");
        this.sessionRoleKey = (roleKey == null || roleKey.toString().isBlank())
                ? "role"
                : roleKey.toString();
        
        for (Class<?> controllerClass : routeRegistry.getControllerClasses()) {
            try {
                Object instance = controllerClass.getDeclaredConstructor().newInstance();
                controllers.put(controllerClass, instance);
            } catch (ReflectiveOperationException e) {
                throw new ServletException(
                    "Impossible d'instancier le controller " + controllerClass.getName()
                    + " (constructeur sans argument requis)", e);
            }
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
        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding("UTF-8");
        }

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
            Method method = handler.getMethod();
            Class<?> controllerClass = handler.getControllerClass();

            if (requiresAuthentication(method, controllerClass) && !isAuthenticated(request)) {
                handleUnauthenticated(request, response, method);
                return;
            }

            if (!isAuthorized(method, controllerClass, request)) {
                handleForbidden(response, method);
                return;
            }

            // Object controller = controllerClass.getDeclaredConstructor().newInstance();
            Object controller = controllers.get(handler.getControllerClass());
            if (controller == null) {
                throw new ServletException(
                    "Controller non initialisé : " + handler.getControllerClass().getName());
            }
            method.setAccessible(true);
            Object[] arguments = ParamBinder.resolveArguments(method, request.getParameterMap(), request, response);
            Object result;

            try {
                result = method.invoke(controller, arguments);
            } finally {
                synchronizeSessions(arguments);
            }

            if (method.isAnnotationPresent(ApiRest.class)) {
                writeJsonResponse(response, result);
                return;
            }

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

    private boolean requiresAuthentication(Method method, Class<?> controllerClass) {
        return method.isAnnotationPresent(Authentified.class)
                || controllerClass.isAnnotationPresent(Authentified.class);
    }

    private boolean isAuthenticated(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        return session.getAttribute(sessionAuthKey) != null;
    }

    private void handleUnauthenticated(HttpServletRequest request, HttpServletResponse response, Method method)
            throws IOException {
        if (method.isAnnotationPresent(ApiRest.class)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().write("{\"error\":\"unauthorized\",\"reason\":\"missing session key\"}");
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("text/plain; charset=UTF-8");
        response.getWriter().println("Authentification requise.");
        response.getWriter().println("Clé de session attendue : " + sessionAuthKey);
    }

    private boolean isAuthorized(Method method, Class<?> controllerClass, HttpServletRequest request) {
        String[] requiredRoles = resolveRequiredRoles(method, controllerClass);
        if (requiredRoles.length == 0) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }

        Object rawRole = session.getAttribute(sessionRoleKey);
        if (rawRole == null) {
            return false;
        }

        String userRole = rawRole.toString();
        for (String required : requiredRoles) {
            if (required.equalsIgnoreCase(userRole)) {
                return true;
            }
        }
        return false;
    }

    private String[] resolveRequiredRoles(Method method, Class<?> controllerClass) {
        Role methodRole = method.getAnnotation(Role.class);
        if (methodRole != null) {
            return methodRole.value();
        }
        Role classRole = controllerClass.getAnnotation(Role.class);
        if (classRole != null) {
            return classRole.value();
        }
        return new String[0];
    }

    private void handleForbidden(HttpServletResponse response, Method method) throws IOException {
        if (method.isAnnotationPresent(ApiRest.class)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().write("{\"error\":\"forbidden\",\"reason\":\"insufficient role\"}");
            return;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("text/plain; charset=UTF-8");
        response.getWriter().println("Accès refusé : rôle insuffisant.");
        response.getWriter().println("Clé de rôle attendue : " + sessionRoleKey);
    }

    private void synchronizeSessions(Object[] arguments) {
        for (Object argument : arguments) {
            if (argument instanceof MySession mySession) {
                try {
                    mySession.synchronize();
                } catch (IllegalStateException ignored) {
                }
            }
        }
    }

    private void writeJsonResponse(HttpServletResponse response, Object result) throws IOException {
        response.setContentType("application/json; charset=UTF-8");

        if (result == null) {
            response.getWriter().write("null");
            return;
        }
        if (result instanceof String jsonString) {
            response.getWriter().write(jsonString);
            return;
        }
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    private void renderView(HttpServletRequest request, HttpServletResponse response, ModelAndView modelAndView)
            throws ServletException, IOException {
        String viewName = modelAndView.getViewName();

        if (viewName != null && viewName.startsWith("redirect:")) {
            String target = viewName.substring("redirect:".length());
            response.sendRedirect(request.getContextPath() + target);
            return;
        }

        modelAndView.getModel().forEach(request::setAttribute);
        String viewPath = buildViewPath(viewName);
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