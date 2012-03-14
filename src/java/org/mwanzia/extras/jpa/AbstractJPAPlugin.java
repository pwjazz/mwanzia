package org.mwanzia.extras.jpa;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Type.PersistenceType;

import org.mwanzia.Interceptor;
import org.mwanzia.Plugin;
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
public abstract class AbstractJPAPlugin extends Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractJPAPlugin.class);
    private static final ThreadLocal<EntityManager> CURRENT_ENTITY_MANAGER = new ThreadLocal<EntityManager>();

    public static EntityManager getCurrentEntityManager() {
        return CURRENT_ENTITY_MANAGER.get();
    }

    protected abstract EntityManager getEntityManager();

    protected class JPAInterceptor extends Interceptor {
        @Override
        public void beforeInvocation() throws Exception {
            CURRENT_ENTITY_MANAGER.set(getEntityManager());
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

        private boolean isManagedEntity(Object object) {
            if (object == null)
                return false;
            Class clazz = object.getClass();
            for (ManagedType managedType : getEntityManager().getEntityManagerFactory().getMetamodel()
                    .getManagedTypes()) {
                if (PersistenceType.EMBEDDABLE != managedType.getPersistenceType()
                        && managedType.getJavaType().isAssignableFrom(clazz))
                    return true;
            }
            return false;
        }
    }
}