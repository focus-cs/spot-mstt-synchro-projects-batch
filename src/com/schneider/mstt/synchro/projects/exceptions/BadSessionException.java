package com.schneider.mstt.synchro.projects.exceptions;

public class BadSessionException extends Exception {

    private static final long serialVersionUID = -8960271439142291940L;

    public BadSessionException(final String message) {
        super(message);
    }

    /**
     *
     * @param exception
     */
    public BadSessionException(final Throwable exception) {
        super(exception);
    }
}
