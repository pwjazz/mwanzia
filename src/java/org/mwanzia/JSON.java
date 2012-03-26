package org.mwanzia;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mwanzia.SmallPropertyUtils.Property;

/**
 * Utility for serializing and deserializing JSON.
 * 
 * @author percy
 * 
 */
public class JSON {
    public static final String MWANZIA_TYPE = "@class";
    public static final SimpleDateFormat ISO8601_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static final List<SerializationModifier> SERIALIZATION_MODIFIERS = new ArrayList<SerializationModifier>();
    private static final List<DeserializationModifier> DESERIALIZATION_MODIFIERS = new ArrayList<DeserializationModifier>();

    public static void addSerializationModifier(SerializationModifier modifier) {
        SERIALIZATION_MODIFIERS.add(modifier);
    }

    public static void addDeserializationModifier(DeserializationModifier modifier) {
        DESERIALIZATION_MODIFIERS.add(modifier);
    }

    /**
     * A hook for transparently replacing objects during serialization.
     * 
     * @author percy wegmann ( percy <at> karen and percy <dot> net )
     * 
     */
    public static interface SerializationModifier {
        public <T> T modify(T original, Property property, Map<String, Object> serializationContext) throws Exception;
    }

    public static interface DeserializationModifier {
        public <T> T modify(T deserialized, Object original) throws Exception;
    }

    /**
     * Convert the given json (assumed to be Map/List/Object/Primitive) to a
     * strongly typed object.
     * 
     * @param <T>
     * @param json
     * @param clazz
     * @return
     */
    public static <T> T fromJson(Object json, Class clazz) {
        if (json == null)
            return null;
        Object result = null;
        if (json instanceof Map) {
            Map<String, Object> in = (Map<String, Object>) json;
            try {
                if (clazz == null || Object.class == clazz) {
                    String className = (String) in.get("@class");
                    if (className != null) {
                        clazz = JSON.class.getClassLoader().loadClass(className);
                    }
                }
                if (clazz == null || Map.class.isAssignableFrom(clazz)) {
                    // Treat it like a map
                    Map<String, Object> map = new HashMap<String, Object>();
                    for (Map.Entry<String, Object> entry : in.entrySet()) {
                        map.put(entry.getKey(), fromJson(entry.getValue(), null));
                    }
                    result = map;
                } else if (Date.class.isAssignableFrom(clazz)) {
                    // This is a date - treat it specially an ISO8601 string
                    // date
                    String isoString = (String) in.get("isoString");
                    try {
                        result = ISO8601_DATE_FORMAT.parseObject(isoString);
                    } catch (ParseException pe) {
                        throw new RuntimeException(String.format("Unable to parse ISO8601 date %1$s: %2$s",
                                isoString,
                                pe.getMessage()), pe);
                    }
                } else {
                    // Otherwise treat as a normal class
                    result = clazz.newInstance();
                    Map<String, Property> properties = SmallPropertyUtils.getProperties(clazz);
                    for (Map.Entry<String, Object> entry : in.entrySet()) {
                        Property property = properties.get(entry.getKey());
                        if (property != null && property.isWriteable()) {
                            property.write(result, fromJson(entry.getValue(), property.getPropertyType()));
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(String.format("Unable to build class '%1$s' from json: %2$s",
                        clazz,
                        e.getMessage()), e);
            }
        } else if (json.getClass().isArray()) {
            Object[] in = (Object[]) json;
            if (clazz != null && Collection.class.isAssignableFrom(clazz)) {
                List<Object> out = new ArrayList<Object>();
                for (int i = 0; i < in.length; i++) {
                    out.add(fromJson(in[i], null));
                }
                result = out;
            } else {
                Class componentType = Object.class;
                if (clazz != null)
                    componentType = clazz.getComponentType();
                Object[] out = (Object[]) Array.newInstance(componentType, in.length);
                for (int i = 0; i < in.length; i++) {
                    out[i] = fromJson(in[i], null);
                }
                result = out;
            }
        } else if (json instanceof Collection) {
            Collection in = (Collection) json;
            if (clazz != null && Collection.class.isAssignableFrom(clazz)) {
                List<Object> out = new ArrayList<Object>();
                for (Object item : in) {
                    out.add(fromJson(item, null));
                }
                result = out;
            } else {
                Class componentType = Object.class;
                if (clazz != null)
                    componentType = clazz.getComponentType();
                Object[] out = (Object[]) Array.newInstance(componentType, in.size());
                int i = 0;
                for (Object item : in) {
                    out[i] = fromJson(item, componentType);
                    i += 1;
                }
                result = out;
            }
        } else {
            result = json;
        }

        // Plug in deserialization modifiers
        if (result != null) {
            for (DeserializationModifier modifier : DESERIALIZATION_MODIFIERS) {
                try {
                    result = modifier.modify(result, json);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }

        // Now adapt value to target type
        if (clazz != null) {
            result = SmallPropertyUtils.coerce(result, clazz);
        }
        return (T) result;
    }

    public static Object toJson(Object value, boolean whitelist) {
        return toJson(value, whitelist, null);
    }

    public static Object toJson(Object value, boolean whitelist, Property property) {
        return toJson(value, whitelist, property, new HashMap<String, Object>());
    }

    public static Object toJson(Object value, boolean whitelist, Property property,
            Map<String, Object> serializationContext) {
        if (value == null) {
            return null;
        }
        if (value instanceof Class) {
            return ((Class) value).getName();
        }
        for (SerializationModifier modifier : SERIALIZATION_MODIFIERS) {
            try {
                value = modifier.modify(value, property, serializationContext);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        Class<?> clazz = value.getClass();
        if (clazz.isArray()) {
            Object[] in = (Object[]) value;
            Object[] result = (Object[]) Array.newInstance(clazz.getComponentType(), in.length);
            for (int i = 0; i < in.length; i++) {
                result[i] = toJson(in[i], whitelist, property, serializationContext);
            }
            return result;
        } else if (value instanceof Collection) {
            Collection in = (Collection) value;
            List<Object> result = new ArrayList<Object>();
            for (Object item : in) {
                result.add(toJson(item, whitelist, property, serializationContext));
            }
            return result;
        } else if (Map.class.isAssignableFrom(clazz)) {
            // Convert the map
            Map<Object, Object> result = new HashMap<Object, Object>();
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) value).entrySet()) {
                result.put(entry.getKey(), toJson(entry.getValue(), whitelist, property, serializationContext));
            }
            return result;
        } else if (Modifier.isFinal(clazz.getModifiers()) || Number.class.isAssignableFrom(clazz)) {
            // Just return the value as is
            return value;
        } else {
            // Treat this as an object
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("@class", clazz);
            if (value instanceof Date) {
                // Dates get special treatment
                Date in = (Date) value;
                result.put("isoString", ISO8601_DATE_FORMAT.format(in));
            } else {
                Map<String, Property> properties = SmallPropertyUtils.getProperties(clazz);
                for (Map.Entry<String, Property> entry : properties.entrySet()) {
                    property = entry.getValue();

                    boolean includeProperty = whitelist ? property.isJsonIncluded() : !property.isJsonExcluded();
                    if (includeProperty) {
                        result.put(entry.getKey(),
                                toJson(property.read(value), whitelist, property, serializationContext));
                    }
                }
            }
            return result;
        }
    }
}