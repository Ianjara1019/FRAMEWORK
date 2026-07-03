package framework.routing;

import java.util.Objects;

public class UrlMethod {
    private final String url;
    private final String method;

    public UrlMethod(String url, String method) {
        this.url = normalizeUrl(url);
        this.method = normalizeMethod(method);
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof UrlMethod other)) {
            return false;
        }
        return Objects.equals(url, other.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    @Override
    public String toString() {
        return method + " " + url;
    }

    private String normalizeUrl(String value) {
        if (value == null || value.isBlank()) {
            return "/";
        }

        String normalized = value.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeMethod(String value) {
        if (value == null || value.isBlank()) {
            return "ANY";
        }
        return value.trim().toUpperCase();
    }
}
