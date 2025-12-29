package com.widdo.nexus.core.query;

import com.widdo.nexus.core.annotation.Query;
import com.widdo.nexus.core.annotation.QueryRef;
import com.widdo.nexus.core.exception.NexusExceptionHelper;
import com.widdo.nexus.core.log.LogMessages;
import com.widdo.nexus.core.log.NexusLogger;
import com.widdo.nexus.core.util.ClassScanner;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;

/**
 * NexusQueryLoader.
 * <p>
 * 查询加载器，支持从YAML文件和注解加载Cypher查询
 *
 * @author XYL
 * @date 2025/08/27 18:25
 * @since 0.0.1-SNAPSHOT
 */
public class NexusQueryLoader {

    private final NexusLogger logger = NexusLogger.getLogger(getClass());

    private final Map<String, String> queries = new HashMap<>();

    private final Map<String, String> queryDescription = new HashMap<>();
    private final Set<String> scannedPackages = new HashSet<>();

    /**
     * 从YAML文件加载查询
     *
     * @param filePath YAML文件路径（classpath相对路径）
     */
    public void loadFromYaml(String filePath) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath)) {
            if (inputStream == null) {
                logger.warn("Query file not found, skipping: " + filePath);
                return;
            }

            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(inputStream);

            // 支持多种YAML结构
            if (data.containsKey("queries")) {
                // 格式1: queries作为顶级键
                @SuppressWarnings("unchecked")
                Map<String, Map<String, String>> queryMap = (Map<String, Map<String, String>>) data.get("queries");

                queryMap.entrySet().forEach(entry -> {
                    final String queryId = entry.getKey();
                    final Map<String, String> value = entry.getValue();
                    final String wnql = value.getOrDefault("wnql", "");
                    final String description = value.getOrDefault("description", "");
                    queries.put(queryId, wnql);
                    queryDescription.put(queryId, description);
                });
            } else {
                //TODO：计划删除 格式2: 直接是查询ID到查询语句的映射
                data.forEach((key, value) -> queries.put(key, value.toString()));
            }

            logger.info(LogMessages.QUERY_LOADER_START, filePath);

            System.out.println("Loaded " + queries.size() + " queries from " + filePath);
        } catch (Exception e) {
            throw NexusExceptionHelper.queryError("Failed to load queries from YAML file: " + filePath, e);
        }
    }

    /**
     * 从多个YAML文件加载查询
     *
     * @param filePaths YAML文件路径列表
     */
    public void loadFromYaml(List<String> filePaths) {
        for (String filePath : filePaths) {
            loadFromYaml(filePath);
        }
    }

    /**
     * 扫描指定包中的类，查找带有@Query注解的方法
     *
     * @param packageNames 要扫描的包名列表
     */
    public void scanAnnotations(String... packageNames) {
        for (String packageName : packageNames) {
            if (scannedPackages.contains(packageName)) {
                continue; // 避免重复扫描
            }

            try {
                Set<Class<?>> classes = ClassScanner.findClasses(packageName);
                for (Class<?> clazz : classes) {
                    if (clazz.isInterface()) {
                        processInterface(clazz);
                    }
                }

                scannedPackages.add(packageName);
                System.out.println("Scanned " + classes.size() + " classes in package: " + packageName);
            } catch (Exception e) {
                throw NexusExceptionHelper.queryError("Failed to scan package: " + packageName, e);
            }
        }
    }

    /**
     * 处理接口，提取@Query注解
     */
    private void processInterface(Class<?> interfaceClass) {
        for (Method method : interfaceClass.getDeclaredMethods()) {
            Query queryAnnotation = method.getAnnotation(Query.class);
            if (queryAnnotation != null) {
                String queryId = generateQueryId(interfaceClass, method);
                queries.put(queryId, queryAnnotation.value());
                queryDescription.put(queryId, queryAnnotation.description());
            }

            QueryRef queryRefAnnotation = method.getAnnotation(QueryRef.class);
            if (queryRefAnnotation != null) {
                // 查询引用，需要与YAML中定义的查询ID关联
                String queryId = queryRefAnnotation.value();
                if (!queries.containsKey(queryId)) {
                    throw NexusExceptionHelper.queryError("Query reference not found: " + queryId + " in method: " + method.getName());
                }
                // 不需要额外处理，查询执行时会通过getQuery方法获取
            }
        }
    }

    /**
     * 生成查询ID：类全限定名 + 方法名
     */
    private String generateQueryId(Class<?> clazz, Method method) {
        return clazz.getName() + "." + method.getName();
    }

    public String getDescription(String queryId) {
        return queryDescription.get("queryId");
    }

    /**
     * 获取查询语句
     *
     * @param queryId 查询ID
     * @return Cypher查询语句
     */
    public String getQuery(String queryId) {
        if (!queries.containsKey(queryId)) {
            throw NexusExceptionHelper.queryError("Query not found: " + queryId)
                    .withQueryId(Long.parseLong(queryId));
        }
        return queries.get(queryId);
    }

    /**
     * 获取查询语句，如果不存在则返回默认值
     *
     * @param queryId      查询ID
     * @param defaultValue 默认查询语句
     * @return Cypher查询语句
     */
    public String getQuery(String queryId, String defaultValue) {
        return queries.getOrDefault(queryId, defaultValue);
    }

    /**
     * 添加查询语句
     *
     * @param queryId 查询ID
     * @param query   Cypher查询语句
     */
    public void addQuery(String queryId, String query) {
        queries.put(queryId, query);
    }

    /**
     * 获取所有查询ID
     *
     * @return 查询ID集合
     */
    public Set<String> getAllQueryIds() {
        return Collections.unmodifiableSet(queries.keySet());
    }

    /**
     * 清空所有查询
     */
    public void clear() {
        queries.clear();
        scannedPackages.clear();
    }
}
