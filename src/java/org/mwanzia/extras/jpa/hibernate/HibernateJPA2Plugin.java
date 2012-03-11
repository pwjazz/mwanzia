package org.mwanzia.extras.jpa.hibernate;

import org.hibernate.Hibernate;
import org.mwanzia.Interceptor;
import org.mwanzia.JSON;
import org.mwanzia.JSON.SerializationModifier;
import org.mwanzia.extras.jpa.JPA2Plugin;

/**
 * Special version of JPA plugin that adds support for remote lazy loading (if
 * using Hibernate).
 * 
 * @author percy
 * 
 */
public abstract class HibernateJPA2Plugin extends JPA2Plugin {
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
    public Interceptor buildInterceptor() {
        return new HibernateInterceptor();
    }

    protected class HibernateInterceptor extends JPAInterceptor {
        @Override
        public Object replaceResult(Object result) throws Exception {
            Hibernate.initialize(result);
            return super.replaceResult(result);
        }
    }
}
