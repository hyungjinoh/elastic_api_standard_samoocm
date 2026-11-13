package com.rayful.elastic.api.common.builder;

import java.util.HashMap;
import java.util.Map;

import com.rayful.elastic.api.common.exception.QueryBuilderException;

public class ExistsQueryBuilder implements IQueryBuilder {

    private Map<String, Object> existsMap = new HashMap<>();

    public ExistsQueryBuilder field(String fieldName) throws QueryBuilderException {
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("field", fieldName);
        this.existsMap.put("exists", valueMap);
        return this;
    }

    @Override
    public Map<String, Object> build() {
        return this.existsMap;
    }
}
