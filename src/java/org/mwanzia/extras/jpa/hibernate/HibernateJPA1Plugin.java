package org.mwanzia.extras.jpa.hibernate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.tuple.EntityModeToTuplizerMapping;
import org.hibernate.tuple.Tuplizer;
import org.hibernate.tuple.component.PojoComponentTuplizer;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;
import org.mwanzia.Application;
import org.mwanzia.Interceptor;
import org.mwanzia.JSON;
import org.mwanzia.JSON.SerializationModifier;
import org.mwanzia.MwanziaError;
import org.mwanzia.SmallPropertyUtils;
import org.mwanzia.SmallPropertyUtils.Property;
import org.mwanzia.extras.jpa.JPAPlugin;

/**
 * Special version of JPAPlugin that adds support for remote lazy loading (if
 * using Hibernate). It also enforces call by reference semantics.
 * 
 * @author percy
 * 
 */
public abstract class HibernateJPA1Plugin extends JPAPlugin {
    static {
        JSON.addSerializationModifier(new SerializationModifier() {
            public <T> T modify(T original, Property property, Map<String, Object> serializationContext) {
                return HibernatePluginUtil.handleLazyInitialization(original);
            }
        });
    }

    public HibernateJPA1Plugin(Application application) {
        super(application);
    }

    @Override
    public List<Class> getRemoteTypes() {
        List<Class> remoteTypes = new ArrayList<Class>(super.getRemoteTypes());
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
        public Object replaceResult(Object result) throws Exception {
            Hibernate.initialize(result);
            return super.replaceResult(result);
        }

        protected boolean isManagedEntity(Object object) {
            if (object == null)
                return false;
            Class clazz = object.getClass();
            ClassMetadata metaData = getSession().getSessionFactory().getClassMetadata(clazz);
            return metaData != null && metaData.hasIdentifierProperty();
        }
    }

    protected Session getSession() {
        EntityManager entityManager = getEntityManager();
        for (Property descriptor : SmallPropertyUtils.getProperties(entityManager).values()) {
            if (Session.class.isAssignableFrom(descriptor.getPropertyType()))
                try {
                    return (Session) descriptor.read(entityManager);
                } catch (Exception e) {
                    throw new MwanziaError("Unable to read Hibernate Session from EntityManager: " + e.getMessage(), e);
                }

        }
        throw new MwanziaError("Unable to discover Hibernate Session from EntityManager of type: "
                + entityManager.getClass().getName());
    }
}
