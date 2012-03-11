package org.mwanzia.extras.jpa;

import javax.persistence.Entity;
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
 * most recent version of the entity. If the entity uses optimistic locking,
 * this plugin will use the version number from the client so that optimistic
 * locking conflicts can be correctly detected on the server.
 * </p>
 * 
 * @author percy
 */
public abstract class JPAPlugin extends Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(JPAPlugin.class);
    private static final ThreadLocal<EntityManager> CURRENT_ENTITY_MANAGER = new ThreadLocal<EntityManager>();

    public static EntityManager getCurrentEntityManager() {
        return CURRENT_ENTITY_MANAGER.get();
    }

    @Override
    public Interceptor buildInterceptor() {
        return new JPAInterceptor();
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
                if (target != null && target.getClass().isAnnotationPresent(Entity.class)) {
                    LOGGER.debug("Saving target {}", target);
                    getEntityManager().persist(target);
                }
            } finally {
                CURRENT_ENTITY_MANAGER.set(null);
            }
        }

        public <T extends Throwable> T invocationFailed(T exception) throws Exception {
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