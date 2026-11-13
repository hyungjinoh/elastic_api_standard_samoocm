package com.rayful.elastic.api.standard.controller;

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

import com.rayful.elastic.api.common.config.ConfigValidator;
import com.rayful.elastic.api.common.config.SearchConfig;
import com.rayful.elastic.api.common.exception.SearchException;
import com.rayful.elastic.api.common.service.ICommonService;
import com.rayful.elastic.api.common.util.SearchUtil;
import com.rayful.elastic.api.common.vo.ApiAutoResultVO;
import com.rayful.elastic.api.common.vo.ApiResultExceptVO;
import com.rayful.elastic.api.common.vo.ResponseVO;
import com.rayful.elastic.api.common.vo.SearchResultVO;
import com.rayful.elastic.api.standard.service.IStandardAutoService;
import com.rayful.elastic.api.standard.vo.RequestAutoParamVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 검색 표준 자동완성 API
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping(value = "/api")
public class StandardAutoController {
	
	private static final String SERVICE_TYPE = "auto";
	private static final String RETURN_TYPE = "application/json";
	
	@Autowired
	private SearchConfig searchConfig;
	
	@Autowired
	private IStandardAutoService autoService;
	
	@Autowired
	private ICommonService commonService;
	
	/**
	 * 검색 표준 자동완성 검색 (POST)
	 * @param paramVO
	 * @return
	 * @throws Exception
	 */
	@PostMapping("/auto")
	public ResponseEntity<Object> getSearchList(@RequestBody RequestAutoParamVO paramVO
												, HttpServletRequest request
												, HttpServletResponse response) {
		
		ResponseEntity<Object> responseEntity = null;
		HttpHeaders resHeaders = SearchUtil.getHttpHeaders(RETURN_TYPE);
		
		String traceId = SearchUtil.getTraceId(SERVICE_TYPE);
		
		log.info("=======================");
		log.info("AutoComplete URL: (POST) {}, TraceId: {}", request.getRequestURL(), traceId);
		log.info("=======================");
		log.info("[{}] Param : {}", traceId, paramVO.toString());
		
		// 필수 파라미터 체크 및 기본값 설정
	    String validationError = paramVO.validateAndSetDefaults();
		if (validationError != null) {
	        log.error("[{}] {}", traceId, validationError);
	        return createErrorResponse(HttpStatus.BAD_REQUEST.value(), validationError, resHeaders);
	    }
		
		// 설정파일 체크
	    String validateConfigError = ConfigValidator.validateAutoCompleteConfig(searchConfig, paramVO, traceId);
	    if (validateConfigError != null) {
	    	log.error("[{}] {}", traceId, validateConfigError);
	    	return createErrorResponse(HttpStatus.BAD_REQUEST.value(), validateConfigError, resHeaders);
	    }

		try {
			
			// 자동완성 검색
			SearchResultVO searchResult = this.autoService.getAutoList(paramVO, traceId);
			
			// 자동완성 검색결과 파싱
			ApiAutoResultVO resultVO = this.commonService.resultAutoParse(searchResult, paramVO, RETURN_TYPE, traceId);
			
			ResponseVO responseBody = new ResponseVO(resultVO.getApiResult());
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
