package com.widdo.nexus.neo4j.util;

import org.neo4j.driver.types.IsoDuration;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Point;
import org.neo4j.driver.types.Relationship;

/**
 * neo4j原生结果类型转换.
 *
 * @author XYL
 * @date 2022/08/15 15:09
 * @since 263.1.1.0
 */
@SuppressWarnings("ALL")
public class Neo4jConvertor {

    /**
     * public constructor.
     *
     * @return an object called {@link Neo4jConvertor}
     */
    public static Neo4jConvertor build() {
        return new Neo4jConvertor();
    }

    /**
     * 转换neo4j的node.
     *
     * @param nodeOrigin 原始节点
     * @return com.widdo.nexus.core.result.entity.Node
     * @author XYL
     * @className cn.widdo.starter.neo4j.utils.Neo4jConvertor
     * @date 2022/09/26 18:24
     **/
    public com.widdo.nexus.core.result.entity.Node convertNeo4jNodeToNode(Node nodeOrigin) {
        com.widdo.nexus.core.result.entity.Node node = new com.widdo.nexus.core.result.entity.Node();
        node.setProperties(nodeOrigin.asMap());
        node.setId(nodeOrigin.elementId());
        node.setLabels(nodeOrigin.labels());
        return node;
    }

    /**
     * 转换neo4j的relationship.
     *
     * @param relationshipOrigin 原始关系
     * @return com.widdo.nexus.core.result.entity.Relationship
     * @author XYL
     * @className cn.widdo.starter.neo4j.utils.Neo4jConvertor
     * @date 2022/09/26 18:24
     **/
    public com.widdo.nexus.core.result.entity.Relationship convertNeo4jRelationshipToRelationship(
            Relationship relationshipOrigin) {
        com.widdo.nexus.core.result.entity.Relationship relationship = new com.widdo.nexus.core.result.entity.Relationship();
        relationship.setId(relationshipOrigin.elementId());
        relationship.setProperties(relationshipOrigin.asMap());
        relationship.setStartNodeElementId(relationshipOrigin.startNodeElementId());
        relationship.setEndNodeElementId(relationshipOrigin.endNodeElementId());
        relationship.setType(relationshipOrigin.type());
        return relationship;
    }

    /**
     * 转换neo4j的Point2D.
     *
     * @param pointOrigin 原始 point
     * @return com.widdo.nexus.core.result.entity.Point2D
     * @author XYL
     * @className cn.widdo.starter.neo4j.utils.Neo4jConvertor
     * @date 2022/09/26 18:25
     **/
    public com.widdo.nexus.core.result.entity.Point2D convertNeo4jPoint2DToPoint2D(Point pointOrigin) {
        com.widdo.nexus.core.result.entity.Point2D point = new com.widdo.nexus.core.result.entity.Point2D();
        point.setSrid(pointOrigin.srid());
        point.setX(pointOrigin.x());
        point.setY(pointOrigin.y());
        return point;
    }

    /**
     * 转换neo4j的Point3D.
     *
     * @param pointOrigin 原始 point
     * @return com.widdo.nexus.core.result.entity.Point3D
     * @author XYL
     * @className cn.widdo.starter.neo4j.utils.Neo4jConvertor
     * @date 2022/09/26 18:25
     **/
    public com.widdo.nexus.core.result.entity.Point3D convertNeo4jPoint2DToPoint3D(Point pointOrigin) {
        com.widdo.nexus.core.result.entity.Point3D point = new com.widdo.nexus.core.result.entity.Point3D();
        point.setSrid(pointOrigin.srid());
        point.setX(pointOrigin.x());
        point.setY(pointOrigin.y());
        point.setZ(pointOrigin.z());
        return point;
    }

    /**
     * 转换neo4j的Duration.
     *
     * @param durationOrigin 原始 pint
     * @return com.widdo.nexus.core.result.entity.IsoDuration
     * @author XYL
     * @className cn.widdo.starter.neo4j.utils.Neo4jConvertor
     * @date 2022/09/26 18:26
     **/
    public com.widdo.nexus.core.result.entity.IsoDuration convertNeo4jDurationToDuration(IsoDuration durationOrigin) {
        com.widdo.nexus.core.result.entity.IsoDuration duration = new com.widdo.nexus.core.result.entity.IsoDuration();
        duration.setDays(durationOrigin.days());
        duration.setMonths(durationOrigin.months());
        duration.setSeconds(durationOrigin.seconds());
        duration.setNanoseconds(durationOrigin.nanoseconds());
        return duration;
    }

}
