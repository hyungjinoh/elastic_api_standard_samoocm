package com.rayful.elastic.api.common.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rayful.elastic.api.standard.vo.RequestAutoParamVO;
import com.rayful.elastic.api.standard.vo.RequestSearchParamVO;

public class ConfigValidator {
    
    private static final Logger log = LoggerFactory.getLogger(ConfigValidator.class);
    
    private static final String SEARCH_SEPARATOR01 = ","; 			// 설정파일 구분자(1차구분자)

    public static String validateSearchConfig(SearchConfig searchConfig, RequestSearchParamVO paramVO, String traceId) {
        String indexNames = searchConfig.getIndexName().get(paramVO.getCollapseCd());
        String errMsg = null;
        
        if(indexNames != null && indexNames.length() > 0 && !"idx_auto".equalsIgnoreCase(indexNames)) {

        	// [우장각 커스텀] 원래는 "collapseCd==99999" 일 때만 콤마(멀티 인덱스)를 허용했는데,
        	// 그 조건을 "index-name 설정값 자체가 콤마를 포함하는지"로 일반화했다. 이러면 99999 가
        	// 아닌 다른 collapseCd 도(예: 90000=우장각 통합검색) 설정만으로 멀티 인덱스 조회가 가능해진다.
        	// 99999 전용이던 동작은 그대로 유지됨 — 99999 의 index-name 도 여전히 콤마를 포함하면 멀티로 처리.
        	boolean isMultiIndex = indexNames.contains(SEARCH_SEPARATOR01);

        	String[] arrIndexNames = isMultiIndex
                    ? indexNames.split(SEARCH_SEPARATOR01)  // 여러 개
                    : new String[] { indexNames };          // 단일 값
        	
            List<String> searchAreas = paramVO.getSrchArea();
            String sort = paramVO.getSort();
            log.debug("[{}] Index Names : {}", traceId, indexNames);
            
            for (String indexName : arrIndexNames) {
                errMsg = checkCommonConfig(searchConfig, indexName, searchAreas, sort, traceId);
                if (errMsg != null) break;

                errMsg = checkSearchSpecificConfig(searchConfig, indexName, traceId);
                if (errMsg != null) break;
            }
        } else {
        	log.error("[{}] request Param - Invalid CollapseCd : {}", traceId, paramVO.getCollapseCd());
        	errMsg = "request Param - Invalid CollapseCd";
        }

        return errMsg;
    }

    public static String validateAutoCompleteConfig(SearchConfig searchConfig, RequestAutoParamVO paramVO, String traceId) {
    	String indexName = searchConfig.getIndexName().get(paramVO.getCollapseCd());
    	String errMsg = null;
        
        if((indexName != null && indexName.length() > 0) && "idx_auto".equalsIgnoreCase(indexName)) {

        	List<String> searchAreas = paramVO.getSrchArea();
            String sort = paramVO.getSort();
            
            log.debug("[{}] Index Name : {}", traceId, indexName);
            
            errMsg = checkCommonConfig(searchConfig, indexName, searchAreas, sort, traceId);
            
            if(errMsg == null) {
            	errMsg = checkAutoCompleteSpecificConfig(searchConfig, indexName, traceId);
            }
        } else {
        	log.error("[{}] request Param - Invalid CollapseCd : {}", traceId, paramVO.getCollapseCd());
        	errMsg = "request Param - Invalid CollapseCd";
        }

        return errMsg;
    }
    
    private static String checkCommonConfig(SearchConfig searchConfig, String indexName, List<String> searchAreas, String sort, String traceId) {
        // Check sectionCode
        if (!(searchConfig.getSectionCode().containsKey(indexName) && 
              searchConfig.getSectionCode().get(indexName) != null &&
              !searchConfig.getSectionCode().get(indexName).isEmpty())) {
            log.error("[{}] application.xml - Invalid sectionCode for index: {}", traceId, indexName);
            return "Invalid sectionCode for index: " + indexName;
        } 
        // Check sectionName
        if (!(searchConfig.getSectionName().containsKey(indexName) && 
               searchConfig.getSectionName().get(indexName) != null &&
               !searchConfig.getSectionName().get(indexName).isEmpty())) {
            log.error("[{}] application.xml - Invalid sectionName for index: {}", traceId, indexName);
            return "Invalid sectionName for index: " + indexName;
        } 
        // Check includes
        if (!(searchConfig.getIncludes().containsKey(indexName) && 
               searchConfig.getIncludes().get(indexName) != null &&
               !searchConfig.getIncludes().get(indexName).isEmpty())) {
            log.error("[{}] application.xml - Invalid includes for index: {}", traceId, indexName);
            return "Invalid includes for index: " + indexName;
        } 
        // Check sort
        if (!(searchConfig.getSort().containsKey(indexName) && 
                searchConfig.getSort().get(indexName).containsKey(sort) &&
                searchConfig.getSort().get(indexName).get(sort) != null &&
                searchConfig.getSort().get(indexName).get(sort).length() > 0)) {
            log.error("[{}] application.xml - Invalid sort for index: {}", traceId, indexName);
            return "Invalid sort for index: " + indexName;
        } 
        // Check fields for each search area
        for (String area : searchAreas) {
            if (!(searchConfig.getFields().containsKey(indexName) && 
                  searchConfig.getFields().get(indexName).containsKey(area) &&
                  searchConfig.getFields().get(indexName).get(area) != null &&
                  searchConfig.getFields().get(indexName).get(area).length() > 0)) {
                log.error("[{}] application.xml - Invalid fields for index: {} and search area: {}", traceId, indexName, area);
                return "Invalid fields for index: " + indexName + " and search area: " + area;
            }
        }
        
        return null;
    }

    private static String checkSearchSpecificConfig(SearchConfig searchConfig, String indexName, String traceId) {
        // Check highlight
        if (!(searchConfig.getHighlight().containsKey(indexName))) {
            log.error("[{}] application.xml - Invalid highlight for index: {}", traceId, indexName);
            return "Invalid highlight for index: " + indexName;
        } 
        // Check highlight sub
        if (!(searchConfig.getHighlightSub().containsKey(indexName))) {
            log.error("[{}] application.xml - Invalid highlight sub for index: {}", traceId, indexName);
            return "Invalid highlight sub for index: " + indexName;
        } 
        // Check aggregation
        if (!(searchConfig.getAggs().containsKey(indexName))) {
            log.error("[{}] application.xml - Invalid aggregation for index: {}", traceId, indexName);
            return "Invalid aggregation for index: " + indexName;
        } 

        return null;
    }

    private static String checkAutoCompleteSpecificConfig(SearchConfig searchConfig, String indexName, String traceId) {
    	// Check highlight
        if (!(searchConfig.getHighlight().containsKey(indexName))) {
            log.error("[{}] application.xml - Invalid highlight for index: {}", traceId, indexName);
            return "Invalid highlight for index: " + indexName;
        } 

        return null;
    }
    
}
