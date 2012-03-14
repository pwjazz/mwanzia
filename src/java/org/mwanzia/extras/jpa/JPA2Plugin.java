package org.mwanzia.extras.jpa;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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
public abstract class JPA2Plugin extends AbstractJPAPlugin {
    @Override
    public Interceptor buildInterceptor() {
        return new JPA2Interceptor();
    }

    @Override
    public List<Class> getRemoteTypes() {
        List<Class> remoteTypes = new ArrayList<Class>(super.getRemoteTypes());
        for (ManagedType managedType : getEntityManager().getEntityManagerFactory().getMetamodel().getManagedTypes()) {
            remoteTypes.add(managedType.getJavaType());
        }
        return remoteTypes;
    }

    protected class JPA2Interceptor extends JPAInterceptor {
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