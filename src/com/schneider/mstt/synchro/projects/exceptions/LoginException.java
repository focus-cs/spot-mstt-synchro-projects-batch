package com.schneider.mstt.synchro.projects.exceptions;

public class LoginException extends Exception {

    private static final long serialVersionUID = 7726356156153132770L;

    public LoginException(final String message) {
        super(message);
    }

    /**
     *
     * @param exception
     */
    public LoginException(final Throwable exception) {
        super(exception);

    }
}
