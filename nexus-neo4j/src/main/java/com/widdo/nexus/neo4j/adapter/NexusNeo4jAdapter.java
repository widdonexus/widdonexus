package com.widdo.nexus.neo4j.adapter;

import com.widdo.nexus.core.adapter.NexusDatabaseAdapter;
import com.widdo.nexus.core.entity.NexusEntityMapper;
import com.widdo.nexus.core.enums.NexusDatabaseFeature;
import com.widdo.nexus.core.health.Health;
import com.widdo.nexus.core.meta.NexusDatabaseMetadata;
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
import org.neo4j.driver.exceptions.ServiceUnavailableException;

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
public class NexusNeo4jAdapter implements NexusDatabaseAdapter {

    private final Driver driver;

    private final String database;

    private NexusEntityMapper entityMapper;

    public NexusNeo4jAdapter(String uri, String username, String password, String database, NexusEntityMapper entityMapper) {
        this.driver = GraphDatabase.driver(uri,
                AuthTokens.basic(username, password),
                Config.builder()
                        .withMaxConnectionPoolSize(100)
                        .withConnectionAcquisitionTimeout(30, TimeUnit.SECONDS)
                        .build());
        this.database = database;
        this.entityMapper = entityMapper;
    }

    @Override
    public String getDatabaseType() {
        return "";
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

    }

    @Override
    public NexusTransaction beginTransaction() {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public boolean supportsFeature(NexusDatabaseFeature feature) {
        return false;
    }

    @Override
    public NexusDatabaseMetadata getMetadata() {
        return null;
    }

    @Override
    public Health healthCheck() {
        return null;
    }

    @Override
    public <T> T postExecute(NexusGraphResultSet resultSet, Class<T> resultType) {

        final com.widdo.nexus.core.result.Result<List<Map<String, Value>>> result = Neo4jUtil.originResult(resultSet);

        //数据转换，需要把袁术结果转化成Map或者List<Map>

        if (resultType == List.class) {
            // 处理泛型类型
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
