package io.mcache.sdk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/** A cache entry returned by the mcache server. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Item {

    private String prefix;
    private Object data;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant expireTime;

    public Item() {}

    public Item(String prefix, Object data, Instant createdAt, Instant updatedAt, Instant expireTime) {
        this.prefix = prefix;
        this.data = data;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.expireTime = expireTime;
    }

    @JsonProperty("prefix")
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }

    @JsonProperty("data")
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    @JsonProperty("createdAt")
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    /** Note: server serialises this field as "UpdatedAt" (capital U). */
    @JsonProperty("UpdatedAt")
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @JsonProperty("expireTime")
    public Instant getExpireTime() { return expireTime; }
    public void setExpireTime(Instant expireTime) { this.expireTime = expireTime; }

    @Override
    public String toString() {
        return "Item{prefix='" + prefix + "', data=" + data + '}';
    }
}
