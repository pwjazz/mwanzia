package org.mwanzia.extras.jpa;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.EntityManager;

import org.mwanzia.Application;
import org.mwanzia.Interceptor;
import org.mwanzia.JSON;
import org.mwanzia.JSON.DeserializationModifier;
import org.mwanzia.JSON.SerializationModifier;
import org.mwanzia.Plugin;
import org.mwanzia.SmallPropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Plugin that adds support for JPA entities. When an entity is the target of a
 * remote call, this Plugin will make sure that the entity has been loaded from
 * the database based on it's ID so that the call is always made against the
 * most recent version of the entity.
 * </p>
 * 
 * <p>
 * Users of the plugin are responsible for implementing the getEntityManager()
 * method in order to hook into their environment.
 * </p>
 * 
 * @author percy
 */
public abstract class JPAPlugin extends Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(JPAPlugin.class);
    private static final ThreadLocal<EntityManager> CURRENT_ENTITY_MANAGER = new ThreadLocal<EntityManager>();

    static {
        JSON.addDeserializationModifier(new DeserializationModifier() {
            public <T> T modify(T deserialized, Object original) throws Exception {
                if (original instanceof Map) {
                    Map map = (Map) original;
                    if ("org.mwanzia.extras.jpa.Reference".equals(map.get("@class"))) {
                        return (T) new Reference(deserialized).dereference();
                    }
                }
                return deserialized;
            }
        });

        JSON.addSerializationModifier(new SerializationModifier() {
            public <T> T modify(T original, Map<String, Object> serializationContext) throws Exception {
                T result = original;
                if (original != null && original.getClass().isAnnotationPresent(Entity.class)) {
                    EntityKey key = new EntityKey(original);
                    Map<EntityKey, Object> serializedObjects = (Map<EntityKey, Object>) serializationContext
                            .get("_alreadySerialized");
                    if (serializedObjects == null) {
                        serializedObjects = new HashMap<EntityKey, Object>();
                        serializationContext.put("_alreadySerialized", serializedObjects);
                    }
                    T alreadySerialized = (T) serializedObjects.get(key);
                    if (alreadySerialized != null) {
                        // This object has already been serialized - just put in
                        // a reference
                        return (T) new Reference(original);
                    } else {
                        serializedObjects.put(key, original);
                    }
                }
                return original;
            };
        });
    }

    public JPAPlugin(Application application) {
        super(application);
    }

    public static EntityManager getCurrentEntityManager() {
        return CURRENT_ENTITY_MANAGER.get();
    }

    protected abstract EntityManager getEntityManager();

    public abstract class JPAInterceptor extends Interceptor {
        @Override
        public void beforeInvocation(Class targetClass, Method method) throws Exception {
            CURRENT_ENTITY_MANAGER.set(getEntityManager());
            super.beforeInvocation(targetClass, method);
        }

        public Object[] prepareInvocation(Object target, Method method, Object[] arguments) throws Exception {
            Object[] modifiedArguments = new Object[arguments.length];
            int i = 0;
            // Force call by reference semantics for parameters that are managed
            // entities, unless the parameter is annotated with @ByValue
            for (Annotation[] parameterAnnotations : method.getParameterAnnotations()) {
                Object argument = arguments[i];
                boolean forceByReference = argument != null && !Reference.class.isAssignableFrom(argument.getClass())
                        && isManagedEntity(argument);
                if (forceByReference && argument != null) {
                    for (Annotation annotation : parameterAnnotations) {
                        if (ByValue.class.isAssignableFrom(annotation.getClass())) {
                            // Override forced by reference behavior
                            forceByReference = false;
                            break;
                        }
                    }
                }
                if (forceByReference) {
                    modifiedArguments[i] = new Reference(argument).dereference();
                } else {
                    modifiedArguments[i] = argument;
                }
                i += 1;
            }
            return super.prepareInvocation(target, method, modifiedArguments);
        }

        @Override
        public void invocationSucceeded(Object target, Object result) throws Exception {
            try {
                if (target != null && isManagedEntity(target)) {
                    LOGGER.debug("Saving target {}", target);
                    getEntityManager().persist(target);
                }
            } finally {
                CURRENT_ENTITY_MANAGER.set(null);
            }
        }

        public Throwable invocationFailed(Throwable exception) throws Exception {
            try {
                return exception;
            } finally {
                CURRENT_ENTITY_MANAGER.set(null);
            }
        }

        protected abstract boolean isManagedEntity(Object object);
    }

    private static class EntityKey {
        public Class clazz;
        public Serializable id;

        public EntityKey(Object entity) {
            this.clazz = entity.getClass();
            this.id = SmallPropertyUtils.readProperty(entity, "id");
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            EntityKey other = (EntityKey) obj;
            if (clazz == null) {
                if (other.clazz != null)
                    return false;
            } else if (!clazz.equals(other.clazz))
                return false;
            if (id == null) {
                if (other.id != null)
                    return false;
            } else if (!id.equals(other.id))
                return false;
            return true;
        }

    }

}