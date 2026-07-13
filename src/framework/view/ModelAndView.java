package framework.view;

import java.util.LinkedHashMap;
import java.util.Map;

public class ModelAndView {
    private final String viewName;
    private final Map<String, Object> model = new LinkedHashMap<>();

    public ModelAndView(String viewName) {
        this.viewName = viewName;
    }

    public ModelAndView(String viewName, Map<String, Object> model) {
        this.viewName = viewName;
        if (model != null) {
            this.model.putAll(model);
        }
    }

    public String getViewName() {
        return viewName;
    }

    public Map<String, Object> getModel() {
        return model;
    }

    public ModelAndView addObject(String name, Object value) {
        model.put(name, value);
        return this;
    }
}