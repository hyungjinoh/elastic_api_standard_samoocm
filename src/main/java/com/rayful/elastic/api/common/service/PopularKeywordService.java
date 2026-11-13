package com.rayful.elastic.api.common.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rayful.elastic.api.common.builder.AggsQueryBuilder;
import com.rayful.elastic.api.common.builder.BoolQueryBuilder;
import com.rayful.elastic.api.common.builder.DSLQueryBuilder;
import com.rayful.elastic.api.common.builder.RangeQueryBuilder;
import com.rayful.elastic.api.common.builder.TermQueryBuilder;
import com.rayful.elastic.api.common.config.SearchConfig;
import com.rayful.elastic.api.common.dao.ICommonDao;
import com.rayful.elastic.api.common.exception.QueryBuilderException;
import com.rayful.elastic.api.common.vo.SearchResultVO;
import com.rayful.elastic.api.standard.vo.RequestPopParamVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PopularKeywordService {

    @Autowired
    private SearchConfig searchConfig;

    @Autowired
    private ICommonDao commonDao;

    @Autowired
    private ICommonService commonService;

    /**
     * DB에서 인기검색어 가져오기
     */
    public List<Map<String, Object>> getDbPopularKeywordList(RequestPopParamVO rqParams) {
        
    	List<Map<String, Object>> popKeywordList = new ArrayList<>();
        
    	try {
    		
            Map<String, Object> param = new HashMap<>();
            
            param.put("appId", searchConfig.getSiteInfo().get("code"));
            
            if(rqParams != null) {
            	param.put("period", rqParams.getPeriod());
                param.put("size", rqParams.getSize());
            } else {
            	param.put("period",(Integer) searchConfig.getPopular().getDb().get("period"));
                param.put("size", (Integer) searchConfig.getPopular().getDb().get("size"));
            }
            
            log.debug("getDbPopularKeywordList Param : {}", param);
            
            popKeywordList = this.commonDao.selectPopkeywordList(param);
            
            //log.info("getDbPopularKeywordList: {}", popKeywordList);
        } catch (Exception e) {
        	log.error("Failed to Select popKeyword List(DB). Error message: {}", e.getMessage(), e);
        	return List.of(); // 빈 목록 반환
        }
    	
        return popKeywordList;
    }
    
    /**
     * Elasticsearch에서 인기검색어 가져오기
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getEsPopularKeywordList(RequestPopParamVO rqParams) {

        List<Map<String, Object>> popKeywordList = new ArrayList<>();

        try {
            String index = (String) searchConfig.getPopular().getEs().get("index");
            String aggName = (String) searchConfig.getPopular().getEs().get("agg-name");
            
            String query = createPopularKeywordQuery(rqParams).buildJson();
            log.debug("getEsPopularKeywordList param : {}", rqParams);
            log.debug("getEsPopularKeywordList ES Query : {}", query);
            
            SearchResultVO result = commonService.search(index, query);

            // 어그리게인션 처리
            Map<String, Object> aggregations = (Map<String, Object>) result.getResult().get("aggregations");
            if (aggregations == null || !aggregations.containsKey(aggName)) {
                log.warn("Aggregation [{}] not found in result: {}", aggName, result.getResult());
                return popKeywordList;
            }

            List<Map<String, Object>> buckets = (List<Map<String, Object>>)
                ((Map<String, Object>) aggregations.get(aggName)).get("buckets");

            if (buckets == null) {
                log.warn("No buckets found for aggregation [{}]", aggName);
                return popKeywordList;
            }

            for (Map<String, Object> bucket : buckets) {
                Map<String, Object> keywordMap = new HashMap<>();
                keywordMap.put("keyword", bucket.get("key"));
                keywordMap.put("count", bucket.get("doc_count"));
                popKeywordList.add(keywordMap);
            }
            
            //log.info("getEsPopularKeywordList: {}", popKeywordList);
        } catch (Exception e) {
        	log.error("Failed to Request popKeyword List(ES). Error message: {}", e.getMessage(), e);
            return List.of(); // 빈 리스트 반환
        }

        return popKeywordList;
    }
    
    @SuppressWarnings({ "unused", "null" })
	private DSLQueryBuilder createPopularKeywordQuery(RequestPopParamVO rqParams) throws QueryBuilderException {
        // Bool 쿼리 생성
        BoolQueryBuilder boolQuery = new BoolQueryBuilder().builder();
        boolQuery.must(
            new TermQueryBuilder()
                .term((String) searchConfig.getPopular().getEs().get("search-field"), 
                		(String) searchConfig.getPopular().getEs().get("search-value"))
        );
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern((String) searchConfig.getPopular().getEs().get("date-format"));

	    ZoneId zoneId = ZoneId.of((String) searchConfig.getPopular().getEs().get("timezone"));
	
	    ZonedDateTime nowKst = ZonedDateTime.now(zoneId);
	    String now = nowKst.format(formatter);
	
	    int period = 0;
	    
	    if(rqParams != null) {
	    	period = rqParams.getPeriod();
	    } else {
	    	period = (Integer) searchConfig.getPopular().getEs().get("period");
	    }
	    
	    String startDay = nowKst
	             .minusDays(period)
	             //자정 기준으로 시작일 하려면
	             //.withHour(0).withMinute(0).withSecond(0).withNano(0)
	             .format(formatter);

        // range 조건 추가
        RangeQueryBuilder rangeQuery = new RangeQueryBuilder()
                .rangeStr((String) searchConfig.getPopular().getEs().get("range-field"), 
			                		startDay, 
			                		now, 
			                		searchConfig.getRangeOption().get("dateformat"));
        
        boolQuery.filter(rangeQuery);
        
        Map<String, Map<String, Object>> aggs = new HashMap<>();
        
        int size = 0;
	    
        if(rqParams != null) {
        	size = rqParams.getSize();
	    } else {
	    	size = (Integer) searchConfig.getPopular().getEs().get("size");
	    }
	    
        // AggsQueryBuilder 활용
        AggsQueryBuilder aggsBuilder = new AggsQueryBuilder()
                .terms((String) searchConfig.getPopular().getEs().get("agg-field"), size)
                .order((String) searchConfig.getPopular().getEs().get("order-key"), (String) searchConfig.getPopular().getEs().get("order-value"));
       
        aggs.put((String) searchConfig.getPopular().getEs().get("agg-name"), aggsBuilder.build());

        // DSLQueryBuilder 구성
        DSLQueryBuilder dslQueryBuilder = new DSLQueryBuilder()
                .builder()
                .size(0)                      // 문서 결과는 필요 없으므로 0
                .trackTotalHits(false)        // 집계만 필요
                .query(boolQuery)
                .aggregation(aggs)
                .build();

        return dslQueryBuilder;
    }
    
}
