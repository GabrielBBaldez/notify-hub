package io.notifyhub.core.audience;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages named audiences and resolves them to lists of contacts.
 *
 * <pre>{@code
 * AudienceManager manager = new AudienceManager(contactRepository);
 * manager.createAudience("vips", Set.of("vip", "plan:premium"));
 *
 * List<Contact> contacts = manager.resolve("vips");
 * // Returns all contacts that have BOTH "vip" AND "plan:premium" tags
 * }</pre>
 */
public class AudienceManager {

    private final Map<String, Audience> audiences = new ConcurrentHashMap<>();
    private final ContactRepository contactRepository;

    public AudienceManager(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    public Audience createAudience(String name, Set<String> tags) {
        Audience audience = new Audience(name, tags);
        audiences.put(name, audience);
        return audience;
    }

    public Optional<Audience> getAudience(String name) {
        return Optional.ofNullable(audiences.get(name));
    }

    public boolean deleteAudience(String name) {
        return audiences.remove(name) != null;
    }

    public List<Audience> listAudiences() {
        return List.copyOf(audiences.values());
    }

    /**
     * Resolve an audience to the list of contacts matching ALL its tags.
     */
    public List<Contact> resolve(Audience audience) {
        return contactRepository.findByAllTags(audience.getTags());
    }

    /**
     * Resolve an audience by name. Throws if audience not found.
     */
    public List<Contact> resolve(String audienceName) {
        Audience audience = audiences.get(audienceName);
        if (audience == null) {
            throw new IllegalArgumentException("Audience not found: " + audienceName);
        }
        return resolve(audience);
    }

    public ContactRepository getContactRepository() {
        return contactRepository;
    }
}
