package org.mwanzia.extras.security.shiro;

import java.lang.reflect.Method;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.authz.annotation.RequiresUser;
import org.apache.shiro.subject.Subject;
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
        super();
        this.application = application;
    }

    @Override
    public Interceptor buildInterceptor() {
        return new Interceptor() {
            @Override
            public void beforeInvocation() throws Exception {
                MwanziaShiroRealm.setApplication(application);
                super.beforeInvocation();
            }

            @Override
            public Object[] prepareInvocation(Object target, Method method, Object[] arguments) throws Exception {
                if (method.isAnnotationPresent(RequiresAuthentication.class)
                        || method.isAnnotationPresent(RequiresPermissions.class)
                        || method.isAnnotationPresent(RequiresRoles.class)
                        || method.isAnnotationPresent(RequiresUser.class)) {
                    Subject currentUser = SecurityUtils.getSubject();
                    if (currentUser == null || !currentUser.isAuthenticated())
                        throw new AuthenticationException("Please log in");
                }
                return super.prepareInvocation(target, method, arguments);
            }

            @Override
            public void invocationSucceeded(Object target, Object result) throws Exception {
                MwanziaShiroRealm.setApplication(null);
                super.invocationSucceeded(target, result);
            }

            public <T extends Throwable> T invocationFailed(T exception) throws Exception {
                MwanziaShiroRealm.setApplication(null);
                return super.invocationFailed(exception);
            }
        };
    }
}
