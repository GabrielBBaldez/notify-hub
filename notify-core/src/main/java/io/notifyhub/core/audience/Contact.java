package io.notifyhub.core.audience;

import io.notifyhub.core.Notifiable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A contact represents a person/recipient with tags and metadata.
 * Implements {@link Notifiable} so it can be used directly with NotifyHub.
 *
 * <pre>{@code
 * Contact contact = Contact.builder()
 *     .name("Alice")
 *     .email("alice@example.com")
 *     .phone("+5548999999999")
 *     .tag("vip")
 *     .tag("plan:premium")
 *     .metadata("company", "Acme")
 *     .build();
 * }</pre>
 */
public class Contact implements Notifiable {

    private final String id;
    private String name;
    private String email;
    private String phone;
    private String pushToken;
    private final Set<String> tags;
    private final Map<String, String> metadata;

    private Contact(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.name = builder.name;
        this.email = builder.email;
        this.phone = builder.phone;
        this.pushToken = builder.pushToken;
        this.tags = new LinkedHashSet<>(builder.tags);
        this.metadata = new LinkedHashMap<>(builder.metadata);
    }

    // ===================== GETTERS =====================

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPushToken() { return pushToken; }
    public Set<String> getTags() { return Collections.unmodifiableSet(tags); }
    public Map<String, String> getMetadata() { return Collections.unmodifiableMap(metadata); }

    // ===================== SETTERS =====================

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setPushToken(String pushToken) { this.pushToken = pushToken; }

    // ===================== TAG OPERATIONS =====================

    public void addTag(String tag) { this.tags.add(tag); }
    public void removeTag(String tag) { this.tags.remove(tag); }
    public boolean hasTag(String tag) { return this.tags.contains(tag); }

    public boolean hasAllTags(Set<String> requiredTags) {
        return this.tags.containsAll(requiredTags);
    }

    // ===================== METADATA OPERATIONS =====================

    public void putMetadata(String key, String value) { this.metadata.put(key, value); }
    public String getMetadataValue(String key) { return this.metadata.get(key); }

    // ===================== NOTIFIABLE =====================

    @Override public String getNotifyEmail() { return email; }
    @Override public String getNotifyPhone() { return phone; }
    @Override public String getNotifyPushToken() { return pushToken; }
    @Override public String getNotifyName() { return name; }

    // ===================== BUILDER =====================

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String name;
        private String email;
        private String phone;
        private String pushToken;
        private final Set<String> tags = new LinkedHashSet<>();
        private final Map<String, String> metadata = new LinkedHashMap<>();

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder phone(String phone) { this.phone = phone; return this; }
        public Builder pushToken(String pushToken) { this.pushToken = pushToken; return this; }
        public Builder tag(String tag) { this.tags.add(tag); return this; }
        public Builder tags(Set<String> tags) { this.tags.addAll(tags); return this; }
        public Builder metadata(String key, String value) { this.metadata.put(key, value); return this; }

        public Contact build() { return new Contact(this); }
    }
}
