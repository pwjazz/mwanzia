package org.mwanzia.extras.jpa.appengine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.datanucleus.store.appengine.jpa.DatastoreEntityManager;
import org.mwanzia.Application;
import org.mwanzia.Interceptor;
import org.mwanzia.extras.jpa.JPAPlugin;

/**
 * <p>
 * Version of JPAPlugin that works on Google's AppEngine.
 * </p>
 * 
 * @author percy
 */
public abstract class AppEngineJPAPlugin extends JPAPlugin {
    public AppEngineJPAPlugin(Application application) {
        super(application);
    }

    @Override
    public Interceptor buildInterceptor() {
        return new AppEngineJPAInterceptor();
    }

    @Override
    public List<Class> getRemoteTypes() {
        List<Class> remoteTypes = new ArrayList<Class>(super.getRemoteTypes());
        remoteTypes.addAll(getManagedClasses());
        return remoteTypes;
    }

    protected Set<Class> getManagedClasses() {
        DatastoreEntityManager entityManager = (DatastoreEntityManager) getEntityManager();
        Collection<String> classNames = entityManager.getObjectManager().getMetaDataManager().getClassesWithMetaData();
        Set<Class> classes = new HashSet<Class>();
        ClassLoader loader = getClass().getClassLoader();
        try {
            for (String className : classNames) {
                classes.add(loader.loadClass(className));
            }
            return classes;
        } catch (ClassNotFoundException cnfe) {
            throw new RuntimeException("Unable to find class: " + cnfe.getMessage(), cnfe);
        }
    }

    protected class AppEngineJPAInterceptor extends JPAInterceptor {
        protected boolean isManagedEntity(Object object) {
            if (object == null)
                return false;
            Class clazz = object.getClass();
            for (Class managedClass : getManagedClasses()) {
                if (managedClass.isAssignableFrom(clazz))
                    return true;
            }
            return false;
        }
    }

}