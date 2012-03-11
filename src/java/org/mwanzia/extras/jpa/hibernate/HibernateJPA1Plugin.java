package org.mwanzia.extras.jpa.hibernate;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.tuple.EntityModeToTuplizerMapping;
import org.hibernate.tuple.Tuplizer;
import org.hibernate.tuple.component.PojoComponentTuplizer;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;
import org.mwanzia.Interceptor;
import org.mwanzia.JSON;
import org.mwanzia.JSON.SerializationModifier;
import org.mwanzia.MwanziaError;
import org.mwanzia.extras.jpa.ByValue;
import org.mwanzia.extras.jpa.JPAPlugin;
import org.mwanzia.extras.jpa.Reference;

/**
 * Special version of JPA plugin that adds support for remote lazy loading (if
 * using Hibernate).
 * 
 * @author percy
 * 
 */
public abstract class HibernateJPA1Plugin extends JPAPlugin {
    static {
        JSON.addSerializationModifier(new SerializationModifier() {
            public <T> T modify(T original) {
                if (!Hibernate.isInitialized(original))
                    return null;
                else
                    return original;
            }
        });
    }

    @Override
    public List<Class> getRemoteTypes() {
        List<Class> remoteTypes = super.getRemoteTypes();
        for (Object entryObject : getSession().getSessionFactory().getAllClassMetadata().entrySet()) {
            Map.Entry<String, ClassMetadata> entry = (Map.Entry<String, ClassMetadata>) entryObject;
            String className = entry.getKey();
            ClassMetadata metaData = entry.getValue();
            try {
                remoteTypes.add(this.getClass().getClassLoader().loadClass(className));
            } catch (ClassNotFoundException cnfe) {
                throw new MwanziaError("Unable to auto-register entity of type: " + className, cnfe);
            }
            Field tuplizerMappingField = null;
            try {
                tuplizerMappingField = ComponentType.class.getDeclaredField("tuplizerMapping");
                tuplizerMappingField.setAccessible(true);
                for (Type propertyType : metaData.getPropertyTypes()) {
                    if (propertyType instanceof ComponentType) {
                        Tuplizer tuplizer = ((EntityModeToTuplizerMapping) tuplizerMappingField.get(propertyType))
                                .getTuplizer(EntityMode.POJO);
                        Class mappedClass = ((PojoComponentTuplizer) tuplizer).getMappedClass();
                        remoteTypes.add(mappedClass);
                    }
                }
            } catch (NoSuchFieldException nsfe) {
                throw new MwanziaError("No tuplizerMapping field defined on ComponentType", nsfe);
            } catch (IllegalAccessException iae) {
                throw new MwanziaError("Unable to access tuplizerMapping on ComponentType", iae);
            } finally {
                if (tuplizerMappingField != null)
                    tuplizerMappingField.setAccessible(false);
            }
        }
        return remoteTypes;
    }

    @Override
    public Interceptor buildInterceptor() {
        return new HibernateInterceptor();
    }

    protected class HibernateInterceptor extends JPAInterceptor {
        @Override
        public Object[] prepareInvocation(Object target, Method method, Object[] arguments) throws Exception {
            Object[] modifiedArguments = new Object[arguments.length];
            int i = 0;
            // Force call by reference semantics for parameters that are managed
            // entities, unless the parameter is annotated with @ByValue
            for (Annotation[] parameterAnnotations : method.getParameterAnnotations()) {
                Object argument = arguments[i];
                boolean forceByReference = !Reference.class.isAssignableFrom(argument.getClass())
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
        public Object replaceResult(Object result) throws Exception {
            Hibernate.initialize(result);
            return super.replaceResult(result);
        }

        private boolean isManagedEntity(Object object) {
            if (object == null)
                return false;
            Class clazz = object.getClass();
            ClassMetadata metaData = getSession().getSessionFactory().getClassMetadata(clazz);
            return metaData != null && metaData.hasIdentifierProperty();
        }
    }

    protected Session getSession() {
        EntityManager entityManager = getEntityManager();
        for (PropertyDescriptor descriptor : PropertyUtils.getPropertyDescriptors(entityManager)) {
            if (Session.class.isAssignableFrom(descriptor.getPropertyType()))
                try {
                    return (Session) descriptor.getReadMethod().invoke(entityManager);
                } catch (Exception e) {
                    throw new MwanziaError("Unable to read Hibernate Session from EntityManager: " + e.getMessage(), e);
                }

        }
        throw new MwanziaError("Unable to discover Hibernate Session from EntityManager of type: "
                + entityManager.getClass().getName());
    }
}
