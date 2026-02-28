package io.notifyhub.core.audience;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe in-memory implementation of {@link ContactRepository}.
 * Suitable for development and testing.
 */
public class InMemoryContactRepository implements ContactRepository {

    private final Map<String, Contact> contacts = new ConcurrentHashMap<>();

    @Override
    public Contact save(Contact contact) {
        contacts.put(contact.getId(), contact);
        return contact;
    }

    @Override
    public Optional<Contact> findById(String id) {
        return Optional.ofNullable(contacts.get(id));
    }

    @Override
    public List<Contact> findAll() {
        return List.copyOf(contacts.values());
    }

    @Override
    public boolean delete(String id) {
        return contacts.remove(id) != null;
    }

    @Override
    public List<Contact> findByTag(String tag) {
        return contacts.values().stream()
                .filter(c -> c.hasTag(tag))
                .collect(Collectors.toList());
    }

    @Override
    public List<Contact> findByAllTags(Set<String> tags) {
        if (tags == null || tags.isEmpty()) return findAll();
        return contacts.values().stream()
                .filter(c -> c.hasAllTags(tags))
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return contacts.size();
    }

    @Override
    public void clear() {
        contacts.clear();
    }
}
