package com.widdo.nexus.core.context;

import com.widdo.nexus.core.persistence.NexusPersistenceStrategy;

import java.util.Set;

/**
 * Widdo Nexus 框架通用上下文接口
 * 提供组件注册、获取和持久化功能
 */
public interface NexusContext {
    
    // 组件注册与管理
    <T> void registerComponent(String name, T component);
    <T> void registerComponent(Class<T> type, T component);
    <T> T getComponent(String name, Class<T> type);
    <T> T getComponent(Class<T> type);
    boolean containsComponent(String name);
    boolean containsComponent(Class<?> type);
    Set<String> getComponentNames();
    void removeComponent(String name);
    void removeComponent(Class<?> type);
    
    // 持久化策略管理
    void registerPersistenceStrategy(String name, NexusPersistenceStrategy strategy);
    NexusPersistenceStrategy getPersistenceStrategy(String name);
    Set<String> getPersistenceStrategyNames();
    
    // 通用持久化操作
    <T> void persist(String strategyName, String key, T value);
    <T> T retrieve(String strategyName, String key, Class<T> type);
    boolean exists(String strategyName, String key);
    void remove(String strategyName, String key);
    
    // 上下文生命周期
    void initialize();
    void shutdown();
    boolean isInitialized();
}