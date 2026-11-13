package com.rayful.elastic.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rayful.elastic.api.common.config.SearchConfig;
import com.rayful.elastic.api.common.service.PopularKeywordService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
	    prefix = "search.popular", 
	    name = "scheduler-use", 
	    havingValue = "Y", 
	    matchIfMissing = false
)
public class AppScheduler {

    private final PopularKeywordService popularKeywordService;
    
    @Autowired
	private SearchConfig searchConfig;

    private List<Map<String, Object>> elasticKeywordList = new ArrayList<>();
    private List<Map<String, Object>> dbKeywordList = new ArrayList<>();

    @Scheduled(cron = "#{@searchConfig.popular.schedulerCron}")
    public void refreshPopularKeywords() {
    	
		log.info("@@@PopularKeywordScheduler execute : {}", java.time.LocalDateTime.now());
    	
    	if ("all".equalsIgnoreCase(searchConfig.getPopular().getSchedulerType())) {
    		dbKeywordList = popularKeywordService.getDbPopularKeywordList(null);
    		log.info("dbKeywordList : {}", dbKeywordList);
    		
    		elasticKeywordList = popularKeywordService.getEsPopularKeywordList(null);
    		log.info("elasticKeywordList : {}", elasticKeywordList);
    	} else if ("db".equalsIgnoreCase(searchConfig.getPopular().getSchedulerType())) {
    		dbKeywordList = popularKeywordService.getDbPopularKeywordList(null);
    		log.info("dbKeywordList : {}", dbKeywordList);
    	} else {
    		elasticKeywordList = popularKeywordService.getEsPopularKeywordList(null);
    		log.info("elasticKeywordList : {}", elasticKeywordList);
    	}
    }
    
    public List<Map<String, Object>> getDbKeywordList() {
        return dbKeywordList;
    }
    
    public List<Map<String, Object>> getElasticKeywordList() {
        return elasticKeywordList;
    }
   
}
