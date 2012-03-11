package org.mwanzia;

/**
 * <p>
 * Represents a serious, unexpected condition (such as one caused by a
 * misconfiguration of the system or a code error).
 * </p>
 * 
 * @author percy wegmann ( percy <at> karen and percy <dot> net )
 * 
 */
public class MwanziaError extends Error {
    private static final long serialVersionUID = 238087174296579922L;

    public MwanziaError() {
    }

    public MwanziaError(String message) {
        super(message);
    }

    public MwanziaError(Throwable cause) {
        super(cause);
    }

    public MwanziaError(String message, Throwable cause) {
        super(message, cause);
    }

}
