package framework.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class ParamBinder {

    private ParamBinder() {
    }

    public static Object[] resolveArguments(Method method, Map<String, String[]> parameterMap,
            HttpServletRequest request, HttpServletResponse response) throws ServletException {
        Parameter[] parameters = method.getParameters();
        Object[] arguments = new Object[parameters.length];

        for (int index = 0; index < parameters.length; index++) {
            Parameter parameter = parameters[index];
            Class<?> parameterType = parameter.getType();

            if (parameterType == HttpServletRequest.class) {
                arguments[index] = request;
            } else if (parameterType == HttpServletResponse.class) {
                arguments[index] = response;
            } else if (isSimpleType(parameterType)) {
                arguments[index] = convert(firstValue(parameterMap, parameter.getName()), parameterType);
            } else {
                arguments[index] = bindObject(parameterType, parameterMap);
            }
        }

        return arguments;
    }

    private static Object bindObject(Class<?> targetType, Map<String, String[]> parameterMap)
            throws ServletException {
        try {
            Object object = targetType.getDeclaredConstructor().newInstance();

            for (Field field : targetType.getDeclaredFields()) {
                String rawValue = firstValue(parameterMap, field.getName());
                if (rawValue == null) {
                    continue;
                }

                field.setAccessible(true);
                field.set(object, convert(rawValue, field.getType()));
            }

            return object;
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            throw new ServletException("Impossible de binder l'objet " + targetType.getName(), e);
        }
    }

    private static String firstValue(Map<String, String[]> parameterMap, String name) {
        String[] values = parameterMap.get(name);
        return values == null || values.length == 0 ? null : values[0];
    }

    private static boolean isSimpleType(Class<?> type) {
        return type == String.class
                || type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == double.class || type == Double.class
                || type == float.class || type == Float.class
                || type == boolean.class || type == Boolean.class
                || type == short.class || type == Short.class
                || type == byte.class || type == Byte.class
                || type == char.class || type == Character.class
                || type == BigDecimal.class
                || type == LocalDate.class
                || type == LocalDateTime.class;
    }

    private static Object convert(String rawValue, Class<?> targetType) {
        if (rawValue == null) {
            if (!targetType.isPrimitive()) {
                return null;
            }
            if (targetType == boolean.class) {
                return false;
            }
            if (targetType == char.class) {
                return '\0';
            }
            return 0;
        }

        if (targetType == String.class) return rawValue;
        if (targetType == int.class || targetType == Integer.class) return Integer.valueOf(rawValue);
        if (targetType == long.class || targetType == Long.class) return Long.valueOf(rawValue);
        if (targetType == double.class || targetType == Double.class) return Double.valueOf(rawValue);
        if (targetType == float.class || targetType == Float.class) return Float.valueOf(rawValue);
        if (targetType == boolean.class || targetType == Boolean.class) return Boolean.valueOf(rawValue);
        if (targetType == short.class || targetType == Short.class) return Short.valueOf(rawValue);
        if (targetType == byte.class || targetType == Byte.class) return Byte.valueOf(rawValue);
        if (targetType == char.class || targetType == Character.class) return rawValue.charAt(0);
        if (targetType == BigDecimal.class) return new BigDecimal(rawValue);
        if (targetType == LocalDate.class) return LocalDate.parse(rawValue, DateTimeFormatter.ISO_LOCAL_DATE);
        if (targetType == LocalDateTime.class) {
            return LocalDateTime.parse(rawValue, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        throw new IllegalArgumentException("Type de parametre non supporte : " + targetType.getName());
    }
}