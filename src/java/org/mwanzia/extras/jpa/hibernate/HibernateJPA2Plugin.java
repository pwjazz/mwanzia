package org.mwanzia.extras.jpa.hibernate;

import java.util.Map;

import org.hibernate.Hibernate;
import org.mwanzia.Application;
import org.mwanzia.Interceptor;
import org.mwanzia.JSON;
import org.mwanzia.JSON.SerializationModifier;
import org.mwanzia.extras.jpa.JPA2Plugin;

/**
 * Special version of JPA2Plugin that adds support for remote lazy loading (if
 * using Hibernate).
 * 
 * @author percy
 * 
 */
public abstract class HibernateJPA2Plugin extends JPA2Plugin {
    static {
        JSON.addSerializationModifier(new SerializationModifier() {
            public <T> T modify(T original, Map<String, Object> serializationContext) {
                return HibernatePluginUtil.handleLazyInitialization(original);
            }
        });
    }

    public HibernateJPA2Plugin(Application application) {
        super(application);
    }

    @Override
    public Interceptor buildInterceptor() {
        return new HibernateInterceptor();
    }

    protected class HibernateInterceptor extends JPA2Interceptor {
        @Override
        public Object replaceResult(Object result) throws Exception {
            Hibernate.initialize(result);
            return super.replaceResult(result);
        }
    }
}
