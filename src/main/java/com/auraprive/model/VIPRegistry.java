package com.auraprive.model;

import com.auraprive.exception.ExclusiveAccessException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generic type-safe container managing collections of SecuredEntity implementations.
 */
public class VIPRegistry<T extends SecuredEntity> {
    private final List<T> registryList = new ArrayList<>();

    public synchronized void register(T entity) throws ExclusiveAccessException {
        if (entity == null || !entity.verifyAccess()) {
            throw new ExclusiveAccessException("Entity rejected by VIPRegistry: Invalid security clearance contract.");
        }
        if (entity instanceof Event) {
            ((Event) entity).validatePayload();
        }
        registryList.add(entity);
    }

    public synchronized List<T> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(registryList));
    }

    public synchronized int count() {
        return registryList.size();
    }
}