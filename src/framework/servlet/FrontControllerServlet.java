package framework.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.ElementType;
import java.util.List;

import framework.annotation.Controller;
import framework.utils.AnnotationScanner;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

public class FrontControllerServlet extends HttpServlet {
    private List<Class<?>> controllers;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String packageToScan = config.getInitParameter("scanPackage");

        if (packageToScan == null || packageToScan.trim().isEmpty()) {
            throw new ServletException("Le paramètre 'scanPackage' est manquant dans le web.xml");
        }
        try {
            this.controllers = AnnotationScanner.findAnnotatedClasses(packageToScan, Controller.class,
                    ElementType.TYPE);
        } catch (Exception e) {
            throw new ServletException("Erreur lors du scan des annotations", e);
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
        String url = request.getRequestURI();
        response.getWriter().println("URL : " + url);
        PrintWriter out = response.getWriter();
        for (Class<?> c : this.controllers) {
            out.println(c.getName());
        }
    }
}
