package com.widdo.nexus.neo4j.adapter;

import com.widdo.nexus.core.adapter.NexusGraphAdapter;
import com.widdo.nexus.core.enums.NexusDatabaseFeature;
import com.widdo.nexus.core.meta.NexusDatabaseMetadata;
import com.widdo.nexus.core.meta.NexusNeo4jDatabaseMetadata;
import com.widdo.nexus.core.result.NexusGraphResultSet;
import com.widdo.nexus.core.result.NexusResult;
import com.widdo.nexus.core.result.NexusResultInterface;
import com.widdo.nexus.core.result.ResultEnum;
import com.widdo.nexus.core.result.entity.Value;
import com.widdo.nexus.core.transaction.NexusTransaction;
import com.widdo.nexus.neo4j.NexusNeo4jResultSet;
import com.widdo.nexus.neo4j.constant.Neo4jConstants;
import com.widdo.nexus.neo4j.util.Neo4jUtil;
import com.widdo.nexus.neo4j.util.ResultUtil;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.springframework.boot.actuate.health.Health;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * NexusNeo4jAdapter.
 *
 * @author XYL
 * @date 2025/08/27 17:11
 * @since 0.0.1-SNAPSHOT
 */
public class NexusNeo4jAdapter implements NexusGraphAdapter {

    private final Driver driver;

    private final String database;

    public NexusNeo4jAdapter(String uri, String username, String password, String database) {
        this.driver = GraphDatabase.driver(uri,
                AuthTokens.basic(username, password),
                Config.builder()
                        .withMaxConnectionPoolSize(100)
                        .withConnectionAcquisitionTimeout(30, TimeUnit.SECONDS)
                        .build());
        this.database = database;
    }

    @Override
    public String getDatabaseType() {
        return "NEO4j";
    }

    @Override
    public com.widdo.nexus.core.result.Result<?> toExecute(String model, String query, Map<String, Object> parameters) {

        try (Session session = driver.session(SessionConfig.builder().withDatabase(database).withDefaultAccessMode(AccessMode.WRITE).build())) {
            if (Neo4jConstants.RUNNER_READ.equals(model)) {
                return session.executeRead(tx -> {
                    Result rs = parameters == null ? tx.run(query) : tx.run(query, parameters);
                    return Neo4jUtil.packResult(rs);
                });
            }
            return session.executeWrite(tx -> {
                Result rs = parameters == null ? tx.run(query) : tx.run(query, parameters);
                return Neo4jUtil.packResult(rs);
            });

        } catch (ServiceUnavailableException exception) {
            return ResultUtil.error(ResultEnum.ERROR, 10010, exception.getMessage());
        } catch (org.neo4j.driver.exceptions.ClientException exception) {
            System.out.println("--error by ClientException");
            return ResultUtil.error(ResultEnum.ERROR, exception.getMessage());
        }
    }

    @Override
    public NexusGraphResultSet executeQuery(String query, Map<String, Object> parameters) {
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {
            return session.executeRead(tx -> {
                Result result = parameters == null ? tx.run(query) : tx.run(query, parameters);
                return new NexusNeo4jResultSet(result.keys(), result.list());
            });
        }
    }

    @Override
    public void executeCommand(String command, Map<String, Object> parameters) {
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {
            session.run(command, parameters);
        }
    }

    @Override
    public NexusTransaction beginTransaction() {
        return null;
    }

    @Override
    public void close() {
        if (driver != null) {
            driver.close();
        }
    }

    @Override
    public boolean supportsFeature(NexusDatabaseFeature feature) {
        return false;
    }

    @Override
    public NexusDatabaseMetadata getMetadata() {
        final NexusNeo4jDatabaseMetadata databaseMetadata = new NexusNeo4jDatabaseMetadata();
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {

            //获取数据库信息
            final NexusResult dbInfo = session.executeRead(tx -> {
                Result result = tx.run("CALL dbms.components()\n" +
                        "YIELD name, versions, edition\n" +
                        "RETURN name, versions, edition;");

                final com.widdo.nexus.core.result.Result<List<Map<String, Value>>> originResult = Neo4jUtil.originResult(new NexusNeo4jResultSet(result.keys(), result.list()));
                //解析结果
                return NexusResultInterface.NEO4j.ALL.wrapper(originResult);

            });

            // 获取节点和关系数量
            final NexusResult countInfo = session.executeRead(tx -> {
                Result result = tx.run("CALL apoc.meta.stats()\n" +
                        "YIELD labelCount, relTypeCount, nodeCount, relCount, stats \n" +
                        "RETURN labelCount, relTypeCount, nodeCount, relCount, stats");

                final com.widdo.nexus.core.result.Result<List<Map<String, Value>>> originResult = Neo4jUtil.originResult(new NexusNeo4jResultSet(result.keys(), result.list()));
                //解析结果
                return NexusResultInterface.NEO4j.ALL.wrapper(originResult);
            });

            databaseMetadata.setVersion(dbInfo.getOrDefault("versions", "").toString());

            databaseMetadata.setStats(countInfo);

        } catch (Exception e) {
            // 记录错误但不抛出异常
            System.err.println("Error retrieving metadata: " + e.getMessage());
        }

        return databaseMetadata;
    }

    @Override
    public Health healthCheck() {
        Health.Builder healthBuilder = new Health.Builder();

        try (Session session = driver.session(SessionConfig.forDatabase(database))) {
            // 执行简单的查询来测试连接状态
            Result result = session.run("RETURN 1 AS connection_test");

            if (result.hasNext()) {
                Record record = result.next();
                int testValue = record.get("connection_test").asInt();

                if (testValue == 1) {
                    // 连接正常
                    healthBuilder.up()
                            .withDetail("database", database)
                            .withDetail("status", "connected")
                            .withDetail("message", "Successfully connected to Neo4j database");

                    // 获取数据库统计信息
                    Result statsResult = session.run("CALL db.stats()");
                    if (statsResult.hasNext()) {
                        Record stats = statsResult.next();
                        healthBuilder.withDetail("nodes", stats.get("nodes").asInt())
                                .withDetail("relationships", stats.get("relationships").asInt())
                                .withDetail("labels", stats.get("labels").asInt());
                    }
                } else {
                    // 连接测试失败
                    healthBuilder.down()
                            .withDetail("error", "Connection test failed")
                            .withDetail("test_value", testValue);
                }
            } else {
                // 没有返回结果
                healthBuilder.down()
                        .withDetail("error", "No results returned from connection test");
            }
        } catch (Exception e) {
            // 连接异常
            healthBuilder.down()
                    .withDetail("error", "Failed to connect to Neo4j database")
                    .withException(e);
        }

        return healthBuilder.build();
    }

    @Override
    public <T> T postExecute(NexusGraphResultSet resultSet, Class<T> resultType) {

        final com.widdo.nexus.core.result.Result<List<Map<String, Value>>> result = Neo4jUtil.originResult(resultSet);

        //数据转换，需要把袁术结果转化成Map或者List<Map>

        if (resultType == List.class) {
            // 处理泛型类型
//            final List<?> objects = entityMapper.mapToEntities(results, getListElementType());
//            return (T) entityMapper.mapToEntities(results, getListElementType());
            return (T) NexusResultInterface.NEO4j.ALL.wrapper(result);
        }

//        return entityMapper.mapToEntity(results.get(0), resultType);
        return (T) NexusResultInterface.NEO4j.ALL.wrapper(result);
    }

    @Override
    public Class<?> getListElementType() {
        return Map.class;
    }
}
