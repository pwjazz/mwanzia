package org.mwanzia.extras.security.shiro;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;

/**
 * Applications that use Shiro for security should implement this interface to
 * link authentication and authorization to their specific storage mechanism.
 * 
 * @author percy wegmann ( percy <at> karen and percy <dot> net )
 * 
 */
public interface ShiroSecuredApplication {
    /**
     * @see org.apache.shiro.realm.AuthenticatingRealm.doGetAuthenticationInfo()
     * @param token
     * @return
     * @throws AuthenticationException
     */
    AuthenticationInfo getAuthenticationInfo(AuthenticationToken token) throws AuthenticationException;

    /**
     * @see org.apache.shiro.realm.AuthorizingRealm.doGetAuthorizationInfo()
     * @param principals
     * @return
     */
    AuthorizationInfo getAuthorizationInfo(PrincipalCollection principals);
}
