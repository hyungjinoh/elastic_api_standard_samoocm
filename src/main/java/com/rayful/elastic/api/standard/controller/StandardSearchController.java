package com.rayful.elastic.api.standard.controller;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.rayful.elastic.api.common.config.ConfigValidator;
import com.rayful.elastic.api.common.config.SearchConfig;
import com.rayful.elastic.api.common.exception.SearchException;
import com.rayful.elastic.api.common.service.ICommonService;
import com.rayful.elastic.api.common.util.RecentSearchUtil;
import com.rayful.elastic.api.common.util.SearchUtil;
import com.rayful.elastic.api.common.vo.ApiResultExceptVO;
import com.rayful.elastic.api.common.vo.ApiResultVO;
import com.rayful.elastic.api.common.vo.ResponseVO;
import com.rayful.elastic.api.common.vo.SearchResultVO;
import com.rayful.elastic.api.standard.service.IStandardSearchService;
import com.rayful.elastic.api.standard.vo.RequestSearchParamVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 검색 표준 검색 API
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class StandardSearchController {
	
	private static final String SERVICE_TYPE = "search";
	private static final String RETURN_TYPE = "application/json";
	
	// 통합검색(collapseCd=99999) 시 함께 조회할 메뉴(title 전용) 인덱스의 collapseCd
	// - application.yml의 search.index-name에 별도로 등록 (예: "80000": idx_09_menus)
	private static final String INTEGRATED_COLLAPSE_CD = "99999";
	private static final String MENU_COLLAPSE_CD = "80000";
	
	@Autowired
	private SearchConfig searchConfig;
	
	@Autowired
	private IStandardSearchService searchService;
	
	@Autowired
	private ICommonService commonService;
	
	/**
	 * 검색 표준 버전조회 (GET/POST)
	 * @return
	 */
	@RequestMapping(
		    value = "/", 
		    method = {RequestMethod.GET, RequestMethod.POST}, 
		    produces = MediaType.APPLICATION_JSON_VALUE
		)
	public ResponseEntity<Object> version() {
		
		String zoneId = searchConfig.getProductInfo().get("timezone");
		String parsedBuildTime = null;
		LocalDateTime dateTime = null;
		
		Instant instant = Instant.parse(searchConfig.getProductInfo().get("buildtime"));
		if(zoneId != null && zoneId.length() > 0) {
			// UTC+9 시간 설정
			dateTime = LocalDateTime.ofInstant(instant, ZoneId.of(searchConfig.getProductInfo().get("timezone")));
		} else {
			dateTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
		}
		parsedBuildTime = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		
	    Map<String, Object> productInfoMap = new LinkedHashMap<>();
	    productInfoMap.put("Product Name", searchConfig.getProductInfo().get("prdname"));
	    productInfoMap.put("S/W Name", searchConfig.getProductInfo().get("name"));
	    productInfoMap.put("S/W Version", searchConfig.getProductInfo().get("version"));
	    productInfoMap.put("S/W Build", parsedBuildTime);

	    return new ResponseEntity<>(productInfoMap, HttpStatus.OK);
	}
	
	/**
	 * 검색 표준  검색 (POST)
	 * @param paramVO
	 * @return
	 * @throws Exception
	 */
	@PostMapping("/api/search")
	public ResponseEntity<Object> getSearchList(@RequestBody RequestSearchParamVO paramVO
	                                            , HttpServletRequest request
	                                            , HttpServletResponse response) {

	    ResponseEntity<Object> responseEntity = null;
	    HttpHeaders resHeaders = SearchUtil.getHttpHeaders(RETURN_TYPE);

	    String traceId = SearchUtil.getTraceId(SERVICE_TYPE);

	    log.info("=======================");
	    log.info("Search URL: (POST) {}, TraceId: {}", request.getRequestURL(), traceId);
	    log.info("=======================");
	    log.info("[{}] Param : {}", traceId, paramVO.toString());

	    // 필수 파라미터 체크 및 기본값 설정
	    String validationError = paramVO.validateAndSetDefaults();
	    if (validationError != null && validationError.length() > 0) {
	        log.error("[{}] {}", traceId, validationError);
	        return createErrorResponse(HttpStatus.BAD_REQUEST.value(), validationError, resHeaders);
	    }

	    // 설정파일 체크
	    String validateConfigError = ConfigValidator.validateSearchConfig(searchConfig, paramVO, traceId);
	    if (validateConfigError != null && validateConfigError.length() > 0) {
	    	log.error("[{}] {}", traceId, validateConfigError);
	    	return createErrorResponse(HttpStatus.BAD_REQUEST.value(), validateConfigError, resHeaders);
	    }

	    try {

	        // 검색
	        SearchResultVO searchResult = this.searchService.getSearchList(paramVO, traceId);

	        // 검색 결과 파싱
	        ApiResultVO resultVO = this.commonService.resultSearchParse(searchResult, paramVO, RETURN_TYPE, traceId);

	        // ===== 통합검색(collapseCd=99999)일 때만 메뉴(title 전용) 검색결과를 추가로 조회하여 병합 =====
	        // - 기존 getSearchList()/resultSearchParse() 로직은 위 두 줄 그대로, 전혀 수정하지 않음
	        // - 메뉴 검색은 완전히 별도의 ES 호출(getMenuSearchList/resultMenuSearchParse)로 처리 후 결과만 병합
	        if (INTEGRATED_COLLAPSE_CD.equals(paramVO.getCollapseCd())) {
	            RequestSearchParamVO menuParamVO = buildMenuParamVO(paramVO);
	            SearchResultVO menuSearchResult = this.searchService.getMenuSearchList(menuParamVO, traceId);
	            ApiResultVO menuResultVO = this.commonService.resultMenuSearchParse(menuSearchResult, menuParamVO, RETURN_TYPE, traceId);
	            resultVO = SearchUtil.mergeApiResult(resultVO, menuResultVO);
	        }
	        // ================================================================================

	        // ===== 최근 검색어 저장 및 결과에 포함 (쿠키 식별자 + ES 공유저장, 로드밸런싱 2대에서도 동일하게 동작) =====
	        // - 통합검색(collapseCd=99999), 개별 인덱스 검색(그 외 collapseCd) 모두 이 핸들러를 거치므로
	        //   여기 한 곳에만 추가하면 두 경우 모두 자동으로 적용됨
	        if (paramVO.getKwd() != null && paramVO.getKwd().trim().length() > 0) {
	            String recentSearchId = RecentSearchUtil.resolveRecentSearchId(request, response);
	            List<String> recentKeywords = this.commonService.saveRecentKeyword(recentSearchId, paramVO.getKwd(), traceId);
	            @SuppressWarnings("unchecked")
	            Map<String, Object> apiResultMap = (Map<String, Object>) resultVO.getApiResult();
	            apiResultMap.put("recentKeywords", recentKeywords);
	        }
	        // ================================================================================

	        // 검색 로그 등록
	        if ("Y".equalsIgnoreCase(searchConfig.getDblogUse())) {
	        	// searchConfig.getSiteInfo().get("code")는 설정파일에 지정한 고객사 코드(site-info.code)
	            this.commonService.insertLog(searchConfig.getSiteInfo().get("code"), paramVO, searchResult, traceId);
	        }

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
	
	/**
	 * 통합검색 화면의 개별 섹션(인덱스) 정렬방식 변경 (POST)
	 * - 통합검색 최초 결과에서 특정 섹션의 "정확도/최신순" 버튼 클릭 시 사용
	 * - collapseCd에는 통합검색 코드(99999)가 아니라, 정렬을 바꾸려는 그 섹션의 개별 collapseCd를 담아 요청
	 *   (예: 게시판 섹션이면 게시판의 개별 collapseCd, sort만 바꿔서 재요청)
	 * - 통합검색(getSearchList)과 완전히 분리된 별도 흐름이라, 여기서는 메뉴결과 병합/최근검색어 저장을 하지 않음
	 *   (이미 검색된 키워드에 대한 한 섹션의 정렬만 바꾸는 것이므로 최근검색어를 중복 저장할 필요가 없음)
	 * @param paramVO
	 * @return
	 */
	@PostMapping("/api/search/section")
	public ResponseEntity<Object> getSectionSearchList(@RequestBody RequestSearchParamVO paramVO
	                                            , HttpServletRequest request
	                                            , HttpServletResponse response) {

	    ResponseEntity<Object> responseEntity = null;
	    HttpHeaders resHeaders = SearchUtil.getHttpHeaders(RETURN_TYPE);

	    String traceId = SearchUtil.getTraceId(SERVICE_TYPE);

	    log.info("=======================");
	    log.info("Section Search URL: (POST) {}, TraceId: {}", request.getRequestURL(), traceId);
	    log.info("=======================");
	    log.info("[{}] Param : {}", traceId, paramVO.toString());

	    // 필수 파라미터 체크 및 기본값 설정
	    String validationError = paramVO.validateAndSetDefaults();
	    if (validationError != null && validationError.length() > 0) {
	        log.error("[{}] {}", traceId, validationError);
	        return createErrorResponse(HttpStatus.BAD_REQUEST.value(), validationError, resHeaders);
	    }

	    // 이 엔드포인트는 개별 섹션 전용 - 통합검색 코드(99999)로는 호출 불가
	    if (INTEGRATED_COLLAPSE_CD.equals(paramVO.getCollapseCd())) {
	        String msg = "/api/search/section 은 개별 인덱스(collapseCd)에만 사용할 수 있습니다. 통합검색은 /api/search를 사용하세요.";
	        log.error("[{}] {}", traceId, msg);
	        return createErrorResponse(HttpStatus.BAD_REQUEST.value(), msg, resHeaders);
	    }

	    // 설정파일 체크
	    String validateConfigError = ConfigValidator.validateSearchConfig(searchConfig, paramVO, traceId);
	    if (validateConfigError != null && validateConfigError.length() > 0) {
	    	log.error("[{}] {}", traceId, validateConfigError);
	    	return createErrorResponse(HttpStatus.BAD_REQUEST.value(), validateConfigError, resHeaders);
	    }

	    try {

	        SearchResultVO searchResult = this.searchService.getSectionSearchList(paramVO, traceId);
	        ApiResultVO resultVO = this.commonService.resultSearchParse(searchResult, paramVO, RETURN_TYPE, traceId);

	        // 검색 로그 등록 (통합검색과 동일한 정책)
	        if ("Y".equalsIgnoreCase(searchConfig.getDblogUse())) {
	            this.commonService.insertLog(searchConfig.getSiteInfo().get("code"), paramVO, searchResult, traceId);
	        }

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
	
	/**
	 * 메뉴(title 전용) 검색 (POST)
	 * - title / title.ngram / title.keyword 3가지 서브필드 중 하나라도 매칭되면 결과 포함
	 * - 하이라이팅은 앱 레벨(SearchUtil.getHighlightingStringMulti)로 title 필드에 직접 적용
	 * - 기존 /api/search 엔드포인트(getSearchList)는 전혀 수정하지 않음
	 * @param paramVO
	 * @return
	 */
	@PostMapping("/api/menu/search")
	public ResponseEntity<Object> getMenuSearchList(@RequestBody RequestSearchParamVO paramVO
	                                            , HttpServletRequest request
	                                            , HttpServletResponse response) {

	    ResponseEntity<Object> responseEntity = null;
	    HttpHeaders resHeaders = SearchUtil.getHttpHeaders(RETURN_TYPE);

	    String traceId = SearchUtil.getTraceId(SERVICE_TYPE);

	    log.info("=======================");
	    log.info("Menu Search URL: (POST) {}, TraceId: {}", request.getRequestURL(), traceId);
	    log.info("=======================");
	    log.info("[{}] Param : {}", traceId, paramVO.toString());

	    // 필수 파라미터 체크 및 기본값 설정
	    String validationError = paramVO.validateAndSetDefaults();
	    if (validationError != null && validationError.length() > 0) {
	        log.error("[{}] {}", traceId, validationError);
	        return createErrorResponse(HttpStatus.BAD_REQUEST.value(), validationError, resHeaders);
	    }

	    // 설정파일 체크
	    String validateConfigError = ConfigValidator.validateSearchConfig(searchConfig, paramVO, traceId);
	    if (validateConfigError != null && validateConfigError.length() > 0) {
	    	log.error("[{}] {}", traceId, validateConfigError);
	    	return createErrorResponse(HttpStatus.BAD_REQUEST.value(), validateConfigError, resHeaders);
	    }

	    try {

	        // 메뉴(title 전용) 검색
	        SearchResultVO searchResult = this.searchService.getMenuSearchList(paramVO, traceId);

	        // 메뉴 검색 결과 파싱 (앱 레벨 title 하이라이팅 포함)
	        ApiResultVO resultVO = this.commonService.resultMenuSearchParse(searchResult, paramVO, RETURN_TYPE, traceId);

	        // ===== 최근 검색어 저장 및 결과에 포함 (통합검색과 동일한 쿠키+ES 방식 재사용) =====
	        if (paramVO.getKwd() != null && paramVO.getKwd().trim().length() > 0) {
	            String recentSearchId = RecentSearchUtil.resolveRecentSearchId(request, response);
	            List<String> recentKeywords = this.commonService.saveRecentKeyword(recentSearchId, paramVO.getKwd(), traceId);
	            @SuppressWarnings("unchecked")
	            Map<String, Object> apiResultMap = (Map<String, Object>) resultVO.getApiResult();
	            apiResultMap.put("recentKeywords", recentKeywords);
	        }
	        // ================================================================================

	        // 검색 로그 등록
	        if ("Y".equalsIgnoreCase(searchConfig.getDblogUse())) {
	            this.commonService.insertLog(searchConfig.getSiteInfo().get("code"), paramVO, searchResult, traceId);
	        }

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
	
	/**
	 * 통합검색(paramVO)으로부터 메뉴(title 전용) 검색용 paramVO를 복제 생성
	 * - RequestSearchParamVO.java 자체는 수정하지 않고, 필요한 필드만 옮겨 담는다.
	 * - collapseCd만 메뉴 전용 코드(MENU_COLLAPSE_CD)로 교체
	 * - size는 클라이언트가 보낸 값이 아니라 통합검색 미리보기 개수(search.integrated-preview-size)로 강제
	 *   (메뉴 섹션도 다른 섹션들과 동일하게 "3개씩" 노출되어야 하므로, StandardSearchServiceImpl의
	 *    buildPreviewParamVO()와 동일한 규칙을 여기서도 적용)
	 */
	private RequestSearchParamVO buildMenuParamVO(RequestSearchParamVO paramVO) {
		RequestSearchParamVO menuParamVO = new RequestSearchParamVO();
		menuParamVO.setKwd(paramVO.getKwd());
		menuParamVO.setKwdOption(paramVO.getKwdOption());
		menuParamVO.setReSrchYn(paramVO.getReSrchYn());
		menuParamVO.setPrevKwd(paramVO.getPrevKwd());
		menuParamVO.setSort(paramVO.getSort());
		menuParamVO.setSize(searchConfig.getIntegratedPreviewSize());
		menuParamVO.setPage(1);
		menuParamVO.setCollapseCd(MENU_COLLAPSE_CD);
		return menuParamVO;
	}
	
	/**
	 * 최근 검색어 목록 조회 (GET)
	 * - 검색을 실행하지 않고, 쿠키(RSID)로 식별되는 최근 검색어만 조회할 때 사용
	 * (검색 화면 최초 진입 시 등)
	 */
	@GetMapping("/api/recent/search")
	public ResponseEntity<Object> getRecentKeywords(HttpServletRequest request, HttpServletResponse response) {
		String traceId = SearchUtil.getTraceId(SERVICE_TYPE);
		String recentSearchId = RecentSearchUtil.resolveRecentSearchId(request, response);
		
		Map<String, Object> resultMap = new LinkedHashMap<>();
		resultMap.put("recentKeywords", this.commonService.getRecentKeywordList(recentSearchId, traceId));
		return new ResponseEntity<>(resultMap, HttpStatus.OK);
	}
	
	/**
	 * 최근 검색어 개별 삭제 (DELETE)
	 */
	@DeleteMapping("/api/recent/search/{keyword}")
	public ResponseEntity<Object> deleteRecentKeyword(@PathVariable String keyword
	                                            , HttpServletRequest request
	                                            , HttpServletResponse response) {
		String traceId = SearchUtil.getTraceId(SERVICE_TYPE);
		// [시큐어코딩] 경로변수 입력 검증(과도한 길이 차단 - 자원 소모/오용 방지). 검색어 규칙(≤100)과 정합.
		if (keyword == null || keyword.isEmpty() || keyword.length() > 100) {
			HttpHeaders resHeaders = SearchUtil.getHttpHeaders(RETURN_TYPE);
			return createErrorResponse(HttpStatus.BAD_REQUEST.value(), "keyword는 1~100자여야 합니다.", resHeaders);
		}
		String recentSearchId = RecentSearchUtil.resolveRecentSearchId(request, response);
		
		Map<String, Object> resultMap = new LinkedHashMap<>();
		resultMap.put("recentKeywords", this.commonService.deleteRecentKeyword(recentSearchId, keyword, traceId));
		return new ResponseEntity<>(resultMap, HttpStatus.OK);
	}
	
	/**
	 * 최근 검색어 전체 삭제 (DELETE)
	 */
	@DeleteMapping("/api/recent/search")
	public ResponseEntity<Object> clearRecentKeywords(HttpServletRequest request, HttpServletResponse response) {
		String traceId = SearchUtil.getTraceId(SERVICE_TYPE);
		String recentSearchId = RecentSearchUtil.resolveRecentSearchId(request, response);
		
		this.commonService.clearRecentKeywords(recentSearchId, traceId);
		
		Map<String, Object> resultMap = new LinkedHashMap<>();
		resultMap.put("recentKeywords", new java.util.ArrayList<String>());
		return new ResponseEntity<>(resultMap, HttpStatus.OK);
	}
	
	private ResponseEntity<Object> createErrorResponse(int statusCode, String message, HttpHeaders resHeaders) {
	    ApiResultExceptVO failVO = new ApiResultExceptVO(statusCode, message, RETURN_TYPE, SERVICE_TYPE);
	    ResponseVO responseBody = new ResponseVO(failVO);
	    return new ResponseEntity<>(responseBody, resHeaders, statusCode);
	}
	
}
