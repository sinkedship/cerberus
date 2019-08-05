package com.sinkedship.cerberus.commons.exception;

/**
 * @author Derrick Guan
 */
public class CerberusException extends RuntimeException {

    public CerberusException() {
    }

    public CerberusException(String message) {
        super(message);
    }

    public CerberusException(String message, Throwable cause) {
        super(message, cause);
    }

    public CerberusException(Throwable cause) {
        super(cause);
    }

    public CerberusException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
