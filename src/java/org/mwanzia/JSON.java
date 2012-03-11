package org.mwanzia;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.annotate.JsonTypeInfo.As;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectMapper.DefaultTypeResolverBuilder;
import org.codehaus.jackson.map.ObjectMapper.DefaultTyping;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.SerializationConfig.Feature;
import org.codehaus.jackson.map.SerializerFactory;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;
import org.codehaus.jackson.map.jsontype.NamedType;
import org.codehaus.jackson.map.jsontype.TypeIdResolver;
import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;
import org.codehaus.jackson.map.jsontype.impl.ClassNameIdResolver;
import org.codehaus.jackson.map.ser.AnyGetterWriter;
import org.codehaus.jackson.map.ser.BeanPropertyWriter;
import org.codehaus.jackson.map.ser.BeanSerializer;
import org.codehaus.jackson.map.ser.BeanSerializerFactory;
import org.codehaus.jackson.map.ser.ContainerSerializers.CollectionSerializer;
import org.codehaus.jackson.map.ser.MapSerializer;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;

/**
 * Utility for serializing and deserializing JSON, built on Jackson.
 * 
 * @author percy
 * 
 */
public class JSON {
    public static final String MWANZIA_TYPE = "@class";
    private static final SerializerFactory SERIALIZER_FACTORY = new MwanziaSerializerFactory();
    private static final List<SerializationModifier> SERIALIZATION_MODIFIERS = new ArrayList<SerializationModifier>();

    public static void addSerializationModifier(SerializationModifier modifier) {
        SERIALIZATION_MODIFIERS.add(modifier);
    }

    public static String serialize(Object object, boolean whitelistProperties) {
        try {
            return buildObjectMapper(whitelistProperties).writeValueAsString(object);
        } catch (Exception e) {
            throw new MwanziaException(String.format("Unable to serialize object %1$s: %2$s", object, e.getMessage()), e);
        }
    }

    public static <T> T deserialize(String json, Class<T> clazz) {
        try {
            return buildObjectMapperForDeserialization().readValue(json, clazz);
        } catch (Exception e) {
            throw new MwanziaException(String.format("Unable to deserialize json '%1$s': %2$s", json, e.getMessage()), e);
        }
    }

    public static <T> T deserialize(JsonParser parser, Class<T> clazz) {
        try {
            return buildObjectMapperForDeserialization().readValue(parser, clazz);
        } catch (Exception e) {
            throw new MwanziaException("Unable to deserialize json: " + e.getMessage(), e);
        }
    }

    private static ObjectMapper buildObjectMapper(boolean whitelistProperties) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(Feature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(Feature.INDENT_OUTPUT, true);
        objectMapper.setSerializerFactory(SERIALIZER_FACTORY);
        if (whitelistProperties) {
            objectMapper.setVisibilityChecker( objectMapper.getSerializationConfig().getDefaultVisibilityChecker()
                    .withFieldVisibility(JsonAutoDetect.Visibility.NONE)
                    .withGetterVisibility(JsonAutoDetect.Visibility.NONE));
        }
        return objectMapper;
    }

    private static ObjectMapper buildObjectMapperForDeserialization() {
        ObjectMapper objectMapper = buildObjectMapper(false);
        TypeResolverBuilder typer = new DefaultTypeResolverBuilder(DefaultTyping.JAVA_LANG_OBJECT.NON_FINAL) {
            @Override
            public boolean useForType(JavaType t) {
                Class rawClass = t.getRawClass();
                return !t.isFinal() && !rawClass.isArray() && !Collection.class.isAssignableFrom(rawClass)
                        && !Map.class.isAssignableFrom(rawClass) && !Number.class.isAssignableFrom(rawClass)
                        && !Date.class.isAssignableFrom(rawClass);
            }

            @Override
            protected TypeIdResolver idResolver(JavaType baseType, Collection<NamedType> subtypes, boolean forSer,
                    boolean forDeser) {
                return new OptimisticClassNameIdResolver(baseType);
            }
        };
        typer = typer.init(JsonTypeInfo.Id.CLASS, null);
        typer = typer.inclusion(As.PROPERTY);
        objectMapper.setDefaultTyping(typer);
        return objectMapper;
    }

    /**
     * Special serializer factory.
     * 
     * @author percy wegmann ( percy <at> karen and percy <dot> net )
     * 
     */
    private static class MwanziaSerializerFactory extends BeanSerializerFactory {
        @Override
        @SuppressWarnings("unchecked")
        public JsonSerializer<Object> createSerializer(JavaType type, SerializationConfig config) {
            Class<?> clazz = type.getRawClass();
            BasicBeanDescription beanDesc = config.introspect(type);

            // Serialize objects in a special way
            if (!Modifier.isFinal(clazz.getModifiers()) && !Collection.class.isAssignableFrom(clazz)
                    && !Map.class.isAssignableFrom(clazz) && !Date.class.isAssignableFrom(clazz)
                    && !Number.class.isAssignableFrom(clazz)) {
                return constructBeanSerializer(config, beanDesc);
            } else {
                return super.createSerializer(type, config);
            }
        }

        @Override
        protected JsonSerializer<?> buildCollectionSerializer(JavaType type, SerializationConfig config,
                BasicBeanDescription beanDesc) {
            return new MwanziaCollectionSerializer((CollectionSerializer) super.buildCollectionSerializer(type,
                    config,
                    beanDesc));
        }

        @Override
        protected JsonSerializer<Object> constructBeanSerializer(SerializationConfig config,
                BasicBeanDescription beanDesc) {
            // First: any detectable (auto-detect, annotations) properties to
            // serialize?
            List<BeanPropertyWriter> props = findBeanProperties(config, beanDesc);
            if (props == null || props.size() == 0) {
                // No properties, no serializer
                /*
                 * 27-Nov-2009, tatu: Except that as per [JACKSON-201], we are
                 * ok with that as long as it has a recognized class annotation
                 * (which may come from a mix-in too)
                 */
                if (beanDesc.hasKnownClassAnnotations()) {
                    return BeanSerializer.createDummy(beanDesc.getBeanClass());
                }
                return null;
            }
            // Any properties to suppress?
            props = filterBeanProperties(config, beanDesc, props);
            // Do they need to be sorted in some special way?
            props = sortBeanProperties(config, beanDesc, props);
            BeanSerializer ser = new MwanziaBeanSerializer(beanDesc.getBeanClass(), props);
            // 1.6: any-setter?
            AnnotatedMethod m = beanDesc.findAnyGetter();
            if (m != null) {
                JavaType type = m.getType(beanDesc.bindingsForBeanType());
                // copied from BasicSerializerFactory.buildMapSerializer():
                boolean staticTyping = config.isEnabled(SerializationConfig.Feature.USE_STATIC_TYPING);
                JavaType valueType = type.getContentType();
                TypeSerializer typeSer = createTypeSerializer(valueType, config);
                MapSerializer mapSer = MapSerializer.construct(
                /* ignored props */null, type, staticTyping, typeSer);
                ser.setAnyGetter(new AnyGetterWriter(m, mapSer));
            }

            // One more thing: need to gather view information, if any:
            ser = processViews(config, beanDesc, ser, props);
            return ser;
        }

        private class MwanziaBeanSerializer extends BeanSerializer {

            public MwanziaBeanSerializer(Class<?> type, BeanPropertyWriter[] props, BeanPropertyWriter[] filteredProps) {
                super(type, props, filteredProps);
            }

            public MwanziaBeanSerializer(Class<?> type, BeanPropertyWriter[] writers) {
                super(type, writers);
            }

            public MwanziaBeanSerializer(Class<?> type, Collection<BeanPropertyWriter> props) {
                super(type, props);
            }

            @Override
            protected void serializeFields(Object bean, JsonGenerator jgen, SerializerProvider provider)
                    throws IOException, JsonGenerationException {
                if (bean != null) {
                    for (SerializationModifier modifier : SERIALIZATION_MODIFIERS) {
                        bean = modifier.modify(bean);
                    }
                }
                if (bean == null)
                    jgen.writeNull();
                else {
                    String className = bean.getClass().getName();
                    jgen.writeFieldName(MWANZIA_TYPE);
                    jgen.writeString(className);
                    super.serializeFields(bean, jgen, provider);
                }
            }
        }

        private class MwanziaCollectionSerializer extends JsonSerializer<Collection> {
            private CollectionSerializer original;

            public MwanziaCollectionSerializer(CollectionSerializer original) {
                super();
                this.original = original;
            }

            @Override
            public void serialize(Collection value, JsonGenerator jgen, SerializerProvider provider)
                    throws IOException, JsonProcessingException {
                if (value != null) {
                    for (SerializationModifier modifier : SERIALIZATION_MODIFIERS) {
                        value = modifier.modify(value);
                    }
                }
                if (value != null)
                    original.serialize(value, jgen, provider);
                else
                    jgen.writeNull();
            }
        }
    }

    /**
     * A hook for transparently replacing objects during serialization.
     * 
     * @author percy wegmann ( percy <at> karen and percy <dot> net )
     * 
     */
    public static interface SerializationModifier {
        public <T> T modify(T original);
    }

    /**
     * A variant of ClassNameIdResolver that allows for the possibility that a
     * custom deserializer (as specified by @JsonDeserialize) might return a
     * different type than expected. This is useful, for example, to support the
     * pass by reference mechanism used by the JPA plugin.
     * 
     * @author percy wegmann ( percy <at> karen and percy <dot> net )
     * 
     */
    public static class OptimisticClassNameIdResolver extends ClassNameIdResolver {
        public OptimisticClassNameIdResolver(JavaType baseType) {
            super(baseType);
        }

        public JavaType typeFromId(String id) {
            /*
             * 30-Jan-2010, tatu: Most ids are basic class names; so let's first
             * check if any generics info is added; and only then ask factory to
             * do translation when necessary
             */
            if (id.indexOf('<') > 0) {
                JavaType t = TypeFactory.fromCanonical(id);
                // note: may want to try combining with specialization (esp for
                // EnumMap)
                return t;
            }
            try {
                /*
                 * [JACKSON-350]: Default Class.forName() won't work too well;
                 * context class loader seems like slightly better choice
                 */
                // Class<?> cls = Class.forName(id);
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                Class<?> cls = Class.forName(id, true, loader);
                JavaType baseType = _baseType;
                // If using custom deserialization, allow for the class to
                // change upon deserialization
                if (cls.isAnnotationPresent(JsonDeserialize.class))
                    baseType = TypeFactory.type(Object.class);
                return TypeFactory.specialize(baseType, cls);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Invalid type id '" + id
                        + "' (for id type 'Id.class'): no such class found");
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid type id '" + id + "' (for id type 'Id.class'): "
                        + e.getMessage(), e);
            }
        }

    }
}
