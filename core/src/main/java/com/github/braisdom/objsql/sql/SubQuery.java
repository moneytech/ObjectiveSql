package com.github.braisdom.objsql.sql;

import com.github.braisdom.objsql.util.WordUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SubQuery extends Select {

    private Map<String, Expression> projectionMaps = new HashMap<>();

    @Override
    public SubQuery project(Expression... projections) {
        Objects.requireNonNull(projections, "The projections cannot be null");
        super.project(projections);
        for(Expression expression : projections) {
            projectionMaps.put(expression.getAlias(), expression);
        }
        return this;
    }

    @Override
    public SubQuery as(String alias) {
        super.as(alias);
        return this;
    }

    public Expression getProjection(String name) {
        return projectionMaps.get(name);
    }

    public Expression col(String name) {
        return projectionMaps.get(name);
    }

    @Override
    public String toSql(ExpressionContext expressionContext) throws SQLSyntaxException {
        String alias = getAlias();
        return String.format("(%s) %s", super.toSql(expressionContext),
                WordUtil.isEmpty(alias) ? "" : String.format(" AS %s", alias));
    }
}