package io.mcache.sdk;

/** Thrown when the requested prefix does not exist or has expired. */
public class NotFoundException extends McacheException {

    public NotFoundException(String prefix) {
        super("mcache: not found: " + prefix);
    }
}
