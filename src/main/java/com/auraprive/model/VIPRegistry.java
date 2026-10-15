package com.auraprive.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generic type-safe container managing collections of SecuredEntity implementations.
 */
public class VIPRegistry<T extends SecuredEntity> {
    private final List<T> registryList = new ArrayList<>();

    public synchronized void register(T entity) {
        if (entity != null && entity.verifyAccess()) {
            registryList.add(entity);
        }
    }

    public synchronized List<T> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(registryList));
    }

    public synchronized int count() {
        return registryList.size();
    }
}