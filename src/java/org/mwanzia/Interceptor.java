package org.mwanzia;

import java.lang.reflect.Method;

/**
 * <p>
 * A class that intercepts method invocations. The default implementation does
 * not affect the invocation at all.
 * </p>
 * 
 * <p>
 * Interceptors are used throughout the duration of a method invocation and are
 * discarded afterwards, so it is safe for them to store instance state.
 * </p>
 * 
 * @author percy wegmann ( percy <at> karen and percy <dot> net )
 * 
 */
public class Interceptor {
    /**
     * This callback fires before the Application starts processing the
     * invocation (at the very beginning).
     * 
     * @throws Exception
     */
    public void beforeInvocation() throws Exception {
    }

    /**
     * This callback fires before the remote method is invoked on the target and
     * allows the interceptor to substitute another object for the target. This
     * callback does not fire for static method invocations, as there is no
     * target object.
     * 
     * @param target
     * @return
     * @throws Exception
     */
    public <T> T replaceTarget(T target) throws Exception {
        return target;
    }

    /**
     * This callback fires before the remote method is invoked and allows the
     * interceptor to replace the arguments to the method. This callback should
     * return the same number of arguments as were supplied.
     * 
     * @param target
     * @param method
     * @param arguments
     * @return
     * @throws Exception
     */
    public Object[] prepareInvocation(Object target, Method method, Object[] arguments) throws Exception {
        return arguments;
    }

    /**
     * This callback fires after the remote method has been successfully invoked
     * and allows the interceptor to replace the method's return value with a
     * different object.
     * 
     * @param result
     * @return
     * @throws Exception
     */
    public Object replaceResult(Object result) throws Exception {
        return result;
    }

    /**
     * This callback fires after the remote method has been invoked, if and only
     * if the method invocation did not throw an exception.
     * 
     * @param target
     * @param result
     * @throws Exception
     */
    public void invocationSucceeded(Object target, Object result) throws Exception {
    }

    /**
     * This callback fires after the remote method has been invoked, if and only
     * if the method invocation threw an exception.
     * 
     * @param Throwable
     * @param exception
     *            the exception thrown by the method invocation
     * @return
     * @throws Exception
     */
    public Throwable invocationFailed(Throwable exception) throws Exception {
        return exception;
    }
}
