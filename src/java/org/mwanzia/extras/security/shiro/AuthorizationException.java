package org.mwanzia.extras.security.shiro;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Indicates that there was a problem authorizing the user's access to a method.
 * 
 * @author percy wegmann ( percy <at> karen and percy <dot> net )
 * 
 */
public class AuthorizationException extends Exception {
    private static final long serialVersionUID = -4043065816189083548L;

    public AuthorizationException() {
        super();
    }

    public AuthorizationException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    public AuthorizationException(String arg0) {
        super(arg0);
    }

    public AuthorizationException(Throwable arg0) {
        super(arg0);
    }

    @JsonProperty
    public String getMessage() {
        return super.getMessage();
    }
}
