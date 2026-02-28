package io.notifyhub.core.audience;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository for CRUD operations on {@link Contact}s.
 * Provides tag-based querying for audience resolution.
 */
public interface ContactRepository {

    Contact save(Contact contact);

    Optional<Contact> findById(String id);

    List<Contact> findAll();

    boolean delete(String id);

    /** Find contacts that have the specified tag. */
    List<Contact> findByTag(String tag);

    /** Find contacts that have ALL the specified tags (AND logic). */
    List<Contact> findByAllTags(Set<String> tags);

    long count();

    void clear();
}
