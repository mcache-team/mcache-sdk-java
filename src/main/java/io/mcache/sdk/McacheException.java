package io.mcache.sdk;

/** Base exception for all mcache SDK errors. */
public class McacheException extends RuntimeException {

    public McacheException(String message) {
        super(message);
    }

    public McacheException(String message, Throwable cause) {
        super(message, cause);
    }
}
