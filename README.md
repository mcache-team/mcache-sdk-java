# mcache-sdk-java

Java SDK for [mcache](https://github.com/mcache-team/mcache) — supports both HTTP and gRPC transports.

## Requirements

- Java 11+
- Maven 3.8+

## Installation

Add to your `pom.xml`:

```xml
<dependency>
  <groupId>io.mcache</groupId>
  <artifactId>mcache-sdk</artifactId>
  <version>0.1.0</version>
</dependency>
```

Or build from source:

```bash
mvn install -DskipTests
```

## Quick Start

### HTTP Client

```java
import io.mcache.sdk.Item;
import io.mcache.sdk.http.HttpCacheClient;

try (HttpCacheClient client = new HttpCacheClient("http://localhost:8080")) {

    // Insert with 60-second TTL
    client.insert("user/profile/name", "alice", 60);

    // Insert without TTL (no expiry)
    client.insert("config/debug", true);

    // Get by exact prefix
    Item item = client.get("user/profile/name");
    System.out.println(item.getData());       // alice
    System.out.println(item.getCreatedAt());  // 2026-...

    // Update
    client.update("user/profile/name", "bob");

    // List all children under a path
    List<Item> items = client.listByPrefix("user/profile");
    items.forEach(i -> System.out.println(i.getPrefix() + " = " + i.getData()));

    // Delete
    client.delete("user/profile/name");
}
```

### gRPC Client

```java
import io.mcache.sdk.Item;
import io.mcache.sdk.grpc.GrpcCacheClient;

try (GrpcCacheClient client = new GrpcCacheClient("localhost", 9090)) {

    client.insert("user/profile/name", "alice", 300);

    Item item = client.get("user/profile/name");
    System.out.println(item.getData()); // alice
}
```

## API Reference

Both `HttpCacheClient` and `GrpcCacheClient` implement the `CacheClient` interface:

| Method | Description |
|---|---|
| `insert(prefix, data, ttlSeconds)` | Create a cache entry. Throws `AlreadyExistsException` if prefix exists. |
| `insert(prefix, data)` | Create with no TTL (convenience overload). |
| `get(prefix)` | Get entry by exact prefix. Throws `NotFoundException` if missing or expired. |
| `update(prefix, data, ttlSeconds)` | Update existing entry. Throws `NotFoundException` if missing. |
| `update(prefix, data)` | Update keeping original TTL (convenience overload). |
| `delete(prefix)` | Delete entry. Throws `NotFoundException` if missing. |
| `listByPrefix(prefix)` | List all direct children under a prefix path. |
| `close()` | Release connections. Called automatically via try-with-resources. |

### Exceptions

| Exception | When |
|---|---|
| `NotFoundException` | Prefix not found or expired |
| `AlreadyExistsException` | Duplicate insert |
| `McacheException` | Other server or transport errors (base class) |

### Item fields

| Field | Type | Description |
|---|---|---|
| `getPrefix()` | `String` | The full key path |
| `getData()` | `Object` | JSON-deserialized value |
| `getCreatedAt()` | `Instant` | Creation timestamp |
| `getUpdatedAt()` | `Instant` | Last update timestamp |
| `getExpireTime()` | `Instant` (nullable) | Expiry time, `null` if no TTL |

## Project Structure

```
src/main/java/io/mcache/sdk/
  CacheClient.java              # Unified interface
  Item.java                     # Cache entry model
  McacheException.java          # Base exception
  NotFoundException.java
  AlreadyExistsException.java
  http/
    HttpCacheClient.java        # HTTP REST client (OkHttp + Jackson)
  grpc/
    GrpcCacheClient.java        # gRPC client (grpc-java, no protoc needed)
  proto/
    Messages.java               # Hand-written proto3 wire-format codecs
```
