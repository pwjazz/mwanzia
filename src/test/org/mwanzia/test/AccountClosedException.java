package org.mwanzia.test;

public class AccountClosedException extends Exception {
    private static final long serialVersionUID = -6209840486428619180L;

    public AccountClosedException() {
        super();
    }

    public AccountClosedException(String message, Throwable cause) {
        super(message, cause);
    }

    public AccountClosedException(String message) {
        super(message);
    }

    public AccountClosedException(Throwable cause) {
        super(cause);
    }
}
