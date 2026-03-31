package io.mcache.sdk;

/** Thrown when inserting a prefix that already exists. */
public class AlreadyExistsException extends McacheException {

    public AlreadyExistsException(String prefix) {
        super("mcache: already exists: " + prefix);
    }
}
