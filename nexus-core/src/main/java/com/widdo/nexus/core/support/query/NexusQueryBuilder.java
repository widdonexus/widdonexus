package com.widdo.nexus.core.support.query;

import com.widdo.nexus.core.enums.NexusDirection;
import com.widdo.nexus.core.enums.NexusSortDirection;
import lombok.Value;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * NexusQueryBuilder
 * <p>
 * Nexus查询构建器
 * <p>
 * 设计模式：建造者模式
 *
 * @author XYL
 * @date 2025/08/27 14:43
 * @since 0.0.1-SNAPSHOT
 */
public class NexusQueryBuilder {
    private final StringBuilder cypher = new StringBuilder();
    private final Map<String, Object> parameters = new HashMap<>();
    private final List<Condition> conditions = new ArrayList<>();
    private final List<String> returns = new ArrayList<>();

    public static NexusQueryBuilder create() {
        return new NexusQueryBuilder();
    }

    /**
     * NexusQueryBuilder match.
     *
     * @param matchConsumer matchConsumer
     * @return com.widdo.nexus.support.query.NexusQueryBuilder
     * @author XYL
     * @date 2025/08/27 14:46:51
     */
    public NexusQueryBuilder match(Consumer<MatchBuilder> matchConsumer) {
        MatchBuilder builder = new MatchBuilder();
        matchConsumer.accept(builder);
        cypher.append("MATCH ").append(builder.build()).append("\n");
        return this;
    }

    /**
     * NexusQueryBuilder where.
     *
     * @param whereConsumer whereConsumer
     * @return com.widdo.nexus.support.query.NexusQueryBuilder
     * @author XYL
     * @date 2025/08/27 15:09:07
     */
    public NexusQueryBuilder where(Consumer<WhereBuilder> whereConsumer) {
        WhereBuilder builder = new WhereBuilder();
        whereConsumer.accept(builder);
        conditions.addAll(builder.getConditions());
        return this;
    }

    /**
     * NexusQueryBuilder optionalMatch.
     *
     * @param matchConsumer matchConsumer
     * @return com.widdo.nexus.support.query.NexusQueryBuilder
     * @author XYL
     * @date 2025/08/27 15:10:23
     */
    public NexusQueryBuilder optionalMatch(Consumer<MatchBuilder> matchConsumer) {
        MatchBuilder builder = new MatchBuilder();
        matchConsumer.accept(builder);
        cypher.append("OPTIONAL MATCH ").append(builder.build()).append("\n");
        return this;
    }

    /**
     * NexusQueryBuilder returning.
     *
     * @param returnConsumer returnConsumer
     * @return com.widdo.nexus.support.query.NexusQueryBuilder
     * @author XYL
     * @date 2025/08/27 15:12:46
     */
    public NexusQueryBuilder returning(Consumer<ReturnBuilder> returnConsumer) {
        ReturnBuilder builder = new ReturnBuilder();
        returnConsumer.accept(builder);
        returns.addAll(builder.getReturns());
        return this;
    }

    /**
     * NexusQueryBuilder orderBy.
     *
     * @param orderByConsumer orderByConsumer
     * @return com.widdo.nexus.support.query.NexusQueryBuilder
     * @author XYL
     * @date 2025/08/27 15:24:23
     */
    public NexusQueryBuilder orderBy(Consumer<OrderByBuilder> orderByConsumer) {

        final OrderByBuilder builder = new OrderByBuilder();
        orderByConsumer.accept(builder);

        cypher.append("ORDER BY ").append(builder.build()).append("\n");
        return this;
    }

    /**
     * NexusQueryBuilder skip.
     *
     * @param count count
     * @return com.widdo.nexus.support.query.NexusQueryBuilder
     * @author XYL
     * @date 2025/08/27 15:25:30
     */
    public NexusQueryBuilder skip(int count) {
        cypher.append("SKIP ").append(count).append("\n");
        return this;
    }

    /**
     * NexusQueryBuilder limit.
     *
     * @param count count
     * @return com.widdo.nexus.support.query.NexusQueryBuilder
     * @author XYL
     * @date 2025/08/27 15:26:17
     */
    public NexusQueryBuilder limit(int count) {
        cypher.append("LIMIT ").append(count).append("\n");
        return this;
    }

    /**
     * NexusQueryBuilder build.
     *
     * @param
     * @return com.widdo.nexus.support.query.NexusQuery
     * @author XYL
     * @date 2025/08/27 15:31:07
     */
    public NexusQuery build() {
        // 构建WHERE子句
        if (!conditions.isEmpty()) {
            cypher.append("WHERE ");
            String whereClause = conditions.stream()
                    .map(Condition::toString)
                    .collect(Collectors.joining(" AND "));
            cypher.append(whereClause).append("\n");
        }

        if (!returns.isEmpty()) {
            cypher.append("RETURN ").append(String.join(" ", returns));
        }

        return new NexusQuery(cypher.toString(), Map.copyOf(parameters));
    }

    // 条件类
    @Value
    private static class Condition {
        String expression;

        @Override
        public String toString() {
            return expression;
        }
    }

    // 内部构建器类
    public class MatchBuilder {
        private final List<String> patterns = new ArrayList<>();

        public MatchBuilder node(String label, String alias) {
            patterns.add("(" + alias + (label != null ? ":" + label : "") + ")");
            return this;
        }

        public MatchBuilder relationship(String type, String alias, NexusDirection direction) {
            String relPattern = direction == NexusDirection.OUTGOING ? "-[" : "<-[";
            relPattern += (alias != null ? alias : "");
            relPattern += (type != null ? ":" + type : "");
            relPattern += direction == NexusDirection.OUTGOING ? "]->" : "]-";
            patterns.add(relPattern);
            return this;
        }

        public String build() {
            return String.join(" ", patterns);
        }
    }

    // WHERE条件构建器
    public class WhereBuilder {
        private final List<Condition> conditions = new ArrayList<>();

        public WhereBuilder eq(String property, Object value) {
            String paramName = "param" + parameters.size();
            parameters.put(paramName, value);
            conditions.add(new Condition(property + " = $" + paramName));
            return this;
        }

        public WhereBuilder gt(String property, Object value) {
            String paramName = "param" + parameters.size();
            parameters.put(paramName, value);
            conditions.add(new Condition(property + " > $" + paramName));
            return this;
        }

        public WhereBuilder in(String property, Collection<?> values) {
            String paramName = "param" + parameters.size();
            parameters.put(paramName, values);
            conditions.add(new Condition(property + " IN $" + paramName));
            return this;
        }

        public List<Condition> getConditions() {
            return conditions;
        }
    }

    public class ReturnBuilder {
        private final List<String> returns = new ArrayList<>();

        public ReturnBuilder returning(String... expressions) {
            if (expressions.length == 0) {
                returns.add("*");
            } else {
                returns.add(String.join(",", expressions));
            }
            return this;
        }

        public List<String> getReturns() {
            return returns;
        }
    }

    public class OrderByBuilder {
        private final List<String> patterns = new ArrayList<>();

        public OrderByBuilder orderBy(String property, NexusSortDirection direction) {
            patterns.add(property);
            patterns.add(direction.name());
            return this;
        }

        public String build() {
            return String.join(" ", patterns);
        }
    }
}
