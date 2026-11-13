package com.rayful.elastic.api.common.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.rayful.elastic.api.common.util.GsonUtils;
import com.rayful.elastic.api.common.util.SearchUtil;

public class DSLQueryBuilder {
	
	private Map<String, Object> dslMap; //전체 DSL Map
    private boolean trackTotalHits; // 전체 검색결과 10000건 제한 해제
    private int size; // 요청 검색 count
    private int from; // 요청 페이지
    private Map<String, Object> queryMap; // 쿼리 Map
    private List<Map<String, Map<String, String>>> sortList; // 정렬
    private Map<String, Object> highlightMap = new HashMap<>(); // 하이라이트 Map
    private Map<String, Map<String, Object>> aggregationsMap = new HashMap<>(); // 어그리게이션 Map
    private Map<String, Object> sourceMap = new HashMap<>(); // 소스 Map(include 대상)
    
    public DSLQueryBuilder builder() {
    	dslMap = new LinkedHashMap<>();
        return this;
    }

    public DSLQueryBuilder trackTotalHits(boolean trackTotalHits) {
        this.trackTotalHits = trackTotalHits;
        return this;
    }

    public DSLQueryBuilder from(int from) {
        this.from = from;
        return this;
    }

    public DSLQueryBuilder size(int size) {
        this.size = size;
        return this;
    }

    public DSLQueryBuilder sortList(List<Map<String, Map<String, String>>> sortList) {
        if (!sortList.isEmpty()) {
            this.sortList = sortList;
        }
        return this;
    }
    
    public DSLQueryBuilder sort(String fields, String sep1, String sep2) {
        
        List<Map<String, Map<String, String>>> resultList = new ArrayList<>();
	    
        if (fields != null && fields.length() > 0) {
	        List<String> sortList = SearchUtil.getStringToList(fields, sep1);

	        for (String item : sortList) {
	            if (item.indexOf(sep2) > -1) {
	                String fieldName = item.split(sep2)[0];
	                String direction = item.split(sep2)[1];

	                Map<String, String> orderMap = new HashMap<>();
	                orderMap.put("order", direction);

	                Map<String, Map<String, String>> sortMap = new HashMap<>();
	                sortMap.put(fieldName, orderMap);

	                resultList.add(sortMap);
	            }
	        }
	    }
        
	    this.sortList = resultList;
        return this;
    }
    
    public DSLQueryBuilder highlight(String[] fields, Map<String, Object> highlightOptionMap) {
        if (fields != null && fields.length > 0) {
            Map<String, Object> fieldMap = new HashMap<>();
            for (String field : fields) {
                fieldMap.put(field, new HashMap<>());
            }
            this.highlightMap.put("fields", fieldMap);
        }
        
        if (highlightOptionMap!= null && highlightOptionMap.size() > 0) {
            this.highlightMap.putAll(highlightOptionMap);
        }
        return this;
    }
    
    public DSLQueryBuilder highlight(String[] fields, Map<String, Object> highlightOptionMap, String queryText) {
        if (fields != null && fields.length > 0) {
            Map<String, Object> fieldMap = new HashMap<>();
            for (String field : fields) {
                Map<String, Object> fieldOptions = new HashMap<>();

                // highlight_query 추가
                Map<String, Object> matchQuery = new HashMap<>();
                Map<String, Object> matchField = new HashMap<>();
                matchField.put(field, queryText);
                matchQuery.put("match", matchField);

                fieldOptions.put("highlight_query", matchQuery);

                fieldMap.put(field, fieldOptions);
            }
            this.highlightMap.put("fields", fieldMap);
        }

        if (highlightOptionMap != null && !highlightOptionMap.isEmpty()) {
            this.highlightMap.putAll(highlightOptionMap);
        }
        return this;
    }
    
    public DSLQueryBuilder highlight(String[] fields, Map<String, Object> highlightOptionMap, List<String> kwdList, String keywordOption) {
        if (fields != null && fields.length > 0 && kwdList != null && !kwdList.isEmpty()) {
            Map<String, Object> fieldMap = new HashMap<>();
            
            String matchQueryStr = "match";
            
            if("phrase".equalsIgnoreCase(keywordOption)) {
            	matchQueryStr = "match_phrase";
            }
            for (String field : fields) {
                Map<String, Object> fieldOptions = new HashMap<>();

                // 키워드 각각을 match로 만들고 must에 넣기 (AND 조건)
                List<Map<String, Object>> mustList = new ArrayList<>();
                for (String keyword : kwdList) {
                    Map<String, Object> matchField = new HashMap<>();
                    matchField.put(field, keyword);

                    Map<String, Object> matchQuery = new HashMap<>();
                    matchQuery.put(matchQueryStr, matchField);

                    mustList.add(matchQuery);
                }

                // highlight_query: { "bool": { "must": [ ... ] } }
                Map<String, Object> boolQuery = new HashMap<>();
                boolQuery.put("must", mustList);

                Map<String, Object> highlightQuery = new HashMap<>();
                highlightQuery.put("bool", boolQuery);

                fieldOptions.put("highlight_query", highlightQuery);
                fieldMap.put(field, fieldOptions);
            }

            this.highlightMap.put("fields", fieldMap);
        }

        if (highlightOptionMap != null && !highlightOptionMap.isEmpty()) {
            this.highlightMap.putAll(highlightOptionMap);
        }

        return this;
    }
    
    public DSLQueryBuilder aggregation(Map<String, Map<String, Object>> arrMap) {
        if (!arrMap.isEmpty()) {
            this.aggregationsMap = arrMap;
        }
        return this;
    }
    
    public DSLQueryBuilder source(String[] fields) {
        if (fields != null && fields.length > 0) {
            this.sourceMap.put("includes", fields);
        }
        return this;
    }

    public DSLQueryBuilder query(IQueryBuilder queryBuilder) {
        this.queryMap = queryBuilder.build();
        return this;
    }

    public DSLQueryBuilder build() {
    	if (this.trackTotalHits) {
        	dslMap.put("track_total_hits", true);
        }
    	
        if (this.size >= 0) {
        	dslMap.put("size", this.size);
        }

        if (this.from >= 0) {
        	dslMap.put("from", this.from);
        }
        
        if (this.queryMap != null && !this.queryMap.isEmpty()) {
            dslMap.put("query", this.queryMap);
        }

        if (this.sortList != null && !this.sortList.isEmpty()) {
        	dslMap.put("sort", this.sortList);
        }
        
        if (this.highlightMap != null && !this.highlightMap.isEmpty()) {
        	dslMap.put("highlight", this.highlightMap);
        }
        
        if (this.aggregationsMap != null && !this.aggregationsMap.isEmpty()) {
        	dslMap.put("aggs", this.aggregationsMap);
        }
        
        if (this.sourceMap != null && !this.sourceMap.isEmpty()) {
        	dslMap.put("_source", this.sourceMap);
        }
        
        return this;
    }

    public String buildJson() {
        return GsonUtils.toJson(this.dslMap);
    }
	
}
