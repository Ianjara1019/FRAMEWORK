package framework.session;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpSession;

public class MySession {
    private final HttpSession httpSession;
    private final Map<String, Object> values = new LinkedHashMap<>();
    private final Set<String> removedKeys = new LinkedHashSet<>();
    private boolean cleared;

    public MySession(HttpSession httpSession) {
        if (httpSession == null) {
            throw new IllegalArgumentException("HttpSession ne peut pas etre null.");
        }

        this.httpSession = httpSession;
        Enumeration<String> attributeNames = httpSession.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String name = attributeNames.nextElement();
            values.put(name, httpSession.getAttribute(name));
        }
    }

    public Object get(String name) {
        return values.get(name);
    }

    public <T> T get(String name, Class<T> type) {
        Object value = get(name);
        return value == null ? null : type.cast(value);
    }

    public boolean contains(String name) {
        return values.containsKey(name);
    }

    public void put(String name, Object value) {
        requireName(name);
        if (value == null) {
            remove(name);
            return;
        }

        values.put(name, value);
        removedKeys.remove(name);
    }

    public void remove(String name) {
        requireName(name);
        values.remove(name);
        removedKeys.add(name);
    }

    public void clear() {
        removedKeys.addAll(values.keySet());
        values.clear();
        cleared = true;
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(values);
    }

    public void synchronize() {
        if (cleared) {
            Enumeration<String> attributeNames = httpSession.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                removedKeys.add(attributeNames.nextElement());
            }
        }

        for (String name : removedKeys) {
            httpSession.removeAttribute(name);
        }
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            httpSession.setAttribute(entry.getKey(), entry.getValue());
        }
    }

    private void requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Le nom de session ne peut pas etre vide.");
        }
    }
}
