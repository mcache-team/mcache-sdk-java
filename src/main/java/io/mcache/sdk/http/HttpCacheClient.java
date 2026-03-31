package io.mcache.sdk.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.mcache.sdk.*;
import okhttp3.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP-based mcache client using the REST API.
 *
 * <pre>{@code
 * try (HttpCacheClient c = new HttpCacheClient("http://localhost:8080")) {
 *     c.insert("user/name", "alice", 60);
 *     Item item = c.get("user/name");
 *     System.out.println(item.getData()); // alice
 * }
 * }</pre>
 */
public class HttpCacheClient implements CacheClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final String baseUrl;
    private final OkHttpClient http;
    private final ObjectMapper mapper;

    public HttpCacheClient(String baseUrl) {
        this(baseUrl, 10, TimeUnit.SECONDS);
    }

    public HttpCacheClient(String baseUrl, long timeout, TimeUnit unit) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.http = new OkHttpClient.Builder()
                .connectTimeout(timeout, unit)
                .readTimeout(timeout, unit)
                .writeTimeout(timeout, unit)
                .build();
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /** Package-private constructor for testing with a custom OkHttpClient. */
    HttpCacheClient(String baseUrl, OkHttpClient http) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.http = http;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ------------------------------------------------------------------ //
    // CacheClient
    // ------------------------------------------------------------------ //

    @Override
    public void insert(String prefix, Object data, long ttlSeconds) {
        Map<String, Object> body = new HashMap<>();
        body.put("prefix", prefix);
        body.put("data", data);
        if (ttlSeconds > 0) {
            body.put("timeout", ttlSeconds * 1_000_000_000L); // nanoseconds
        }
        try (Response resp = execute(new Request.Builder()
                .url(baseUrl + "/v1/data")
                .put(toBody(body))
                .build())) {
            if (resp.code() == 201) return;
            if (resp.code() == 409) throw new AlreadyExistsException(prefix);
            throwOnError(resp, prefix);
        }
    }

    @Override
    public Item get(String prefix) {
        try (Response resp = execute(new Request.Builder()
                .url(baseUrl + "/v1/data/" + encode(prefix))
                .get()
                .build())) {
            if (resp.code() == 404) throw new NotFoundException(prefix);
            throwOnError(resp, prefix);
            return parseBody(resp, Item.class);
        }
    }

    @Override
    public void update(String prefix, Object data, long ttlSeconds) {
        Map<String, Object> body = new HashMap<>();
        body.put("data", data);
        if (ttlSeconds > 0) {
            body.put("timeout", ttlSeconds * 1_000_000_000L);
        }
        try (Response resp = execute(new Request.Builder()
                .url(baseUrl + "/v1/data/" + encode(prefix))
                .post(toBody(body))
                .build())) {
            if (resp.code() == 200) return;
            if (resp.code() == 404) throw new NotFoundException(prefix);
            throwOnError(resp, prefix);
        }
    }

    @Override
    public void delete(String prefix) {
        try (Response resp = execute(new Request.Builder()
                .url(baseUrl + "/v1/data/" + encode(prefix))
                .delete()
                .build())) {
            if (resp.code() == 200) return;
            if (resp.code() == 404) throw new NotFoundException(prefix);
            throwOnError(resp, prefix);
        }
    }

    @Override
    public List<Item> listByPrefix(String prefix) {
        String url = baseUrl + "/v1/data/listByPrefix?prefix=" + encode(prefix);
        try (Response resp = execute(new Request.Builder().url(url).get().build())) {
            throwOnError(resp, prefix);
            String body = bodyString(resp);
            if (body == null || body.isBlank() || body.equals("null")) {
                return Collections.emptyList();
            }
            List<Item> items = mapper.readValue(body, new TypeReference<List<Item>>() {});
            return items != null ? items : Collections.emptyList();
        } catch (IOException e) {
            throw new McacheException("failed to parse listByPrefix response", e);
        }
    }

    @Override
    public void close() {
        http.dispatcher().executorService().shutdown();
        http.connectionPool().evictAll();
    }

    // ------------------------------------------------------------------ //
    // Helpers
    // ------------------------------------------------------------------ //

    private Response execute(Request req) {
        try {
            return http.newCall(req).execute();
        } catch (IOException e) {
            throw new McacheException("http request failed: " + e.getMessage(), e);
        }
    }

    private RequestBody toBody(Object obj) {
        try {
            return RequestBody.create(mapper.writeValueAsBytes(obj), JSON);
        } catch (IOException e) {
            throw new McacheException("failed to serialise request body", e);
        }
    }

    private <T> T parseBody(Response resp, Class<T> type) {
        try {
            String s = bodyString(resp);
            if (s == null) throw new McacheException("empty response body");
            return mapper.readValue(s, type);
        } catch (IOException e) {
            throw new McacheException("failed to parse response", e);
        }
    }

    private static String bodyString(Response resp) {
        try {
            ResponseBody b = resp.body();
            return b != null ? b.string() : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static void throwOnError(Response resp, String prefix) {
        if (resp.isSuccessful()) return;
        String msg = bodyString(resp);
        throw new McacheException("mcache http " + resp.code() + ": " + msg);
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
