package com.schneider.mstt.synchro.projects.exceptions;

import com.sciforma.psnext.api.PSException;

/**
 * Triggered if we have a problem to logging out from PSNext<br>
 */
public class ErrorLoggingOutException extends PSException {

    private static final long serialVersionUID = 6249833140231753655L;

    /**
     *
     * @param message The message to print.
     * @param rootCause Root exception.
     */
    public ErrorLoggingOutException(final String message, final Throwable rootCause) {
        super(message, rootCause);
    }

    /**
     *
     * @param message The message to print.
     */
    public ErrorLoggingOutException(final String message) {
        super(message);
    }

}
