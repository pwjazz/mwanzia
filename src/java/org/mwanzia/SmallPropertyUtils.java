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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility for performing reflection on properties.
 * 
 * @author percy wegmann ( percy <at> karen and percy <dot> net )
 * 
 */
public class SmallPropertyUtils {
    // Cache for property information
    private static final Map<Class, Map<String, Property>> PROPERTY_CACHE = new ConcurrentHashMap<Class, Map<String, Property>>();

    /**
     * Retrieve a map of Properties by name for the given object.
     * 
     * @param object
     * @return
     */
    public static Map<String, Property> getProperties(Object object) {
        return getProperties(object.getClass());
    }

    /**
     * Retrieve a map of Properties by name for the given class.
     * 
     * @param clazz
     * @return
     */
    public static Map<String, Property> getProperties(Class clazz) {
        Map<String, Property> properties = PROPERTY_CACHE.get(clazz);
        if (properties == null) {
            properties = new HashMap<String, Property>();
            for (Method method : clazz.getMethods()) {
                if (method.getDeclaringClass() == Object.class) {
                    // Skip properties from base Object class
                    continue;
                }
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
                        property.setReadMethod(method);
                    } else {
                        property.setWriteMethod(method);
                    }
                }
            }
            PROPERTY_CACHE.put(clazz, properties);
        }
        return properties;
    }

    /**
     * Read the property of the given object identified by the given name.
     * 
     * @param <T>
     * @param object
     * @param name
     * @return
     */
    public static <T> T readProperty(Object object, String name) {
        if (object instanceof Map) {
            return (T) ((Map) object).get(name);
        } else {
            return getProperties(object).get(name).read(object);
        }
    }

    /**
     * Set the property on the given object of the given name with the given
     * value.
     * 
     * @param object
     * @param name
     * @param value
     */
    public static void writeProperty(Object object, String name, Object value) {
        if (object instanceof Map) {
            ((Map) object).put(name, value);
        } else {
            getProperties(object).get(name).write(object, value);
        }
    }

    /**
     * <p>
     * Coerce the given value to the specified type using common sense
     * conversions.
     * </p>
     * 
     * <p>
     * Numbers - converted on the basis if Number.intValue(),
     * Number.doubleValue(), etc.
     * </p>
     * 
     * <p>
     * Collections - convert between Lists, Sets and SortedSets as appropriate.
     * </p>
     * 
     * <p>
     * char[] <-> String
     * </p>
     * 
     * <p>
     * String -> UUID
     * </p>
     * 
     * <p>
     * String -> Enum
     * </p>
     * 
     * <p>
     * Anyting -> String -- using .toString()
     * </p>
     * 
     * @param <T>
     * @param value
     * @param targetType
     * @return
     */
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
        } else if (value instanceof char[] && String.class.isAssignableFrom(targetType)) {
            // Convert char[] to string
            value = String.valueOf((char[]) value);
        } else if (targetType.isEnum() && value instanceof String) {
            // Convert string to enum
            try {
                value = targetType.getMethod("valueOf", new Class[] { String.class }).invoke(null, value);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        } else if (UUID.class.isAssignableFrom(targetType) && value instanceof String) {
            // Convert string to UUID
            value = UUID.fromString((String) value);
        } else if (String.class.isAssignableFrom(targetType) && !(value instanceof String)) {
            // Convert anything to string
            value = value.toString();
        }
        return (T) value;
    }

    /**
     * Represents a named property on an object.
     * 
     * @author percy wegmann ( percy <at> karen and percy <dot> net )
     * 
     */
    public static class Property {
        private String name;
        private Method readMethod;
        private Method writeMethod;
        private Class propertyType;
        private boolean readable;
        private boolean writeable;
        private boolean jsonIncluded;
        private boolean jsonExcluded;

        public Property(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Method getReadMethod() {
            return readMethod;
        }

        public void setReadMethod(Method readMethod) {
            this.readMethod = readMethod;
            if (readMethod != null) {
                this.readable = true;
                if ("message".equals(name) && Throwable.class.isAssignableFrom(readMethod.getDeclaringClass())) {
                    this.jsonIncluded = true;
                } else {
                    this.jsonIncluded = readMethod.isAnnotationPresent(JsonInclude.class)
                            || readMethod.getDeclaringClass().isAnnotationPresent(JsonInclude.class);
                }
                this.propertyType = readMethod.getReturnType();
            }
            this.jsonExcluded = readMethod == null || readMethod.isAnnotationPresent(JsonExclude.class)
                    || readMethod.getDeclaringClass().isAnnotationPresent(JsonExclude.class);
        }

        public Method getWriteMethod() {
            return writeMethod;
        }

        public void setWriteMethod(Method writeMethod) {
            this.writeMethod = writeMethod;
            if (writeMethod != null) {
                this.writeable = true;
                if (propertyType == null) {
                    propertyType = writeMethod.getParameterTypes()[0];
                }
            }
        }

        public boolean isReadable() {
            return readable;
        }

        public boolean isWriteable() {
            return writeable;
        }

        public Class getPropertyType() {
            return propertyType;
        }

        public boolean isJsonIncluded() {
            return jsonIncluded;
        }

        public boolean isJsonExcluded() {
            return jsonExcluded;
        }

        public boolean isReadAnnotationPresent(Class<? extends Annotation> annotationClass) {
            return readMethod != null && readMethod.isAnnotationPresent(annotationClass);
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
