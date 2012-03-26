package org.mwanzia;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

public class SmallPropertyUtils {
    public static Map<String, Property> getProperties(Object object) {
        return getProperties(object.getClass());
    }

    public static Map<String, Property> getProperties(Class clazz) {
        Map<String, Property> properties = new HashMap<String, Property>();
        for (Method method : clazz.getMethods()) {
            String methodName = method.getName();
            boolean isReadMethod = (methodName.startsWith("get") || ((Boolean.TYPE.isAssignableFrom(method
                    .getReturnType()) || Boolean.class.isAssignableFrom(method.getReturnType())) && methodName
                    .startsWith("is")))
                    && method.getParameterTypes().length == 0;
            boolean isWriteMethod = methodName.startsWith("set") && method.getParameterTypes().length == 1;
            if (isReadMethod || isWriteMethod) {
                String propertyName = null;
                if (methodName.startsWith("is")) {
                    propertyName = methodName.substring(2, 3).toLowerCase() + methodName.substring(3);
                } else {
                    propertyName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
                }
                Property property = properties.get(propertyName);
                if (property == null) {
                    property = new Property(propertyName);
                    properties.put(propertyName, property);
                }
                if (isReadMethod) {
                    property.readMethod = method;
                } else {
                    property.writeMethod = method;
                }
            }
        }
        return properties;
    }

    public static <T> T readProperty(Object object, String name) {
        if (object instanceof Map) {
            return (T) ((Map) object).get(name);
        } else {
            return getProperties(object).get(name).read(object);
        }
    }

    public static void writeProperty(Object object, String name, Object value) {
        if (object instanceof Map) {
            ((Map) object).put(name, value);
        } else {
            getProperties(object).get(name).write(object, value);
        }
    }

    public static <T> T coerce(Object value, Class<T> targetType) {
        if (value == null)
            return null;
        if (value.getClass() != targetType && Number.class.isAssignableFrom(targetType)) {
            // Handle numeric conversions
            Number number = (Number) value;
            if (BigDecimal.class == targetType) {
                value = BigDecimal.valueOf(number.doubleValue());
            } else if (BigInteger.class == targetType) {
                value = BigInteger.valueOf(number.longValue());
            } else if (Byte.class == targetType) {
                value = number.byteValue();
            } else if (Double.class == targetType) {
                value = number.doubleValue();
            } else if (Float.class == targetType) {
                value = number.floatValue();
            } else if (Integer.class == targetType) {
                value = number.intValue();
            } else if (Long.class == targetType) {
                value = number.longValue();
            } else if (Short.class == targetType) {
                value = number.shortValue();
            }
        } else if (value instanceof Collection) {
            // Handle collection conversions
            if (SortedSet.class.isAssignableFrom(targetType)) {
                value = new TreeSet<Object>((Collection<?>) value);
            } else if (Set.class.isAssignableFrom(targetType)) {
                value = new HashSet<Object>((Collection<?>) value);
            }
        } else if (value instanceof String && targetType.isArray()
                && Character.TYPE.isAssignableFrom(targetType.getComponentType())) {
            // Convert string to char[]
            value = ((String) value).toCharArray();
        } else if (targetType.isEnum() && value instanceof String) {
            try {
                value = targetType.getMethod("valueOf", new Class[] { String.class }).invoke(null, value);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        } else if (UUID.class.isAssignableFrom(targetType) && value instanceof String) {
            value = UUID.fromString((String) value);
        } else if (String.class.isAssignableFrom(targetType) && !(value instanceof String)) {
            value = value.toString();
        }
        return (T) value;
    }

    public static class Property {
        public String name;
        public Method readMethod;
        public Method writeMethod;

        public Property(String name) {
            this.name = name;
        }

        public boolean isReadable() {
            return readMethod != null;
        }

        public boolean isWriteable() {
            return writeMethod != null;
        }

        public boolean isJsonIncluded() {
            if ("message".equals(name) && Throwable.class.isAssignableFrom(readMethod.getDeclaringClass())) {
                return true;
            }
            return isReadable()
                    && (readMethod.isAnnotationPresent(JsonInclude.class) || readMethod.getDeclaringClass()
                            .isAnnotationPresent(JsonInclude.class));
        }

        public boolean isJsonExcluded() {
            return !isReadable()
                    || (readMethod.isAnnotationPresent(JsonExclude.class) || readMethod.getDeclaringClass()
                            .isAnnotationPresent(JsonExclude.class));
        }

        public boolean isReadAnnotationPresent(Class<? extends Annotation> annotationClass) {
            return readMethod != null && readMethod.isAnnotationPresent(annotationClass);
        }

        public Class getPropertyType() {
            if (isReadable()) {
                return readMethod.getReturnType();
            } else {
                return writeMethod.getParameterTypes()[0];
            }
        }

        public <T> T read(Object source) {
            try {
                return (T) readMethod.invoke(source, null);
            } catch (IllegalAccessException iae) {
                throw new RuntimeException(iae.getMessage(), iae);
            } catch (InvocationTargetException ite) {
                throw new RuntimeException(ite.getCause().getMessage(), ite.getCause());
            }
        }

        public void write(Object source, Object value) {
            try {
                Class propertyType = writeMethod.getParameterTypes()[0];
                writeMethod.invoke(source, new Object[] { coerce(value, propertyType) });
            } catch (InvocationTargetException ite) {
                throw new RuntimeException(ite.getCause().getMessage(), ite.getCause());
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }
}
