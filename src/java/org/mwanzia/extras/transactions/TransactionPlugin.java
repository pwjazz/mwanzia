package org.mwanzia.extras.transactions;

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
            public void beforeInvocation() throws Exception {
                CURRENT_TRANSACTION.set((T) beginTransaction());
            }

            @Override
            public void invocationSucceeded(Object target, Object result) throws Exception {
                try {
                    commit((T) CURRENT_TRANSACTION.get());
                } finally {
                    CURRENT_TRANSACTION.set(null);
                }
            }

            public Throwable invocationFailed(Throwable exception) throws Exception {
                try {
                    rollback((T) CURRENT_TRANSACTION.get());
                    return exception;
                } finally {
                    CURRENT_TRANSACTION.set(null);
                }
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
