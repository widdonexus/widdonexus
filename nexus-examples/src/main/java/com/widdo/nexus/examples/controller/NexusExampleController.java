package com.widdo.nexus.examples.controller;

import com.widdo.nexus.core.result.NexusResult;
import com.widdo.nexus.core.support.query.NexusQuery;
import com.widdo.nexus.core.support.query.NexusQueryBuilder;
import com.widdo.nexus.core.support.template.NexusAdvancedTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    private final NexusAdvancedTemplate nexusAdvancedTemplate;

    @Autowired
    public NexusExampleController(NexusAdvancedTemplate nexusAdvancedTemplate) {
        this.nexusAdvancedTemplate = nexusAdvancedTemplate;
    }

    /**
     * Nexus 函数式查询语言 wnql
     *
     * @param
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

        return nexusAdvancedTemplate.execute(query, NexusResult.class);
    }

    /**
     * Nexus wnql查询
     *
     * @param params
     *
     * @author XYL
     * @date 2025/08/31 00:11:27
     * @return com.widdo.nexus.core.result.NexusResult
     */
    @PostMapping(value = "/wnql")
    public NexusResult wnqlId(@RequestBody Map<String, Object> params) {
        return nexusAdvancedTemplate.execute("location.findOnePath",
                params, NexusResult.class);
    }
}
