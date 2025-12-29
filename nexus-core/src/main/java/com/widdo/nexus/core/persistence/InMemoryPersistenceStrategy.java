package com.widdo.nexus.core.persistence;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// 内存持久化策略
public class InMemoryPersistenceStrategy implements NexusPersistenceStrategy {
    private final Map<String, Object> storage = new ConcurrentHashMap<>();

    @Override
    public void save(String key, Object value) {
        storage.put(key, value);
    }

    @Override
    public Object load(String key) {
        return storage.get(key);
    }

    @Override
    public void delete(String key) {
        storage.remove(key);
    }

    @Override
    public boolean exists(String key) {
        return storage.containsKey(key);
    }

    @Override
    public Set<String> keys() {
        return Collections.unmodifiableSet(storage.keySet());
    }

    @Override
    public long size() {
        return storage.size();
    }

    @Override
    public void clear() {
        storage.clear();
    }
}