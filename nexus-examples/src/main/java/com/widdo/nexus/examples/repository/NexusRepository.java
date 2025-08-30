package com.widdo.nexus.examples.repository;

import com.widdo.nexus.core.annotation.Query;
import com.widdo.nexus.core.annotation.QueryRef;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * NexusRepository.
 *
 * @author XYL
 * @date 2025/08/29 14:11
 * @since 0.0.1-SNAPSHOT
 */
public interface NexusRepository {

    @Query(value = "MATCH (e: employee) return e", description = "查询所有员工")
    Optional<List<Map<String, Object>>> employees();

    @QueryRef(value = "employee.findCompany", description = "查询员工就职的公司信息")
    List<Map<String, Object>> findCompany(String userId);

    @QueryRef(value = "location.findOnePath", description = "查询已知路径深度为1的所有路径图")
    List<Map<String, Object>> findLocationOnePath(String userId);
}
