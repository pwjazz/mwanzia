package org.mwanzia.extras.jpa;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Type.PersistenceType;

import org.mwanzia.Interceptor;

/**
 * <p>
 * Version of JPAPlugin that takes advantage of JPA2 features to:
 * </p>
 * 
 * <ul>
 * <li>Automatically register all managed entities as remote objects</li>
 * <li>Force managed entities to be passed by reference (unless overridden with
 * the @ByValue annotation)</li>
 * </ul>
 * 
 * @author percy
 */
public abstract class JPA2Plugin extends JPAPlugin {
    @Override
    public Interceptor buildInterceptor() {
        return new JPA2Interceptor();
    }

    @Override
    public List<Class> getRemoteTypes() {
        List<Class> remoteTypes = new ArrayList<Class>(super.getRemoteTypes());
        for (ManagedType managedType : getManagedTypes()) {
            remoteTypes.add(managedType.getJavaType());
        }
        return remoteTypes;
    }

    protected Set<ManagedType<?>> getManagedTypes() {
        return getEntityManager().getMetamodel().getManagedTypes();
    }

    protected class JPA2Interceptor extends JPAInterceptor {
        protected boolean isManagedEntity(Object object) {
            if (object == null)
                return false;
            Class clazz = object.getClass();
            for (ManagedType managedType : getManagedTypes()) {
                if (PersistenceType.EMBEDDABLE != managedType.getPersistenceType()
                        && managedType.getJavaType().isAssignableFrom(clazz))
                    return true;
            }
            return false;
        }
    }

}