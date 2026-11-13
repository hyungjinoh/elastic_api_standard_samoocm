package com.rayful.elastic.api.common.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "search")
public class SearchConfig {
	private String dblogUse;
	private List<String> apiKey;
	private AllowIp allowIp;
	private Map<String, String> siteInfo;
	private Map<String, String> productInfo;
	private Map<String, String> indexName;
	private Map<String, String> sectionName;
	private Map<String, String> sectionCode;
	private Map<String, String> includes;
	private Map<String, Map<String, String>> fields;
	private Map<String, String> highlight;
	private Map<String, String> highlightSub;
	private Map<String, Object> highlightOption;
	private String highlightAddField;
	private Map<String, Map<String, String>> sort;
	private Map<String, String> attached;
	private String aclIndices;
	private Map<String, Map<String, String>> condition;
	private String aggsMaxCount;
	private Map<String, Map<String, String>> aggs;
	private Map<String, String> aggsSort;
	private Map<String, String> queryOption;
	private Map<String, String> queryPhraseOption;
	private Map<String, String> rangeOption;
	private int contentMaxSize;
	private Popular popular;
	
	// 메뉴 검색 동의어 인덱스명 (application.yml의 search.menu-synonym-index)
	private String menuSynonymIndex;
	
	// 통합검색(collapseCd=99999) 시 인덱스(섹션)당 미리보기 노출 개수 (application.yml의 search.integrated-preview-size)
	private int integratedPreviewSize;
	
	@Getter
    @Setter
    public static class AllowIp {
        private String use;
        private List<String> ip;
    }

    @Getter
    @Setter
    public static class Popular {
    	private String schedulerUse;
    	private String schedulerType;
        private String schedulerCron;
        private Map<String, Object> es;
        private Map<String, Object> db;
    }
}
