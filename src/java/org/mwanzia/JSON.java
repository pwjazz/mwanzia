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
 * Utility for serializing and deserializing objects into JSON-like maps,
 * collections and so on. Mwanzia's JSON is just like regular JSON except that
 * typed objects automatically include a "@class" property that lets Mwanzia
 * know the exact class for each object.
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
        public <T> T modify(T original, Map<String, Object> serializationContext) throws Exception;
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
     *            - the json map/collection/etc
     * @param clazz
     *            - the target class for the object being converted (optional)
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
                    // If a specific class was not given, try to find out the
                    // class by examining the embedded @class property
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
                    // This is a date - grab the isoString property and parse it
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
                    // Process each of the properties in turn
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
            // We're dealing with an array, convert all the contained elements
            Object[] in = (Object[]) json;
            if (clazz != null && Collection.class.isAssignableFrom(clazz)) {
                // Treat the array as a List
                List<Object> out = new ArrayList<Object>();
                for (int i = 0; i < in.length; i++) {
                    out.add(fromJson(in[i], null));
                }
                result = out;
            } else {
                // Treat the array as an array
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
            // We're dealing with a list, convert all the contained elements
            Collection in = (Collection) json;
            if (clazz != null && Collection.class.isAssignableFrom(clazz)) {
                // Treat the list as a list
                List<Object> out = new ArrayList<Object>();
                for (Object item : in) {
                    out.add(fromJson(item, null));
                }
                result = out;
            } else {
                // Treat the list as an array
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

        // Allow plugins to modify results using DeserializationModifiers
        if (result != null) {
            for (DeserializationModifier modifier : DESERIALIZATION_MODIFIERS) {
                try {
                    result = modifier.modify(result, json);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }

        // Now coerce the result value to the target type
        if (clazz != null) {
            result = SmallPropertyUtils.coerce(result, clazz);
        }
        return (T) result;
    }

    /**
     * Convert the given value to JSON.
     * 
     * @param value
     * @param whitelist
     *            - whether or not to use property whitelisting
     * @return
     */
    public static Object toJson(Object value, boolean whitelist) {
        return toJson(value, whitelist, new HashMap<String, Object>());
    }

    /**
     * 
     * @param value
     * @param whitelist
     *            - whether or not to use property whitelisting
     * @param serializationContext
     *            - context that plugins can use to store state related to the
     *            current pass of serialization
     * @return
     */
    private static Object toJson(Object value, boolean whitelist, Map<String, Object> serializationContext) {
        if (value == null) {
            return null;
        }
        if (value instanceof Class) {
            return ((Class) value).getName();
        }
        for (SerializationModifier modifier : SERIALIZATION_MODIFIERS) {
            try {
                value = modifier.modify(value, serializationContext);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        Class<?> clazz = value.getClass();
        if (clazz.isArray()) {
            Object[] in = (Object[]) value;
            Object[] result = (Object[]) Array.newInstance(clazz.getComponentType(), in.length);
            for (int i = 0; i < in.length; i++) {
                result[i] = toJson(in[i], whitelist, serializationContext);
            }
            return result;
        } else if (value instanceof Collection) {
            Collection in = (Collection) value;
            List<Object> result = new ArrayList<Object>();
            for (Object item : in) {
                result.add(toJson(item, whitelist, serializationContext));
            }
            return result;
        } else if (Map.class.isAssignableFrom(clazz)) {
            // Convert the map
            Map<Object, Object> result = new HashMap<Object, Object>();
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) value).entrySet()) {
                result.put(entry.getKey(), toJson(entry.getValue(), whitelist, serializationContext));
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
                    Property property = entry.getValue();

                    boolean includeProperty = whitelist ? property.isJsonIncluded() : !property.isJsonExcluded();
                    if (includeProperty) {
                        result.put(entry.getKey(), toJson(property.read(value), whitelist, serializationContext));
                    }
                }
            }
            return result;
        }
    }
}