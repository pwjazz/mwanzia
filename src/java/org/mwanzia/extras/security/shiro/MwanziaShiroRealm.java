package org.mwanzia.extras.security.shiro;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

public class MwanziaShiroRealm extends AuthorizingRealm {
    private static final ThreadLocal<ShiroSecuredApplication> APPLICATION = new ThreadLocal<ShiroSecuredApplication>();

    public static void setApplication(ShiroSecuredApplication application) {
        APPLICATION.set(application);
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        return APPLICATION.get().getAuthenticationInfo(token);
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        return APPLICATION.get().getAuthorizationInfo(principals);
    }

}
