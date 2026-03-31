package io.mcache.sdk.grpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.*;
import io.mcache.sdk.*;
import io.mcache.sdk.proto.Messages.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * gRPC-based mcache client.
 *
 * <pre>{@code
 * try (GrpcCacheClient c = new GrpcCacheClient("localhost", 9090)) {
 *     c.insert("user/name", "alice", 60);
 *     Item item = c.get("user/name");
 *     System.out.println(item.getData()); // alice
 * }
 * }</pre>
 */
public class GrpcCacheClient implements CacheClient {

    private final ManagedChannel channel;
    private final ObjectMapper mapper = new ObjectMapper();

    public GrpcCacheClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build());
    }

    /** Use a pre-built channel (e.g. for TLS or testing). */
    public GrpcCacheClient(ManagedChannel channel) {
        this.channel = channel;
    }

    // ------------------------------------------------------------------ //
    // CacheClient
    // ------------------------------------------------------------------ //

    @Override
    public void insert(String prefix, Object data, long ttlSeconds) {
        try {
            InsertRequest req = new InsertRequest();
            req.prefix = prefix; req.data = toJson(data); req.ttlSeconds = ttlSeconds;
            InsertResponse resp = invoke("/mcache.CacheService/Insert", req.toByteArray(),
                    InsertResponse::parseFrom, prefix);
            if (!resp.success) throw new McacheException("insert failed for prefix: " + prefix);
        } catch (IOException e) { throw new McacheException("failed to encode request", e); }
    }

    @Override
    public Item get(String prefix) {
        try {
            GetRequest req = new GetRequest(); req.prefix = prefix;
            return fromGetResponse(invoke("/mcache.CacheService/Get", req.toByteArray(),
                    GetResponse::parseFrom, prefix));
        } catch (IOException e) { throw new McacheException("failed to encode request", e); }
    }

    @Override
    public void update(String prefix, Object data, long ttlSeconds) {
        try {
            UpdateRequest req = new UpdateRequest();
            req.prefix = prefix; req.data = toJson(data); req.ttlSeconds = ttlSeconds;
            invoke("/mcache.CacheService/Update", req.toByteArray(), UpdateResponse::parseFrom, prefix);
        } catch (IOException e) { throw new McacheException("failed to encode request", e); }
    }

    @Override
    public void delete(String prefix) {
        try {
            DeleteRequest req = new DeleteRequest(); req.prefix = prefix;
            invoke("/mcache.CacheService/Delete", req.toByteArray(), DeleteResponse::parseFrom, prefix);
        } catch (IOException e) { throw new McacheException("failed to encode request", e); }
    }

    @Override
    public List<Item> listByPrefix(String prefix) {
        try {
            ListByPrefixRequest req = new ListByPrefixRequest(); req.prefix = prefix;
            ListByPrefixResponse resp = invoke("/mcache.CacheService/ListByPrefix", req.toByteArray(),
                    ListByPrefixResponse::parseFrom, prefix);
            List<Item> items = new ArrayList<>();
            for (GetResponse r : resp.items) items.add(fromGetResponse(r));
            return items;
        } catch (IOException e) { throw new McacheException("failed to encode request", e); }
    }

    @Override
    public void close() {
        channel.shutdown();
    }

    // ------------------------------------------------------------------ //
    // Helpers
    // ------------------------------------------------------------------ //

    @FunctionalInterface
    private interface Parser<T> {
        T parse(byte[] bytes) throws Exception;
    }

    private <T> T invoke(String fullMethod, byte[] reqBytes, Parser<T> parser, String prefix) {
        MethodDescriptor<byte[], byte[]> method = MethodDescriptor.<byte[], byte[]>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(fullMethod)
                .setRequestMarshaller(ByteMarshaller.INSTANCE)
                .setResponseMarshaller(ByteMarshaller.INSTANCE)
                .build();

        ClientCall<byte[], byte[]> call = channel.newCall(method, CallOptions.DEFAULT);
        byte[][] result = new byte[1][];
        StatusRuntimeException[] error = new StatusRuntimeException[1];
        Object lock = new Object();

        call.start(new ClientCall.Listener<byte[]>() {
            @Override public void onMessage(byte[] message) { result[0] = message; }
            @Override public void onClose(Status status, Metadata trailers) {
                if (!status.isOk()) {
                    error[0] = status.asRuntimeException(trailers);
                }
                synchronized (lock) { lock.notifyAll(); }
            }
        }, new Metadata());
        call.sendMessage(reqBytes);
        call.halfClose();
        call.request(1);

        synchronized (lock) {
            while (result[0] == null && error[0] == null) {
                try { lock.wait(10_000); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new McacheException("interrupted waiting for gRPC response");
                }
            }
        }

        if (error[0] != null) {
            Status.Code code = error[0].getStatus().getCode();
            if (code == Status.Code.NOT_FOUND) throw new NotFoundException(prefix);
            if (code == Status.Code.ALREADY_EXISTS) throw new AlreadyExistsException(prefix);
            throw new McacheException("grpc error " + code + ": " + error[0].getStatus().getDescription());
        }

        try {
            return parser.parse(result[0] != null ? result[0] : new byte[0]);
        } catch (Exception e) {
            throw new McacheException("failed to parse gRPC response", e);
        }
    }

    private byte[] toJson(Object data) {
        try {
            return mapper.writeValueAsBytes(data);
        } catch (IOException e) {
            throw new McacheException("failed to serialise data to JSON", e);
        }
    }

    private Item fromGetResponse(GetResponse r) {
        Object data;
        try {
            data = mapper.readValue(r.data, Object.class);
        } catch (IOException e) {
            data = new String(r.data);
        }
        Instant expireTime = r.expireTime > 0 ? Instant.ofEpochSecond(r.expireTime) : null;
        return new Item(
                r.prefix,
                data,
                Instant.ofEpochSecond(r.createdAt),
                Instant.ofEpochSecond(r.updatedAt),
                expireTime
        );
    }

    /** Raw byte marshaller for gRPC — passes bytes through unchanged. */
    private static final class ByteMarshaller implements MethodDescriptor.Marshaller<byte[]> {
        static final ByteMarshaller INSTANCE = new ByteMarshaller();

        @Override
        public InputStream stream(byte[] value) {
            return new ByteArrayInputStream(value);
        }

        @Override
        public byte[] parse(InputStream stream) {
            try {
                return stream.readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException("failed to read gRPC response bytes", e);
            }
        }
    }
}
