package org.mwanzia.test;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import net.sf.oval.guard.Guarded;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.codehaus.jackson.map.ObjectMapper;
import org.mwanzia.Remote;
import org.mwanzia.extras.jackson.JacksonApplication;
import org.mwanzia.extras.jpa.hibernate.HibernateJPA2Plugin;
import org.mwanzia.extras.security.shiro.ShiroPlugin;
import org.mwanzia.extras.security.shiro.ShiroSecuredApplication;
import org.mwanzia.extras.transactions.TransactionPlugin;
import org.mwanzia.extras.validation.ValidationPlugin;
import org.mwanzia.extras.validation.validators.Required;

@Guarded
public class TestApplication extends JacksonApplication implements ShiroSecuredApplication {
    static {
        // Initialize the JPA persistence context
        JPA.initialize("demo");
    }

    private final ObjectMapper mapper = new ObjectMapper();

    public TestApplication() {
        super();
        // Register a plugin for doing transaction management
        registerPlugin(new TransactionPlugin<EntityTransaction>(this) {
            @Override
            protected EntityTransaction beginTransaction() {
                JPA.getInstance().clear();
                EntityTransaction transaction = JPA.getInstance().getEntityManager().getTransaction();
                transaction.begin();
                return transaction;
            }

            @Override
            protected void commit(EntityTransaction transaction) throws Exception {
                transaction.commit();
            }

            @Override
            protected void rollback(EntityTransaction transaction) throws Exception {
                transaction.rollback();
            }
        });
        registerPlugin(new HibernateJPA2Plugin(this) {
            @Override
            protected EntityManager getEntityManager() {
                return JPA.getInstance().getEntityManager();
            }
        });
        registerPlugin(new ValidationPlugin(this));
        registerPlugin(new ShiroPlugin(this));
    }

    public State[] getStates() {
        return State.values();
    }

    @Remote
    public void login(@Required String username, @Required char[] password) throws AuthenticationException {
        Subject currentUser = SecurityUtils.getSubject();
        if (currentUser.isAuthenticated()) {
            currentUser.logout();
        }
        currentUser.login(new UsernamePasswordToken(username, password));
    }

    @Override
    public AuthenticationInfo getAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        // This is where you would hook into your user database, whether ldap or
        // something else.
        // If one of the out-of-the-box Shiro realms works for you, you can just
        // configure that in shiro.ini.
        // The below implementation just echos whatever username and password
        // were passed in (i.e. everyone authenticates)
        String username = (String) token.getPrincipal();
        // Hash password using username as salt and doing 1024 iterations
        String hashedPassword = new Sha256Hash(token.getCredentials(), username, 1024).toBase64();
        return new SimpleAccount(token.getPrincipal(), hashedPassword, "mwanzia");
    }

    @Override
    public AuthorizationInfo getAuthorizationInfo(PrincipalCollection principals) {
        // This implementation just returns the same roles for everybody
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        info.addRole("corporate");
        info.addRole("manager");
        return info;
    }
}
