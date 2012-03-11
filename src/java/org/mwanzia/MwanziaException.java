package org.mwanzia;

/**
 * <p>
 * Represents an unexpected condition encountered while processing a remote
 * request.
 * </p>
 * 
 * @author percy wegmann ( percy <at> karen and percy <dot> net )
 * 
 */
public class MwanziaException extends RuntimeException {
    private static final long serialVersionUID = 572749485020777691L;

    public MwanziaException() {
    }

    public MwanziaException(String message) {
        super(message);
    }

    public MwanziaException(Throwable cause) {
        super(cause);
    }

    public MwanziaException(String message, Throwable cause) {
        super(message, cause);
    }

}
