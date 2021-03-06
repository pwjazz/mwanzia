package org.mwanzia.extras.security.shiro;

/**
 * Indicates that there was a problem with the user's login.
 * 
 * @author percy wegmann ( percy <at> karen and percy <dot> net )
 * 
 */
public class AuthenticationException extends Exception {
    private static final long serialVersionUID = -4043065816189083548L;

    public AuthenticationException() {
        super();
    }

    public AuthenticationException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    public AuthenticationException(String arg0) {
        super(arg0);
    }

    public AuthenticationException(Throwable arg0) {
        super(arg0);
    }

}
