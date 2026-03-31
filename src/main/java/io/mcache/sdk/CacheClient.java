package io.mcache.sdk;

import java.util.List;

/**
 * Unified interface for interacting with mcache.
 *
 * <p>Two implementations are provided:
 * <ul>
 *   <li>{@link io.mcache.sdk.http.HttpCacheClient} — communicates over HTTP REST</li>
 *   <li>{@link io.mcache.sdk.grpc.GrpcCacheClient} — communicates over gRPC</li>
 * </ul>
 *
 * <p>Both implementations are {@link AutoCloseable}; use them in a try-with-resources block
 * to ensure connections are released.
 */
public interface CacheClient extends AutoCloseable {

    /**
     * Create a new cache entry.
     *
     * @param prefix      hierarchical key path, e.g. {@code "user/profile/name"}
     * @param data        any JSON-serialisable value
     * @param ttlSeconds  time-to-live in seconds; {@code 0} means no expiry
     * @throws AlreadyExistsException if the prefix already exists
     * @throws McacheException        on transport or server errors
     */
    void insert(String prefix, Object data, long ttlSeconds);

    /** Convenience overload with no TTL. */
    default void insert(String prefix, Object data) {
        insert(prefix, data, 0L);
    }

    /**
     * Retrieve a cache entry by exact prefix.
     *
     * @throws NotFoundException if the prefix does not exist or has expired
     * @throws McacheException   on transport or server errors
     */
    Item get(String prefix);

    /**
     * Update an existing cache entry.
     *
     * @param ttlSeconds new TTL in seconds; {@code 0} keeps the original expiry
     * @throws NotFoundException if the prefix does not exist
     * @throws McacheException   on transport or server errors
     */
    void update(String prefix, Object data, long ttlSeconds);

    /** Convenience overload that keeps the original TTL. */
    default void update(String prefix, Object data) {
        update(prefix, data, 0L);
    }

    /**
     * Delete a cache entry.
     *
     * @throws NotFoundException if the prefix does not exist
     * @throws McacheException   on transport or server errors
     */
    void delete(String prefix);

    /**
     * Return all direct child items under the given prefix path.
     *
     * @throws McacheException on transport or server errors
     */
    List<Item> listByPrefix(String prefix);

    /** Release underlying connections. */
    @Override
    void close();
}
