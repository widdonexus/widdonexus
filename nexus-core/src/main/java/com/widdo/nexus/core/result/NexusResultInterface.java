package com.widdo.nexus.core.result;

import com.widdo.nexus.core.result.entity.*;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * NexusResultInterface, convert result to {@link NexusResult}.
 *
 * @param <T>
 * @param <R>
 * @author XYL
 * @date 2022/12/30 16:22
 * @since 263.1.1.0
 */
@SuppressWarnings("ALL")
public interface NexusResultInterface<T, R> {

    /**
     * wrapper result.
     *
     * @param t t
     * @return R
     */
    R wrapper(T t);

    /**
     * wrapper node data.
     *
     * @param node node type of {@link Node}
     * @return node data type of Map
     */
    default Map<String, Object> node(Node node) {
        final HashMap<String, Object> map = new HashMap<>(3);
        final Map<String, Object> properties = node.getProperties();

        final Collection<String> labels = node.getLabels();

        final String id = node.getId();
        map.put("id", id);
        map.put("labels", labels);
        map.put("properties", properties);
        return map;
    }

    /**
     * wrapper relationShip.
     *
     * @param relationship relationShip
     * @return relation data type of Map
     */
    default Map<String, Object> relationShip(Relationship relationship) {
        final HashMap<String, Object> map = new HashMap<>(5);
        map.put("id", relationship.getId());
        map.put("type", relationship.getType());
        map.put("startNodeElementId", relationship.getStartNodeElementId());
        map.put("endNodeElementId", relationship.getEndNodeElementId());
        map.put("properties", relationship.getProperties());

        return map;
    }

    /**
     * wrapper node.
     *
     * @param nodeList nodeList
     * @param value    the value of neo4j result
     */
    default void setNode(Value value, List<Map<String, Object>> nodeList) {
        final Node node = value.asNode();
        final Map<String, Object> nodeMap = node(node);
        nodeList.add(nodeMap);
    }

    /**
     * wrapper relationship.
     *
     * @param value            the value of neo4j result
     * @param relationShipList relationShipList
     */
    default void setRelationship(Value value, List<Map<String, Object>> relationShipList) {
        final Relationship relationship = value.asRelationship();
        final Map<String, Object> relationShipMap = relationShip(relationship);
        relationShipList.add(relationShipMap);
    }

    /**
     * wrapper list result.
     *
     * @param value      the value of neo4j result
     * @param listResult listResult
     */
    default void setList(Value value, List<Object> listResult) {
        final List list = value.asList();
        listResult.addAll(list);
    }

    /**
     * wrapper path.
     *
     * @param value            the value of neo4j result
     * @param nodeList         nodeList
     * @param relationShipList relationShipList
     */
    default void setPath(Value value, List<Map<String, Object>> nodeList, List<Map<String, Object>> relationShipList) {
        final Path path = value.asPath();
        path.getNodes().forEach(n -> {
            final Map<String, Object> node = node(n);
            nodeList.add(node);
        });

        path.getRelationships().forEach(r -> {
            final Map<String, Object> relationShip = relationShip(r);
            relationShipList.add(relationShip);
        });
    }

    enum NEO4j implements NexusResultInterface<Result<List<Map<String, Value>>>, NexusResult> {

        /**
         * node.
         */
        NODE {
            @Override
            public NexusResult wrapper(Result<List<Map<String, Value>>> listResult) {

                final List<Map<String, Object>> nodeList = new ArrayList<>();

                if (ResultEnum.SUCCESS.equals(listResult.getStatus())) {
                    final List<Map<String, Value>> list = listResult.getData();
                    if (!ObjectUtils.isEmpty(list)) {
                        list.forEach(r -> r.forEach((k, v) -> {
                            final String type = v.getType();
                            if (Neo4jType.NODE.equals(type)) {
                                setNode(v, nodeList);
                            }
                        }));
                    }

                    return NexusResult.response(IResultInterface.Neo4j.SUCCESS, nodeList);
                }
                return NexusResult.response(IResultInterface.Neo4j.FAIL);
            }
        },

        /**
         * relationShip.
         */
        RELATIONSHIP {
            @Override
            public NexusResult wrapper(Result<List<Map<String, Value>>> listResult) {
                final ArrayList<Map<String, Object>> relationShipList = new ArrayList<>();

                if (ResultEnum.SUCCESS.equals(listResult.getStatus())) {
                    final List<Map<String, Value>> list = listResult.getData();
                    if (!ObjectUtils.isEmpty(list)) {
                        list.forEach(r -> r.forEach((k, v) -> {
                            final String type = v.getType();
                            if (Neo4jType.RELATIONSHIP.equals(type)) {
                                setRelationship(v, relationShipList);
                            }
                        }));
                    }
                    return NexusResult.response(IResultInterface.Neo4j.SUCCESS, relationShipList);
                }
                return NexusResult.response(IResultInterface.Neo4j.FAIL);
            }
        },

        /**
         * path.
         */
        PATH {
            @Override
            public NexusResult wrapper(Result<List<Map<String, Value>>> listResult) {

                final HashMap<String, Object> map = new HashMap<>(2);

                final ArrayList<Map<String, Object>> nodeList = new ArrayList<>();
                final ArrayList<Map<String, Object>> relationShipList = new ArrayList<>();

                if (ResultEnum.SUCCESS.equals(listResult.getStatus())) {
                    final List<Map<String, Value>> list = listResult.getData();
                    if (!ObjectUtils.isEmpty(list)) {
                        list.forEach(p -> p.forEach((k, v) -> {
                            final String type = v.getType();
                            if (Neo4jType.PATH.equals(type)) {
                                setPath(v, nodeList, relationShipList);
                            }
                        }));
                    }

                    map.put("nodes", nodeList);
                    map.put("relationships", relationShipList);

                    return NexusResult.response(IResultInterface.Neo4j.SUCCESS, map);
                }
                return NexusResult.response(IResultInterface.Neo4j.FAIL);
            }
        },

        /**
         * all.
         */
        ALL {
            @Override
            public NexusResult wrapper(Result<List<Map<String, Value>>> listResult) {
                if (!ResultEnum.SUCCESS.equals(listResult.getStatus())) {
                    return NexusResult.response(IResultInterface.Neo4j.FAIL);
                }

                List<Map<String, Value>> listData = listResult.getData();
                if (ObjectUtils.isEmpty(listData)) {
                    return NexusResult.response(IResultInterface.Neo4j.SUCCESS, Collections.emptyList());
                }

                final List<Map<String, ?>> collect = listData.stream()
                        .flatMap(map -> map.entrySet().stream())
                        .map(entry -> {
                            final String key = entry.getKey();
                            final Value value = entry.getValue();
                            return switch (value.getType()) {
                                case Neo4jType.PATH -> Map.of(key, value.asPath());
                                case Neo4jType.NODE -> Map.of(key, value.asNode());
                                case Neo4jType.RELATIONSHIP -> Map.of(key, value.asRelationship());
                                case Neo4jType.LISTOFANY -> Map.of(key, value.asList());
                                default ->
                                        throw new UnsupportedOperationException("Unsupported Neo4j type: " + value.getType());
                            };
                        })
                        .collect(Collectors.toList());

                return NexusResult.response(IResultInterface.Neo4j.SUCCESS, collect);

            }

        }

    }

    enum HADOOP implements NexusResultInterface<Object, NexusResult> {

        /**
         * hdfs.
         */
        HDFS {
            @Override
            public NexusResult wrapper(Object o) {
                return NexusResult.response(IResultInterface.Hadoop.SUCCESS, o);
            }
        },

        /**
         * yarn.
         */
        YARN {
            @Override
            public NexusResult wrapper(Object o) {
                return null;
            }
        },

        /**
         * map_reduce.
         */
        MAP_REDUCE {
            @Override
            public NexusResult wrapper(Object o) {
                return null;
            }
        };

    }

}
