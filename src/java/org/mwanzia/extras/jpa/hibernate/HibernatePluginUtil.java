package org.mwanzia.extras.jpa.hibernate;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.proxy.AbstractLazyInitializer;
import org.hibernate.proxy.pojo.javassist.JavassistLazyInitializer;
import org.mwanzia.extras.jpa.Reference;

public class HibernatePluginUtil {
    public static <T> T handleLazyInitialization(T original) {
        if (original == null)
            return null;
        if (!Hibernate.isInitialized(original)) {
            if (original instanceof Collection) {
                List<Object> result = new ArrayList<Object>();
                for (Object item : (Collection) original) {
                    result.add(Hibernate.isInitialized(item) ? item : objectToStub(item));
                }
                return (T) result;
            } else if (original instanceof Map) {
                Map<Object, Object> result = new HashMap<Object, Object>();
                for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) original).entrySet()) {
                    result.put(entry.getKey(), Hibernate.isInitialized(entry.getValue()) ? entry.getValue()
                            : objectToStub(entry.getValue()));
                }
                return (T) result;
            } else {
                return (T) objectToStub(original);
            }
        } else {
            return original;
        }
    }

    private static <T> T objectToStub(T original) {
        try {
            Field initializerField = original.getClass().getDeclaredField("handler");
            initializerField.setAccessible(true);
            JavassistLazyInitializer initializer = (JavassistLazyInitializer) initializerField.get(original);
            Field idField = AbstractLazyInitializer.class.getDeclaredField("id");
            idField.setAccessible(true);
            Serializable id = (Serializable) idField.get(initializer);
            return (T) new Reference(original.getClass(), id);
        } catch (Exception e) {
            throw new RuntimeException("Unable to convert to stub: " + e.getMessage(), e);
        }
    }
}
