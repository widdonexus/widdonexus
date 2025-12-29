package com.widdo.nexus.examples.controller;

import com.widdo.nexus.core.result.IResultInterface;
import com.widdo.nexus.core.result.NexusResult;
import com.widdo.nexus.core.result.entity.Value;
import com.widdo.nexus.core.support.query.NexusQuery;
import com.widdo.nexus.core.support.query.NexusQueryBuilder;
import com.widdo.nexus.hadoop.templete.NexusHdfsAdvancedTemplate;
import com.widdo.nexus.neo4j.template.NexusNeo4jAdvancedTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * NexusExampleController.
 *
 * @author XYL
 * @date 2025/08/29 12:50
 * @since 0.0.1-SNAPSHOT
 */
@RequestMapping(value = "/nexus/example")
@RestController
public class NexusExampleController {

    private final NexusNeo4jAdvancedTemplate nexusNeo4jAdvancedTemplate;

    private final NexusHdfsAdvancedTemplate nexusHdfsAdvancedTemplate;

    @Autowired
    public NexusExampleController(NexusNeo4jAdvancedTemplate nexusNeo4jAdvancedTemplate,
                                  NexusHdfsAdvancedTemplate nexusHdfsAdvancedTemplate) {
        this.nexusNeo4jAdvancedTemplate = nexusNeo4jAdvancedTemplate;
        this.nexusHdfsAdvancedTemplate = nexusHdfsAdvancedTemplate;
    }

    /**
     * Nexus 函数式查询语言 wnql
     *
     * @return com.widdo.nexus.core.result.NexusResult
     * @author XYL
     * @date 2025/08/30 17:45:32
     */
    @GetMapping(value = "/function/api")
    public NexusResult function() {

        final NexusQuery query = NexusQueryBuilder.create()
                .match(m -> m.node("employee", "e"))
                .where(w -> w.eq("e.name", "小红"))
                .returning(r -> r.returning("e"))
                .build();

        return nexusNeo4jAdvancedTemplate.execute(query, NexusResult.class);
    }

    /**
     * Nexus wnql查询
     *
     * @param params params
     * @return com.widdo.nexus.core.result.NexusResult
     * @author XYL
     * @date 2025/08/31 00:11:27
     */
    @PostMapping(value = "/wnql")
    public NexusResult wnqlId(@RequestBody Map<String, Object> params) {
        return nexusNeo4jAdvancedTemplate.execute("location.findOnePath",
                params, NexusResult.class);
    }

    /**
     * index.
     *
     * @return com.widdo.nexus.core.result.NexusResult
     * @author XYL
     * @date 2025/12/17 10:08:10
     */
    @PostMapping(value = "/index")
    public NexusResult index() {
        List<Map<String, Value>> indexes = nexusNeo4jAdvancedTemplate.executeCypher(
                "SHOW INDEXES", Collections.emptyMap(), List.class);
        return NexusResult.response(IResultInterface.Neo4j.SUCCESS, indexes);
    }


    /**
     * Nexus Hadoop 命令Id 操作
     * <p>
     * hdfs常用命令
     *
     * @param params params
     * @return com.widdo.nexus.core.result.NexusResult
     * @author XYL
     * @date 2025/12/09 15:19:22
     */
    @PostMapping(value = "/hadoop")
    public NexusResult hadoop(@RequestBody Map<String, Object> params) {
        return nexusHdfsAdvancedTemplate.execute(params.getOrDefault("commandId", "").toString(), params, NexusResult.class);
    }

    /**
     * Nexus Hadoop  命令操作
     *
     * @param params
     * @return com.widdo.nexus.core.result.NexusResult
     * @author XYL
     * @date 2025/12/17 10:07:50
     */
    @PostMapping(value = "/hadoop/command")
    public NexusResult hadoopCommand(@RequestBody Map<String, Object> params) {
        return nexusHdfsAdvancedTemplate.executeCypher(params.getOrDefault("command", "").toString(), params, NexusResult.class);
    }
}