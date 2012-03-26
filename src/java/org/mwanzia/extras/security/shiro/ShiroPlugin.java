package org.mwanzia.extras.security.shiro;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.subject.Subject;
import org.mwanzia.Application;
import org.mwanzia.Interceptor;
import org.mwanzia.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This plugin provides integration with the Apache Shiro security framework.
 * 
 * @author percy wegmann ( percy <at> karen and percy <dot> net )
 * 
 */
public class ShiroPlugin extends Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShiroPlugin.class);

    private ShiroSecuredApplication application;

    public ShiroPlugin(ShiroSecuredApplication application) {
        super((Application) application);
        this.application = application;
    }

    @Override
    public List<Class> getRemoteTypes() {
        return Arrays.asList(new Class[] { AuthenticationException.class, AuthorizationException.class });
    }

    @Override
    public Interceptor buildInterceptor() {
        return new Interceptor() {
            @Override
            public void beforeInvocation(Class targetClass, Method method) throws Exception {
                MwanziaShiroRealm.setApplication(application);
                super.beforeInvocation(targetClass, method);
            }

            @Override
            public Object[] prepareInvocation(Object target, Method method, Object[] arguments) throws Exception {
                if (method.isAnnotationPresent(RequiresAuthentication.class)
                        || method.isAnnotationPresent(RequiresRoles.class)) {
                    Subject currentUser = SecurityUtils.getSubject();
                    if (currentUser == null || !currentUser.isAuthenticated())
                        throw new AuthenticationException("Please log in");
                    if (method.isAnnotationPresent(RequiresRoles.class)) {
                        for (String role : method.getAnnotation(RequiresRoles.class).value().split(",")) {
                            role = role.trim();
                            if (role.length() > 0 && !currentUser.hasRole(role.trim())) {
                                throw new AuthorizationException("Missing required role: " + role);
                            }
                        }
                    }
                }
                return super.prepareInvocation(target, method, arguments);
            }

            @Override
            public void invocationSucceeded(Object target, Object result) throws Exception {
                MwanziaShiroRealm.setApplication(null);
                super.invocationSucceeded(target, result);
            }

            public Throwable invocationFailed(Throwable exception) throws Exception {
                MwanziaShiroRealm.setApplication(null);
                return super.invocationFailed(exception);
            }
        };
    }
}
