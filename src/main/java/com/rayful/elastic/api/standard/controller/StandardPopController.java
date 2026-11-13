package com.rayful.elastic.api.standard.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rayful.elastic.api.AppScheduler;
import com.rayful.elastic.api.common.config.SearchConfig;
import com.rayful.elastic.api.common.exception.SearchException;
import com.rayful.elastic.api.common.service.ICommonService;
import com.rayful.elastic.api.common.service.PopularKeywordService;
import com.rayful.elastic.api.common.util.SearchUtil;
import com.rayful.elastic.api.common.vo.ApiResultExceptVO;
import com.rayful.elastic.api.common.vo.ResponseVO;
import com.rayful.elastic.api.standard.vo.RequestPopParamVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 검색 표준 인기검색어 API
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping(value = "/api")
public class StandardPopController {
	
	private static final String SERVICE_TYPE = "pop";
	private static final String RETURN_TYPE = "application/json";
	private static final String DB_SOURCE_TYPE = "db";
	private static final String ES_SOURCE_TYPE = "es";
	
	@Autowired
	private SearchConfig searchConfig;
	
	@Autowired
	private ICommonService commonService;
	
	@Autowired
	private PopularKeywordService popularKeywordService;
	
	@Autowired(required = false)
	private AppScheduler keywordScheduler;
	
	/**
	 * 검색 표준  인기검색어 (POST)
	 * @param paramVO
	 * @return
	 * @throws Exception
	 */
	@PostMapping("/pop")
	public ResponseEntity<Object> getPopulaKeywordList(@RequestBody RequestPopParamVO paramVO
											           , HttpServletRequest request
											           , HttpServletResponse response) {

	    ResponseEntity<Object> responseEntity = null;
	    
	    HttpHeaders resHeaders = SearchUtil.getHttpHeaders(RETURN_TYPE);
	    
	    String traceId = SearchUtil.getTraceId(SERVICE_TYPE);

	    log.info("=======================");
	    log.info("Popular Keyword : (POST) {}, TraceId: {}", request.getRequestURL(), traceId);
	    log.info("=======================");
	    log.info("[{}] Param : {}", traceId, paramVO.toString());

	    // 필수 파라미터 체크 및 기본값 설정
	    String validationError = paramVO.validateAndSetDefaults();
	    if (validationError != null && validationError.length() > 0) {
	        log.error("[{}] {}", traceId, validationError);
	        return createErrorResponse(HttpStatus.BAD_REQUEST.value(), validationError, resHeaders);
	    }

	    try {

	    	List<Map<String, Object>> keywordList = new ArrayList<>();
	    	Map<String, Object> dbResultMap = new LinkedHashMap<>();
	    	Map<String, Object> esResultMap = new LinkedHashMap<>();
	    	
	    	if("auto".equalsIgnoreCase(paramVO.getRqType())) { // 스케줄러가 수행한 정보 가져오기
	    		
	    		if ("all".equalsIgnoreCase(searchConfig.getPopular().getSchedulerType())) {
	    			// 스케줄러가 수행한 db 조회 데이터
	    			dbResultMap.put("rqType", paramVO.getRqType());
	    			dbResultMap.put("sourceType", DB_SOURCE_TYPE);
	    			dbResultMap.put("keywordList", keywordScheduler.getDbKeywordList());
	    			
	    			// 스케줄러가 수행한 검색엔진 조회 데이터
	    			esResultMap.put("rqType", paramVO.getRqType());
	    			esResultMap.put("sourceType", ES_SOURCE_TYPE);
	    			esResultMap.put("keywordList", keywordScheduler.getElasticKeywordList());
	        	} else if (DB_SOURCE_TYPE.equalsIgnoreCase(searchConfig.getPopular().getSchedulerType())) {
	        		// 스케줄러가 수행한 db 조회 데이터
	        		dbResultMap.put("rqType", paramVO.getRqType());
	    			dbResultMap.put("sourceType", DB_SOURCE_TYPE);
	    			dbResultMap.put("keywordList", keywordScheduler.getDbKeywordList());
	        	} else {
	        		// 스케줄러가 수행한 검색엔진 조회 데이터
	        		esResultMap.put("rqType", paramVO.getRqType());
	    			esResultMap.put("sourceType", ES_SOURCE_TYPE);
	    			esResultMap.put("keywordList", keywordScheduler.getElasticKeywordList());
	        	}
	    	} else { // 직접 요청해서 가져오기
	    		
	    		if ("all".equalsIgnoreCase(paramVO.getSourceType())) {
	    			// 스케줄러가 수행한 db 조회 데이터
	    			dbResultMap.put("rqType", paramVO.getRqType());
	    			dbResultMap.put("sourceType", DB_SOURCE_TYPE);
	    			dbResultMap.put("keywordList", popularKeywordService.getDbPopularKeywordList(paramVO));
	    			
	    			// 스케줄러가 수행한 검색엔진 조회 데이터
	    			esResultMap.put("rqType", paramVO.getRqType());
	    			esResultMap.put("sourceType", ES_SOURCE_TYPE);
	    			esResultMap.put("keywordList", popularKeywordService.getEsPopularKeywordList(paramVO));
	        	} else if (DB_SOURCE_TYPE.equalsIgnoreCase(paramVO.getSourceType())) {
	        		// 스케줄러가 수행한 db 조회 데이터
	    			dbResultMap.put("rqType", paramVO.getRqType());
	    			dbResultMap.put("sourceType", DB_SOURCE_TYPE);
	    			dbResultMap.put("keywordList", popularKeywordService.getDbPopularKeywordList(paramVO));
	        	} else {
	        		// 스케줄러가 수행한 검색엔진 조회 데이터
	    			esResultMap.put("rqType", paramVO.getRqType());
	    			esResultMap.put("sourceType", ES_SOURCE_TYPE);
	    			esResultMap.put("keywordList", popularKeywordService.getEsPopularKeywordList(paramVO));
	        	}
	    	}
	    	 
	    	if(dbResultMap != null && dbResultMap.size() > 0) {
	    		keywordList.add(dbResultMap);
	    	}
	    	
	    	if(esResultMap != null && esResultMap.size() > 0) {
	    		keywordList.add(esResultMap);
	    	}

	    	log.info("[{}] keywordList : {}", traceId, keywordList);
	    	
	    	ResponseVO responseBody = new ResponseVO(keywordList);
	        responseEntity = new ResponseEntity<>(responseBody, resHeaders, HttpStatus.OK);
	    } catch (SearchException se) {
	        ApiResultExceptVO failVO = this.commonService.searchExceptionResponse(se, request, RETURN_TYPE, SERVICE_TYPE);
	        ResponseVO responseBody = new ResponseVO(failVO);
	        responseEntity = new ResponseEntity<>(responseBody, resHeaders, Integer.valueOf(failVO.getErrorCode()));
	    } catch (Exception e) {
	        ApiResultExceptVO failVO = this.commonService.exceptionResponse(e, request, response, RETURN_TYPE, SERVICE_TYPE);
	        ResponseVO responseBody = new ResponseVO(failVO);
	        responseEntity = new ResponseEntity<>(responseBody, resHeaders, Integer.valueOf(failVO.getErrorCode()));
	    }

	    return responseEntity;
	}
	
	private ResponseEntity<Object> createErrorResponse(int statusCode, String message, HttpHeaders resHeaders) {
	    ApiResultExceptVO failVO = new ApiResultExceptVO(statusCode, message, RETURN_TYPE, SERVICE_TYPE);
	    ResponseVO responseBody = new ResponseVO(failVO);
	    return new ResponseEntity<>(responseBody, resHeaders, statusCode);
	}
	
}
