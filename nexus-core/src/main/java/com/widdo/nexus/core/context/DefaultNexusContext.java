package com.widdo.nexus.core.context;

import com.widdo.nexus.core.log.NexusLogger;
import com.widdo.nexus.core.persistence.InMemoryPersistenceStrategy;
import com.widdo.nexus.core.persistence.NexusPersistenceStrategy;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultNexusContext implements NexusContext, InitializingBean, DisposableBean {
    
    private final Map<String, Object> components = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object> typedComponents = new ConcurrentHashMap<>();
    private final Map<String, NexusPersistenceStrategy> persistenceStrategies = new ConcurrentHashMap<>();
    
    private final NexusLogger logger = NexusLogger.getLogger(getClass());
    private volatile boolean initialized = false;
    
    // 组件注册与管理
    @Override
    public <T> void registerComponent(String name, T component) {
        components.put(name, component);
        logger.debug("Registered component '{}' of type {}", name, component.getClass().getName());
    }
    
    @Override
    public <T> void registerComponent(Class<T> type, T component) {
        typedComponents.put(type, component);
        logger.debug("Registered component of type {}", type.getName());
    }
    
    @Override
    public <T> T getComponent(String name, Class<T> type) {
        Object component = components.get(name);
        if (component != null && type.isInstance(component)) {
            return type.cast(component);
        }
        throw new ComponentNotFoundException("Component not found: " + name + " of type " + type.getName());
    }
    
    @Override
    public <T> T getComponent(Class<T> type) {
        Object component = typedComponents.get(type);
        if (component != null && type.isInstance(component)) {
            return type.cast(component);
        }
        throw new ComponentNotFoundException("Component not found of type: " + type.getName());
    }
    
    @Override
    public boolean containsComponent(String name) {
        return components.containsKey(name);
    }
    
    @Override
    public boolean containsComponent(Class<?> type) {
        return typedComponents.containsKey(type);
    }
    
    @Override
    public Set<String> getComponentNames() {
        return Collections.unmodifiableSet(components.keySet());
    }
    
    @Override
    public void removeComponent(String name) {
        components.remove(name);
        logger.debug("Removed component '{}'", name);
    }
    
    @Override
    public void removeComponent(Class<?> type) {
        typedComponents.remove(type);
        logger.debug("Removed component of type {}", type.getName());
    }
    
    // 持久化策略管理
    @Override
    public void registerPersistenceStrategy(String name, NexusPersistenceStrategy strategy) {
        persistenceStrategies.put(name, strategy);
        logger.debug("Registered persistence strategy '{}'", name);
    }
    
    @Override
    public NexusPersistenceStrategy getPersistenceStrategy(String name) {
        NexusPersistenceStrategy strategy = persistenceStrategies.get(name);
        if (strategy == null) {
            throw new PersistenceStrategyNotFoundException("Persistence strategy not found: " + name);
        }
        return strategy;
    }
    
    @Override
    public Set<String> getPersistenceStrategyNames() {
        return Collections.unmodifiableSet(persistenceStrategies.keySet());
    }
    
    // 通用持久化操作
    @Override
    public <T> void persist(String strategyName, String key, T value) {
        NexusPersistenceStrategy strategy = getPersistenceStrategy(strategyName);
        strategy.save(key, value);
        logger.debug("Persisted data with key '{}' using strategy '{}'", key, strategyName);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T retrieve(String strategyName, String key, Class<T> type) {
        NexusPersistenceStrategy strategy = getPersistenceStrategy(strategyName);
        Object value = strategy.load(key);
        if (value != null && type.isInstance(value)) {
            logger.debug("Retrieved data with key '{}' using strategy '{}'", key, strategyName);
            return (T) value;
        }
        logger.debug("Data not found with key '{}' using strategy '{}'", key, strategyName);
        return null;
    }
    
    @Override
    public boolean exists(String strategyName, String key) {
        NexusPersistenceStrategy strategy = getPersistenceStrategy(strategyName);
        return strategy.exists(key);
    }
    
    @Override
    public void remove(String strategyName, String key) {
        NexusPersistenceStrategy strategy = getPersistenceStrategy(strategyName);
        strategy.delete(key);
        logger.debug("Removed data with key '{}' using strategy '{}'", key, strategyName);
    }
    
    // 上下文生命周期
    @Override
    public void initialize() {
        if (!initialized) {
            logger.info("Initializing Nexus Context");
            // 初始化默认持久化策略
            registerPersistenceStrategy("memory", new InMemoryPersistenceStrategy());
            initialized = true;
            logger.info("Nexus Context initialized successfully");
        }
    }
    
    @Override
    public void shutdown() {
        if (initialized) {
            logger.info("Shutting down Nexus Context");
            // 清理资源
            components.clear();
            typedComponents.clear();
            persistenceStrategies.clear();
            initialized = false;
            logger.info("Nexus Context shut down successfully");
        }
    }
    
    @Override
    public boolean isInitialized() {
        return initialized;
    }
    
    // Spring生命周期集成
    @Override
    public void afterPropertiesSet() throws Exception {
        initialize();
    }
    
    @Override
    public void destroy() throws Exception {
        shutdown();
    }
    
    // 异常类
    public static class ComponentNotFoundException extends RuntimeException {
        public ComponentNotFoundException(String message) {
            super(message);
        }
    }
    
    public static class PersistenceStrategyNotFoundException extends RuntimeException {
        public PersistenceStrategyNotFoundException(String message) {
            super(message);
        }
    }
}