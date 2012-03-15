package org.mwanzia.extras.transactions;

import java.lang.reflect.Method;

import org.mwanzia.Interceptor;
import org.mwanzia.Plugin;

/**
 * Plugin that handles transaction demarcation on calling a remote method.
 * 
 * @author percy wegmann ( percy <at> karen and percy <dot> net )
 * 
 * @param <T>
 *            type of transaction handle
 */
public abstract class TransactionPlugin<T> extends Plugin {
    private static final ThreadLocal CURRENT_TRANSACTION = new ThreadLocal();

    @Override
    public Interceptor buildInterceptor() {

        return new Interceptor() {
            @Override
            public void beforeInvocation(Class targetClass, Method method) throws Exception {
                if (requiresTransaction(method, method.getDeclaringClass())) {
                   CURRENT_TRANSACTION.set((T) beginTransaction());
                }
                super.beforeInvocation(targetClass, method);
            }
            
            @Override
            public Object[] prepareInvocation(Object target, Method method, Object[] arguments) throws Exception {
                
                return super.prepareInvocation(target, method, arguments);
            }

            @Override
            public void invocationSucceeded(Object target, Object result) throws Exception {
                try {
                    T currentTransaction = (T) CURRENT_TRANSACTION.get();
                    if (currentTransaction != null) {
                        commit(currentTransaction);
                    }
                } finally {
                    CURRENT_TRANSACTION.set(null);
                }
            }

            public Throwable invocationFailed(Throwable exception) throws Exception {
                try {
                    T currentTransaction = (T) CURRENT_TRANSACTION.get();
                    if (currentTransaction != null) {
                        rollback((T) CURRENT_TRANSACTION.get());
                    }
                    return exception;
                } finally {
                    CURRENT_TRANSACTION.set(null);
                }
            }

            private boolean requiresTransaction(Method method, Class clazz) {
                boolean methodRequiresTransaction = method.isAnnotationPresent(RequiresTransaction.class);
                boolean classRequiresTransaction = clazz.isAnnotationPresent(RequiresTransaction.class);
                if (methodRequiresTransaction || classRequiresTransaction)
                    return true;
                Class superclass = clazz.getSuperclass();
                if (superclass != null)
                    return requiresTransaction(method, superclass);
                return false;
            }
        };
    }

    /**
     * Begin a transaction.
     * 
     * @return a handle to the transaction
     */
    protected abstract T beginTransaction();

    /**
     * Commit the transaction.
     * 
     * @param transaction
     *            a handle to the transaction
     * @throws Exception
     */
    protected abstract void commit(T transaction) throws Exception;

    /**
     * Rollback the transaction.
     * 
     * @param transaction
     *            a handle to the transaction
     * @throws Exception
     */
    protected abstract void rollback(T transaction) throws Exception;
}
