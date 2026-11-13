package com.rayful.elastic.api.common.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.rayful.elastic.api.common.builder.DSLQueryBuilder;
import com.rayful.elastic.api.common.builder.TermQueryBuilder;
import com.rayful.elastic.api.common.config.SearchConfig;
import com.rayful.elastic.api.common.config.WebClientHelper;
import com.rayful.elastic.api.common.dao.ICommonDao;
import com.rayful.elastic.api.common.exception.QueryBuilderException;
import com.rayful.elastic.api.common.exception.SearchErrorCode;
import com.rayful.elastic.api.common.exception.SearchException;
import com.rayful.elastic.api.common.util.GsonUtils;
import com.rayful.elastic.api.common.util.LoggingUtil;
import com.rayful.elastic.api.common.util.SearchUtil;
import com.rayful.elastic.api.common.vo.ApiAutoResultVO;
import com.rayful.elastic.api.common.vo.ApiPopResultVO;
import com.rayful.elastic.api.common.vo.ApiResultExceptVO;
import com.rayful.elastic.api.common.vo.ApiResultVO;
import com.rayful.elastic.api.common.vo.SearchResultVO;
import com.rayful.elastic.api.standard.vo.RequestAutoParamVO;
import com.rayful.elastic.api.standard.vo.RequestPopParamVO;
import com.rayful.elastic.api.standard.vo.RequestSearchParamVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
@Primary
public class CommonServiceImpl implements ICommonService {
	
	@Value("${elasticsearch.host}")
    private String baseUrl;
	
	@Value("${webclient.ssl.header-id-pw:}")
	private String basicAuth;
	
	@Value("${webclient.ssl.header-api-key:}")
	private String esApiKey;
	  
	@Autowired
    private WebClientHelper webClientHelper;
	
	private final ObjectMapper objectMapper;
	
	@Autowired
	private SearchConfig searchConfig;
	
	@Autowired
	private ICommonDao commonDao;
	
	private static final String SEARCH_SEPARATOR01 = ","; 			// 설정파일 구분자(1차구분자)
	private static final String SEARCH_SEPARATOR02 = "\\."; 		// 설정파일 구분자(하이라이팅 배열 구분자)
	private static final String ERROR_SEPARATOR01 = "###"; 			// 에러메세지 구분자
	private static final char POP_SEPARATOR_ASCII = 27;
	
	/**
	 * WebClient 검색 호출
	 */
	@Override
    public SearchResultVO search(String searchUri, String query) {
        SearchResultVO searchResult = new SearchResultVO();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (this.esApiKey != null && !this.esApiKey.trim().isEmpty()) {
        	// esApiKey 값이 존재하면 Authorization 헤더 추가
            headers.set(HttpHeaders.AUTHORIZATION, "ApiKey " + this.esApiKey);
        } else {
        	// basicAuth 값이 존재하면 Authorization 헤더 추가
            if (this.basicAuth != null && !this.basicAuth.trim().isEmpty()) {
                String basicAuthHeader = "Basic " + Base64.getEncoder().encodeToString(this.basicAuth.getBytes(StandardCharsets.UTF_8));
                headers.set(HttpHeaders.AUTHORIZATION, basicAuthHeader);
            }
        }
        
        if (query != null) {
            Mono<Map<String, Object>> response = this.webClientHelper.get(baseUrl)
                .post()
                .uri(searchUri)
                .headers(httpHeaders -> httpHeaders.addAll(headers)) // 모든 헤더 한번에 추가
                .bodyValue(query)
                .retrieve()
                .onStatus(
                    status -> status.is4xxClientError() || status.is5xxServerError(),
                    clientResponse -> clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                        try {
                            JsonNode root = objectMapper.readTree(errorBody);
                            String statusValue = root.path("status").asText();
                            String errorValue = root.path("error").get("root_cause").get(0).get("reason").asText();
                            return Mono.error(new RuntimeException(statusValue + ERROR_SEPARATOR01 + "[" + statusValue + "] " + errorValue));
                        } catch (Exception e) {
                            return Mono.error(e);
                        }
                    })
                )
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
            
            Map<String, Object> responseMap = response.block();
            searchResult.setResult(responseMap);
        }
        
        return searchResult;
    }
	
	/**
     * 로그 등록
     */
	@Override
	@Async("asyncExecutor")
    public CompletableFuture<Void> insertLog(String appId, RequestSearchParamVO paramVO, SearchResultVO searchResult, String traceId) {
        try {
            Map<String, Object> logMap = new HashMap<>();
            
            logMap.put("appId", appId);
            logMap.put("secId", paramVO.getCollapseCd());
            logMap.put("srchDt", Timestamp.valueOf(LocalDateTime.now())); // 현재 시간
            logMap.put("srchKeyword", searchResult.getKeyword());
            logMap.put("resCount", searchResult.getTotalCnt());
            logMap.put("elapse", searchResult.getTook());
            
            this.commonDao.insertSearchLog(logMap);
            log.info("[{}] InsertLog: {}", traceId, logMap);
        } catch (Exception e) {
        	log.error("[{}] Failed to insert search log: {}", traceId, e.getMessage(), e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
	
    /**
     * 검색 예외 처리
     */
    @Override
    public ApiResultExceptVO searchExceptionResponse(SearchException se, HttpServletRequest request, String returnType, String serviceType) {
        return ApiResultExceptVO.builder()
            .serviceType(serviceType)
            .errorCode(se.getErrorCode())
            .errorCodeSub(se.getErrorCodeSub())
            .errorMessage(se.getErrorMessage())
            .errorMessageSubList(se.getErrorMessageSubList())
            .returnType(returnType)
            .build();
    }
	
    /**
     * 예외 처리
     */
    @Override
    public ApiResultExceptVO exceptionResponse(Exception e, HttpServletRequest request, HttpServletResponse response, String returnType, String serviceType) {
        return ApiResultExceptVO.builder()
            .serviceType(serviceType)
            .errorCode(SearchErrorCode.DEFAULT_EXCEPTION.getErrorCode() + SearchErrorCode.DEFAULT_EXCEPTION.getErrorCodeSub())
            .errorCodeSub(SearchErrorCode.DEFAULT_EXCEPTION.getErrorCodeSub())
            .errorMessage(e.getMessage())
            .returnType(returnType)
            .build();
    }
    
    /**
     * 예외 메시지를 분석하여 SearchException을 던지는 메소드
     */
    public void handleSearchException(Exception e) {
        String errorStr = e.getMessage();
        log.error(errorStr, e);

        List<String> subErrorMsg = new ArrayList<>();

        if (errorStr.indexOf(ERROR_SEPARATOR01) > -1) { // 검색엔진 오류 생성 시
            String[] tempArray = errorStr.split(ERROR_SEPARATOR01);
            if (tempArray != null && tempArray.length > 1) {
                subErrorMsg.add(tempArray[1]);
                throw new SearchException(
                    Integer.parseInt(tempArray[0]),
                    SearchErrorCode.SEARCH_EXCPETION.getErrorCode(),
                    SearchErrorCode.SEARCH_EXCPETION.getErrorCodeSub(),
                    SearchErrorCode.SEARCH_EXCPETION.getErrorMessage(),
                    subErrorMsg
                );
            }
        } else { // 커넥션 관련 오류
            if (errorStr.indexOf("connection timed out") > -1 || errorStr.indexOf("Connection refused") > -1) {
                subErrorMsg.add("[" + SearchErrorCode.TIMEOUT_EXCPETION.getErrorCodeSub() + "] " + errorStr);
                throw new SearchException(
                    Integer.valueOf(SearchErrorCode.TIMEOUT_EXCPETION.getErrorCodeSub()),
                    SearchErrorCode.TIMEOUT_EXCPETION.getErrorCode(),
                    SearchErrorCode.TIMEOUT_EXCPETION.getErrorCodeSub(),
                    SearchErrorCode.TIMEOUT_EXCPETION.getErrorMessage(),
                    subErrorMsg
                );
            } else { // 기타 오류
            	subErrorMsg.add("[" + SearchErrorCode.DEFAULT_EXCEPTION.getErrorCodeSub() + "] " + errorStr);
                throw new SearchException(
                    Integer.valueOf(SearchErrorCode.DEFAULT_EXCEPTION.getErrorCodeSub()),
                    SearchErrorCode.DEFAULT_EXCEPTION.getErrorCode(),
                    SearchErrorCode.DEFAULT_EXCEPTION.getErrorCodeSub(),
                    SearchErrorCode.DEFAULT_EXCEPTION.getErrorMessage(),
                    subErrorMsg
                );
            }
        }
    }
	
	/**
	 * 검색결과 파싱
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ApiResultVO resultSearchParse(SearchResultVO searchResult, RequestSearchParamVO paramVO, String returnType, String traceId) {
		
		ApiResultVO resultVO = null;
		int idx = 0; // for index
		int cnt = 0; // 검색 결과 카운트
		int took = 0; // 검색 시간
		int totalTook = 0; // 전체 검색 시간
		int totalCnt = 0; // 전체 결과 건수
		int sectionSize = 0; // 섹션 사이즈
		int status = 0; // 상태 코드
		
		List<Map<String, Object>> resultList = new ArrayList<Map<String,Object>>();
		Map<String, Object> sectionMap = null;
		List<Map<String, Object>> resultResponse = null;
		Map<String, Object> responseMap = null;
		Map<String, Object> hitsMap = null;
		Map<String, Object> sourceMap = null;
		List<Map<String, Object>> hitsList = null;
		List<Map<String, Object>> docList = null;
		Map<String, Object> totalMap = null;
		Map<String, Object> logSectionMap = null;
		List<Map<String, Object>> logResultList = new ArrayList<Map<String,Object>>();
		
		List<Integer> statusList = new ArrayList<Integer>(); // 에러 발생시 에러 상태 리스트
		List<String> errorMessageList = new ArrayList<String>();
		Map<String, Object> errorMap = null;
		String errorReason = null;
		String errorMessage = null;
		
		Map<String, Object> aggregationMap = null;
		Map<String, Object> highlightMap = null;
		List<Map<String, Object>> aggResults = null;
		
		StringBuffer sb = new StringBuffer();
		
		if(searchResult != null && searchResult.getResult() != null) {
			
			// [우장각 커스텀] "collapseCd==99999" 대신 "이 collapseCd 의 index-name 이 실제로 콤마를
			// 포함해 멀티 인덱스(_msearch)로 질의했는지"로 응답 파싱 분기도 일반화.
			// (StandardSearchServiceImpl.getSearchList() 의 질의 생성 쪽과 반드시 같은 기준이어야
			//  responses 배열 길이와 indices 배열 길이가 어긋나 IndexOutOfBoundsException 이 안 난다.)
			String[] indices = searchConfig.getIndexName().get(paramVO.getCollapseCd()).split(SEARCH_SEPARATOR01);
			boolean isMultiIndex = indices.length > 1;

			if(isMultiIndex) {
				resultResponse = (List<Map<String, Object>>) searchResult.getResult().get("responses");
			} else {
				resultResponse = new ArrayList<Map<String,Object>>();
				resultResponse.add(searchResult.getResult());
			}

	        for (String index : indices) {
	        	
				responseMap = resultResponse.get(idx++);
				took = 0;
				
				if(responseMap != null && responseMap.get("hits") != null) {
					
					try {
						
						hitsMap = (Map<String, Object>) responseMap.get("hits");
						hitsList = (List<Map<String,Object>>) hitsMap.get("hits");
						totalMap = (Map<String, Object>) hitsMap.get("total");
						status = 200;
						cnt = (int) totalMap.get("value");
						totalCnt += cnt;
						sectionSize++;
						took = (int) responseMap.get("took");
						totalTook += took;
						docList = new ArrayList<Map<String,Object>>();
						
						if(hitsList != null) {
							
							for (Map<String, Object> hits : hitsList) {
								
								sourceMap = (Map<String, Object>) hits.get("_source");
								
								if(sourceMap != null) {
									
									for (String key : sourceMap.keySet()) {
										Object value = sourceMap.get(key);
										// 검색표준 UI에서 Integer 타입을 String으로 사용 할 경우가 있어 공통적으로 String 형으로 처리
										if(value instanceof Integer) {
											sourceMap.put(key, String.valueOf(value));
										} else if(value instanceof String) {
											String strValue = (String) value;
											
											if ("attach_body".equals(key) || "content".equals(key)) { // 첨부내용과 본문만 대상으로 처리
												if(strValue.length() > searchConfig.getContentMaxSize()) {
													// 최대 본문 사이즈 근처의 공백에서 문자열 자르기
													int spaceIndex = strValue.lastIndexOf(' ', searchConfig.getContentMaxSize());
													if (spaceIndex == -1) {
														sourceMap.put(key, strValue.substring(0, searchConfig.getContentMaxSize()));
													} else {
														sourceMap.put(key, strValue.substring(0, spaceIndex));
													}
												}
											} 
										}
									}
								}
								
								// Highlighting 결과 파싱 및 적용
								if(hits.get("highlight") != null) {
									
									highlightMap = (Map<String, Object>) hits.get("highlight");
									
									for (String key : highlightMap.keySet()) {
										// highlightMap.get(key)는 List<String> 형식이므로, 이를 문자열로 변환하여 적용
										String highlightedString = String.join("", (List<String>) highlightMap.get(key));
										
										if(sourceMap.containsKey(key)) {
											
											String highLightSub = searchConfig.getHighlightSub().get(index);
											
											if(highLightSub != null && highLightSub.length() > 0) {
												String [] arrHighLightSub = highLightSub.split(SEARCH_SEPARATOR01);
												
												// application.yml 의 highlight-sub에 지정된 필드를 별도로 저장
								                if (Arrays.asList(arrHighLightSub).contains(key)) {
								                    sourceMap.put(key +  searchConfig.getHighlightAddField(), highlightedString);
								                } else {
								                    sourceMap.put(key, highlightedString);
								                }
											} else {
												sourceMap.put(key, highlightedString);
											}
										} else {
											
											if(key.contains(".")) {
												String [] filedNames = key.split(SEARCH_SEPARATOR02);
												String baseFieldName = filedNames[0];
												
												// 배열구조 하이라이팅 처리 
												if(sourceMap.get(baseFieldName) instanceof List) {
													List<Map<String, Object>> sourceSubFieldList = (List<Map<String, Object>>) sourceMap.get(baseFieldName);
													List<String> highlightedList = (List<String>) highlightMap.get(key);

													for (Map<String, Object> sourceSubFieldMap : sourceSubFieldList) {
									                    String sourceSubFieldValue = (String) sourceSubFieldMap.get(filedNames[1]);

									                    if (sourceSubFieldValue != null) {
									                    	for (String highlight : highlightedList) {
										                    	
									                    		String removeTagStr = highlight
									                        			.replace((String)searchConfig.getHighlightOption().get("pre_tags"), "")
									                        			.replace((String)searchConfig.getHighlightOption().get("post_tags"), "");
									                        	
									                            if (sourceSubFieldValue.contains(removeTagStr)) { 
									                            	sourceSubFieldMap.put(filedNames[1], highlight);
									                            }
										                    }
									                    }
									                }
													
												} else {
													if(!highlightMap.containsKey(baseFieldName) && sourceMap.containsKey(baseFieldName)) {
														sourceMap.put(baseFieldName, highlightedString);
													}
												}
											} else { // attach_body 의 경우 _source에서 제거하고 해당 조건문으로 attach_body_highlight 필드로 제공
												sourceMap.put(key +  searchConfig.getHighlightAddField(), highlightedString);
											}
										}
									}
								}
								
								docList.add(sourceMap);
							}
							
							// Aggregation 결과 파싱
							if(responseMap.get("aggregations") != null) {
							    aggregationMap = (Map<String, Object>) responseMap.get("aggregations");

							    if(aggregationMap != null && aggregationMap.size() > 0) {
							        aggResults = new ArrayList<>();
							        for (String aggKey : aggregationMap.keySet()) {
							            Map<String, Object> aggResult = new LinkedHashMap<>();
							            aggResult.put("field", aggKey);

							            // name 정보 추가
							            Map<String, String> aggsConfig = searchConfig.getAggs().get(index);
							            if (aggsConfig != null && aggsConfig.get("name") != null) {
							                String nameStr = (String) aggsConfig.get("name");
							                String[] names = nameStr.split(SEARCH_SEPARATOR01);
							                String fieldStr = (String) aggsConfig.get("field");
							                String[] fields = fieldStr.split(SEARCH_SEPARATOR01);
							                for (int i = 0; i < fields.length; i++) {
							                    if (fields[i].trim().equals(aggKey)) {
							                        aggResult.put("name", names[i].trim());
							                        break;
							                    }
							                }
							            }

							            Map<String, Object> aggValue = (Map<String, Object>) aggregationMap.get(aggKey);
							            List<Map<String, Object>> buckets = (List<Map<String, Object>>) aggValue.get("buckets");

							            List<Map<String, Object>> bucketList = new ArrayList<>();
							            for (Map<String, Object> bucket : buckets) {
							                Map<String, Object> bucketMap = new HashMap<>();
							                bucketMap.put("key", bucket.get("key"));
							                bucketMap.put("doc_count", bucket.get("doc_count"));
							                bucketList.add(bucketMap);
							            }

							            aggResult.put("buckets", bucketList);
							            aggResults.add(aggResult);
							        }
							    }
							}
						}
					
						if(sb != null && sb.length() > 0) {
							sb.append(",");
						} 
						
						sb.append("[index:").append(index).append(", sectionCnt:").append(cnt).append(", status:").append(status).append("]");
					} catch (Exception e) {
						handleSearchException(e);
					}
				} else {
					// 에러 발생 시
					if(responseMap.get("error") != null) {
						
						errorMap = (Map<String, Object>) responseMap.get("error");
						status = (int) responseMap.get("status");
						
						List<Map<String, Object>> tempList = (List<Map<String, Object>>) errorMap.get("root_cause");
						
						if(tempList != null && tempList.size() > 0) {
							Map<String, Object> tempMap = tempList.get(0);
							
							errorReason = (String) tempMap.get("reason");
							errorMessage = "[" + status + "] " + errorReason;
							
							if(errorMessageList.indexOf(errorMessage) < 0) {
								errorMessageList.add(errorMessage);
								log.error("[{}] Search error status : {}", traceId, status);
								log.error("[{}] Search error message : {}", traceId, errorMessage);
							}
							
							if(sb != null && sb.length() > 0) {
								sb.append(",");
							} 
						}
						
						sb.append("[index:").append(index).append(", sectionCnt:").append(cnt).append(", status:").append(status).append("]");
					}
					
					docList = null;
					cnt = 0;
					aggResults = null;
				}
				
				sectionMap = new HashMap<String, Object>();
				sectionMap.put("indexName", index);
				sectionMap.put("sectionName", searchConfig.getSectionName().get(index));
				sectionMap.put("sectionCode", searchConfig.getSectionCode().get(index));
				sectionMap.put("sectionCnt", cnt);
				sectionMap.put("docList", docList);
				sectionMap.put("aggregations", aggResults);
				aggResults = null;
				
				resultList.add(sectionMap);
				statusList.add(status);
				
				logSectionMap = new HashMap<String, Object>();
				logSectionMap.put("status", status);
				logSectionMap.put("resultCnt", cnt);
				logResultList.add(logSectionMap);
			}
		 }
		
		// 모든 샤드 에러 일 경우
		if(statusList.size() > 0 && statusList.indexOf(200) < 0) {
			throw new SearchException(
					status
					, SearchErrorCode.SEARCH_EXCPETION.getErrorCode()
					, SearchErrorCode.SEARCH_EXCPETION.getErrorCodeSub()
					, SearchErrorCode.SEARCH_EXCPETION.getErrorMessage()
					, errorMessageList
			);
		}
		
		resultVO = ApiResultVO.builder()
				.searchSectionList(resultList)
				.kwd(paramVO.getKwd())
				.prevKwd(paramVO.getPrevKwd())
				.totalCnt(totalCnt)
				.sectionSize(sectionSize)
				.returnType(returnType)
				.reqTime(SearchUtil.getDate("yyyy/MM/dd HH:mm:ss"))
				.build();

		// for log
		searchResult.setTook(totalTook);
		searchResult.setTotalCnt(totalCnt);
		
		Map<String, Object> logMap = Collections.synchronizedMap(new LinkedHashMap<String, Object>());
		logMap.put("took", totalTook);
		logMap.put("totalCnt", totalCnt);
		
		log.debug("[{}] Result : {}", traceId, resultVO.toString());
		log.info("[{}] Result : {}", traceId, sb.toString());
		
		return resultVO;
	}
	
	/**
	 * 자동완성결과 파싱
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ApiAutoResultVO resultAutoParse(SearchResultVO searchResult, RequestAutoParamVO paramVO, String returnType, String traceId) {
		
		ApiAutoResultVO resultVO = null;
		int idx = 0; // for index
		int cnt = 0; // 검색 결과 카운트
		int took = 0; // 검색 시간
		int totalTook = 0; // 전체 검색 시간
		int totalCnt = 0; // 전체 결과 건수
		int sectionSize = 0; // 섹션 사이즈
		int status = 0; // 상태 코드
		
		Map<String, Object> sectionMap = null;
		List<Map<String, Object>> resultResponse = null;
		Map<String, Object> responseMap = null;
		Map<String, Object> hitsMap = null;
		Map<String, Object> sourceMap = null;
		List<Map<String, Object>> hitsList = null;
		List<Map<String, Object>> docList = null;
		Map<String, Object> totalMap = null;
		Map<String, Object> logSectionMap = null;
		
		Map<String, Object> highlightMap = null;
		
		List<String> errorMessageList = new ArrayList<String>();
		Map<String, Object> errorMap = null;
		String errorReason = null;
		String errorMessage = null;
		
		StringBuffer sb = new StringBuffer();
		
		if(searchResult != null && searchResult.getResult() != null) {
			
			resultResponse = new ArrayList<Map<String,Object>>();
			resultResponse.add(searchResult.getResult());
			
			String index = searchConfig.getIndexName().get(paramVO.getCollapseCd());

	        	
			responseMap = resultResponse.get(idx++);
			took = 0;
			
			if(responseMap != null && responseMap.get("hits") != null) {
				
				try {
					hitsMap = (Map<String, Object>) responseMap.get("hits");
					hitsList = (List<Map<String,Object>>) hitsMap.get("hits");
					totalMap = (Map<String, Object>) hitsMap.get("total");
					status = 200;
					cnt = (int) totalMap.get("value");
					totalCnt += cnt;
					sectionSize++;
					took = (int) responseMap.get("took");
					totalTook += took;
					docList = new ArrayList<Map<String,Object>>();
					
					if(hitsList != null) {
						for (Map<String, Object> hits : hitsList) {
							
							sourceMap = (Map<String, Object>) hits.get("_source");
							
							if(sourceMap != null) {
								// Highlighting 결과 파싱 및 적용
								if(hits.get("highlight") != null) {
									
									highlightMap = (Map<String, Object>) hits.get("highlight");
									
									for (String key : highlightMap.keySet()) {
										// highlightMap.get(key)는 List<String> 형식이므로, 이를 문자열로 변환하여 적용
										String highlightedString = String.join("", (List<String>) highlightMap.get(key));
										String highLightSub = searchConfig.getHighlightSub().get(index);
										
										if(sourceMap.containsKey(key)) {
											
											if(highLightSub != null && highLightSub.length() > 0) {
												String [] arrHighLightSub = highLightSub.split(SEARCH_SEPARATOR01);
												
												// application.yml 의 highlight-sub에 지정된 필드를 별도로 저장
								                if (Arrays.asList(arrHighLightSub).contains(key)) {
								                    sourceMap.put(key +  searchConfig.getHighlightAddField(), highlightedString);
								                } else {
								                    sourceMap.put(key, sourceMap.get(key));
								                }
											}
										} else {
											if(key.contains(".")) {
												
												String [] filedNames = key.split(SEARCH_SEPARATOR02);
												String baseFieldName = filedNames[0];
												
												// [방어 코드] kwd 필수 검증을 없애면서 paramVO.getKwd() 가 null 일 수 있게 됐다.
												// getHighlightingString/engToKor 모두 keyword.length()/replaceAll() 처럼
												// null 이면 바로 NPE 나는 메서드라 빈 문자열로 치환해서 넘긴다(검색어가 없으니
												// 하이라이트도 당연히 없는 게 맞고, 빈 문자열을 넘기면 이 유틸들은 안전하게
												// 아무것도 강조하지 않고 원본을 그대로 돌려준다).
												String safeKwd = paramVO.getKwd() == null ? "" : paramVO.getKwd();
												String compareHighlightStr = SearchUtil.getHighlightingString(String.valueOf(sourceMap.get(baseFieldName)), safeKwd,
														SearchUtil.engToKor(safeKwd),
														(String) searchConfig.getHighlightOption().get("pre_tags"), 
														(String) searchConfig.getHighlightOption().get("post_tags"));
												
												if(!highlightMap.containsKey(baseFieldName) && sourceMap.containsKey(baseFieldName)) {
													
													if(highLightSub != null && highLightSub.length() > 0) {
														String [] arrHighLightSub = highLightSub.split(SEARCH_SEPARATOR01);
														
														// application.yml 의 highlight-sub에 지정된 필드를 별도로 저장
										                if (Arrays.asList(arrHighLightSub).contains(baseFieldName)) {
										                    sourceMap.put(baseFieldName +  searchConfig.getHighlightAddField(), compareHighlightStr);
										                } else {
										                    sourceMap.put(baseFieldName, sourceMap.get(key));
										                }
													} else {
														sourceMap.put(baseFieldName, compareHighlightStr);
													}
												}
											}
										}
									}
								} 
							}
							
							docList.add(sourceMap);
						}
					}
				
					if(sb != null && sb.length() > 0) {
						sb.append(",");
					} 
					
					sb.append("[index:").append(index).append(", totalCnt:").append(cnt).append(", status:").append(status).append("]");
				} catch (Exception e) {
					handleSearchException(e);
				}
			} else {
				// 에러 발생 시
				if(responseMap.get("error") != null) {
					
					errorMap = (Map<String, Object>) responseMap.get("error");
					status = (int) responseMap.get("status");
					
					List<Map<String, Object>> tempList = (List<Map<String, Object>>) errorMap.get("root_cause");
					
					if(tempList != null && tempList.size() > 0) {
						Map<String, Object> tempMap = tempList.get(0);
						
						errorReason = (String) tempMap.get("reason");
						errorMessage = "[" + status + "] " + errorReason;
						
						if(errorMessageList.indexOf(errorMessage) < 0) {
							errorMessageList.add(errorMessage);
							log.error("[{}] Search error status : {}", traceId, status);
							log.error("[{}] Search error message : {}", traceId, errorMessage);
						}
						
						if(sb != null && sb.length() > 0) {
							sb.append(",");
						} 
					}
					
					sb.append("[index:").append(index).append(", sectionCnt:").append(cnt).append(", status:").append(status).append("]");
				}
				
				throw new SearchException(
						status
						, SearchErrorCode.SEARCH_EXCPETION.getErrorCode()
						, SearchErrorCode.SEARCH_EXCPETION.getErrorCodeSub()
						, SearchErrorCode.SEARCH_EXCPETION.getErrorMessage()
						, errorMessageList
				);
			}
			
			sectionMap = new LinkedHashMap<String, Object>();
			sectionMap.put("indexName", index);
			sectionMap.put("sectionName", searchConfig.getSectionName().get(index));
			sectionMap.put("sectionCode", searchConfig.getSectionCode().get(index));
			sectionMap.put("sectionCnt", cnt);
			sectionMap.put("docList", docList);
			
			//resultList.add(sectionMap);
			//statusList.add(status);
			
			logSectionMap = new HashMap<String, Object>();
			logSectionMap.put("status", status);
			logSectionMap.put("resultCnt", cnt);
		}
		
		resultVO = ApiAutoResultVO.builder()
				.autoMap(sectionMap)
				.totalCnt(totalCnt)
				.sectionSize(sectionSize)
				.returnType(returnType)
				.reqTime(SearchUtil.getDate("yyyy/MM/dd HH:mm:ss"))
				.build();
		
		// for log
		searchResult.setTook(totalTook);
		searchResult.setTotalCnt(totalCnt);
		
		Map<String, Object> logMap = Collections.synchronizedMap(new LinkedHashMap<String, Object>());
		logMap.put("took", totalTook);
		logMap.put("totalCnt", totalCnt);
		
		log.debug("[{}] Result : {}", traceId, resultVO.toString());
		log.info("[{}] Result : {}", traceId, sb.toString());
		
		return resultVO;
	}
	
	/**
	 * 인기검색어 파싱 - single
	 */
	public ApiPopResultVO resultPopParse(File popularKeywordFile, RequestPopParamVO paramVO, String returnType, String traceId) {
		ApiPopResultVO resultVO = null;
		
		Map<String, Object> popularMap = new LinkedHashMap<String, Object>();
		Map<String, Object> rankMap = null;
		List<Map<String, Object>> rankList = new ArrayList<Map<String,Object>>();
		String filePath = null;
		String fileEncoding = null;

		String popData = null;
		int popListCnt = 0;
		
		if(popularKeywordFile != null) {
			String fileName = popularKeywordFile.getName();
			
			if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
				throw new IllegalArgumentException("Invalid filename");
			}
			
			filePath = popularKeywordFile.getAbsolutePath();
			fileEncoding = SearchUtil.getFileEncoding(filePath);
			Charset charset = Charset.forName(fileEncoding);
			
			try (
				FileInputStream fileInputStream = new FileInputStream(popularKeywordFile);
				InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, charset);
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader)
			) {
				String line;
				String aStats[] = null;
				int idx = 0;
				
				while ((line = bufferedReader.readLine()) != null) {
					aStats = line.split(Character.toString(POP_SEPARATOR_ASCII));
					
					if(idx++ == 0) {
						if(aStats.length == 6) {
							popularMap.put("crtdate", aStats[0]);
							popularMap.put("logdate", aStats[1]);
							popularMap.put("cycle", aStats[2]);
							popularMap.put("type", aStats[3]);
							popularMap.put("field", aStats[4]);
							popularMap.put("totcnt", aStats[5]);
						}
						
					} else {
						if(aStats.length == 3) {
							popData = aStats[1];
							
							rankMap = new LinkedHashMap<String, Object>();
							rankMap.put("id", idx - 1);
							rankMap.put("count", aStats[0]);
							rankMap.put("percnt", aStats[2]);
							rankMap.put("name", popData);
							rankList.add(rankMap);
						}
					}
					
					if(idx > paramVO.getSize()) {
						break;
					}
				}
				
				popListCnt = rankList.size();
				popularMap.put("ranking", rankList);
				
			} catch (IOException e) {
				LoggingUtil.error(e);
			}
		}
		
		resultVO = ApiPopResultVO.builder()
				.popKeywordMap(popularMap)
				.returnType(returnType)
				.reqTime(SearchUtil.getDate("yyyy/MM/dd HH:mm:ss"))
				.filePath(filePath)
				.build();
		
		log.info("[{}] Status: 200, ResultCnt: {}", traceId, popListCnt);
		
		return resultVO;
	}
	
	// ================================================================================
	// 메뉴(title 전용) 검색결과 파싱 - 신규 별도 메서드
	// title / title.ngram / title.keyword 중 하나라도 매칭되면 히트된 문서이며,
	// _source.title 원문에 사용자가 입력한 키워드가 있으면 앱 레벨로 하이라이팅한다.
	// 기존 resultSearchParse() 및 관련 로직은 전혀 수정하지 않음.
	// ================================================================================
	
	@SuppressWarnings("unchecked")
	@Override
	public ApiResultVO resultMenuSearchParse(SearchResultVO searchResult, RequestSearchParamVO paramVO, String returnType, String traceId) {
		
		ApiResultVO resultVO = null;
		int idx = 0;
		int cnt = 0;
		int took = 0;
		int totalTook = 0;
		int totalCnt = 0;
		int sectionSize = 0;
		int status = 0;
		
		List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
		Map<String, Object> sectionMap = null;
		List<Map<String, Object>> resultResponse = null;
		Map<String, Object> responseMap = null;
		Map<String, Object> hitsMap = null;
		Map<String, Object> sourceMap = null;
		List<Map<String, Object>> hitsList = null;
		List<Map<String, Object>> docList = null;
		Map<String, Object> totalMap = null;
		
		List<Integer> statusList = new ArrayList<Integer>();
		List<String> errorMessageList = new ArrayList<String>();
		Map<String, Object> errorMap = null;
		String errorReason = null;
		String errorMessage = null;
		
		StringBuffer sb = new StringBuffer();
		
		// title 앱레벨 하이라이팅에 사용할 키워드 목록 (검색어 + 재검색 시 이전 검색어)
		// - StandardSearchServiceImpl.getKeywordList()와 동일한 규칙을 이 메서드 안에서만 별도로 구성
		//   (기존 클래스의 private 메서드를 건드리거나 접근범위를 바꾸지 않기 위해 별도 구현)
		List<String> rawKwdList = new ArrayList<>();
		if ("Y".equals(paramVO.getReSrchYn()) && paramVO.getPrevKwd() != null && !paramVO.getPrevKwd().isEmpty()) {
			rawKwdList.add(paramVO.getKwd());
			rawKwdList.addAll(paramVO.getPrevKwd());
		} else {
			rawKwdList.add(paramVO.getKwd());
		}
		
		// 동의어 확장 - 쿼리(createMenuQuery)에서 사용한 것과 동일한 확장 목록을 하이라이팅에도 적용
		// (동의어로 매칭된 문서는 원본 키워드가 title에 없을 수 있으므로, 실제 매칭된 동의어 term이 하이라이팅되어야 함)
		List<String> kwdList = getSynonymKeywordList(rawKwdList, traceId);
		
		String preTag = (String) searchConfig.getHighlightOption().get("pre_tags");
		String postTag = (String) searchConfig.getHighlightOption().get("post_tags");
		
		if (searchResult != null && searchResult.getResult() != null) {
			
			// [우장각 커스텀] 위 resultSearchParse() 와 동일한 이유로 "99999" 하드코딩 대신
			// indices 배열 길이(콤마 포함 여부)로 멀티/단일 응답 파싱을 일반화.
			String[] indices = searchConfig.getIndexName().get(paramVO.getCollapseCd()).split(SEARCH_SEPARATOR01);
			boolean isMultiIndex = indices.length > 1;

			if (isMultiIndex) {
				resultResponse = (List<Map<String, Object>>) searchResult.getResult().get("responses");
			} else {
				resultResponse = new ArrayList<Map<String, Object>>();
				resultResponse.add(searchResult.getResult());
			}
			
			for (String index : indices) {
				
				responseMap = resultResponse.get(idx++);
				took = 0;
				
				if (responseMap != null && responseMap.get("hits") != null) {
					
					try {
						
						hitsMap = (Map<String, Object>) responseMap.get("hits");
						hitsList = (List<Map<String, Object>>) hitsMap.get("hits");
						totalMap = (Map<String, Object>) hitsMap.get("total");
						status = 200;
						cnt = (int) totalMap.get("value");
						totalCnt += cnt;
						sectionSize++;
						took = (int) responseMap.get("took");
						totalTook += took;
						docList = new ArrayList<Map<String, Object>>();
						
						if (hitsList != null) {
							
							for (Map<String, Object> hits : hitsList) {
								
								sourceMap = (Map<String, Object>) hits.get("_source");
								
								// title 필드만 앱 레벨 하이라이팅 처리 (ES highlight 응답 자체를 사용하지 않음)
								if (sourceMap != null && sourceMap.get("title") != null) {
									String titleValue = String.valueOf(sourceMap.get("title"));
									String highlightedTitle = SearchUtil.getHighlightingStringMulti(titleValue, kwdList, preTag, postTag);
									sourceMap.put("title", highlightedTitle);
								}
								
								docList.add(sourceMap);
							}
						}
						
						if (sb.length() > 0) {
							sb.append(",");
						}
						sb.append("[index:").append(index).append(", sectionCnt:").append(cnt).append(", status:").append(status).append("]");
						
					} catch (Exception e) {
						handleSearchException(e);
					}
				} else {
					// 에러 발생 시
					if (responseMap.get("error") != null) {
						
						errorMap = (Map<String, Object>) responseMap.get("error");
						status = (int) responseMap.get("status");
						
						List<Map<String, Object>> tempList = (List<Map<String, Object>>) errorMap.get("root_cause");
						
						if (tempList != null && tempList.size() > 0) {
							Map<String, Object> tempMap = tempList.get(0);
							
							errorReason = (String) tempMap.get("reason");
							errorMessage = "[" + status + "] " + errorReason;
							
							if (errorMessageList.indexOf(errorMessage) < 0) {
								errorMessageList.add(errorMessage);
								log.error("[{}] Menu search error status : {}", traceId, status);
								log.error("[{}] Menu search error message : {}", traceId, errorMessage);
							}
							
							if (sb.length() > 0) {
								sb.append(",");
							}
						}
						
						sb.append("[index:").append(index).append(", sectionCnt:").append(cnt).append(", status:").append(status).append("]");
					}
					
					docList = null;
					cnt = 0;
				}
				
				sectionMap = new HashMap<String, Object>();
				sectionMap.put("indexName", index);
				sectionMap.put("sectionName", searchConfig.getSectionName().get(index));
				sectionMap.put("sectionCode", searchConfig.getSectionCode().get(index));
				sectionMap.put("sectionCnt", cnt);
				sectionMap.put("docList", docList);
				sectionMap.put("aggregations", null); // 메뉴 검색은 집계를 사용하지 않음
				
				resultList.add(sectionMap);
				statusList.add(status);
			}
		}
		
		// 모든 샤드 에러 일 경우
		if (statusList.size() > 0 && statusList.indexOf(200) < 0) {
			throw new SearchException(
					status
					, SearchErrorCode.SEARCH_EXCPETION.getErrorCode()
					, SearchErrorCode.SEARCH_EXCPETION.getErrorCodeSub()
					, SearchErrorCode.SEARCH_EXCPETION.getErrorMessage()
					, errorMessageList
			);
		}
		
		resultVO = ApiResultVO.builder()
				.searchSectionList(resultList)
				.kwd(paramVO.getKwd())
				.prevKwd(paramVO.getPrevKwd())
				.totalCnt(totalCnt)
				.sectionSize(sectionSize)
				.returnType(returnType)
				.reqTime(SearchUtil.getDate("yyyy/MM/dd HH:mm:ss"))
				.build();
		
		searchResult.setTook(totalTook);
		searchResult.setTotalCnt(totalCnt);
		
		log.debug("[{}] Menu Result : {}", traceId, resultVO.toString());
		log.info("[{}] Menu Result : {}", traceId, sb.toString());
		
		return resultVO;
	}
	
	// ================================================================================
	// 메뉴 검색 동의어 확장 - 신규 별도 메서드
	// 별도의 동의어 인덱스(idx_09_menu_syno)에서 kwdList와 일치하는 keyword 문서를 조회해
	// synonyms 필드를 확장 키워드로 추가한다. resultMenuSearchParse()와
	// StandardSearchServiceImpl.createMenuQuery() 양쪽에서 공통으로 재사용한다.
	// ================================================================================
	
	private static final String MENU_SYNONYM_KEYWORD_FIELD = "keyword";
	private static final String MENU_SYNONYM_LIST_FIELD = "synonyms";
	
	@SuppressWarnings("unchecked")
	@Override
	public List<String> getSynonymKeywordList(List<String> kwdList, String traceId) {
		
		List<String> expandedList = new ArrayList<>();
		
		if (kwdList == null || kwdList.isEmpty()) {
			return expandedList;
		}
		
		// 1) 원본 키워드는 동의어 조회 결과와 무관하게 항상 포함
		List<String> normalizedKwdList = new ArrayList<>();
		for (String kwd : kwdList) {
			if (kwd != null && !kwd.trim().isEmpty()) {
				String trimmed = kwd.trim();
				normalizedKwdList.add(trimmed);
				if (!expandedList.contains(trimmed)) {
					expandedList.add(trimmed);
				}
			}
		}
		
		if (normalizedKwdList.isEmpty()) {
			return expandedList;
		}
		
		// 2) 동의어 인덱스(idx_09_menu_syno)에서 원본 키워드와 정확히 일치하는 문서 조회
		try {
			TermQueryBuilder synonymTermsQuery = new TermQueryBuilder()
					.terms(MENU_SYNONYM_KEYWORD_FIELD, normalizedKwdList);
			
			DSLQueryBuilder synonymDslQueryBuilder = new DSLQueryBuilder()
					.builder()
					.size(normalizedKwdList.size() * 10) // 키워드당 여러 동의어 문서가 있을 수 있으므로 여유있게
					.query(synonymTermsQuery)
					.source(new String[] { MENU_SYNONYM_KEYWORD_FIELD, MENU_SYNONYM_LIST_FIELD })
					.build();
			
			SearchResultVO synonymResult = this.search(searchConfig.getMenuSynonymIndex() + "/_search", synonymDslQueryBuilder.buildJson());
			
			if (synonymResult != null && synonymResult.getResult() != null) {
				
				Map<String, Object> hitsMap = (Map<String, Object>) synonymResult.getResult().get("hits");
				
				if (hitsMap != null && hitsMap.get("hits") != null) {
					List<Map<String, Object>> hitsList = (List<Map<String, Object>>) hitsMap.get("hits");
					
					for (Map<String, Object> hit : hitsList) {
						Map<String, Object> source = (Map<String, Object>) hit.get("_source");
						
						if (source != null && source.get(MENU_SYNONYM_LIST_FIELD) != null) {
							List<String> synonyms = (List<String>) source.get(MENU_SYNONYM_LIST_FIELD);
							
							for (String synonym : synonyms) {
								if (synonym != null && !synonym.trim().isEmpty() && !expandedList.contains(synonym.trim())) {
									expandedList.add(synonym.trim());
								}
							}
						}
					}
				}
			}
			
			log.debug("[{}] Synonym expanded keywords : {} -> {}", traceId, normalizedKwdList, expandedList);
			
		} catch (QueryBuilderException qe) {
			log.error("[{}] Synonym query build error : {}", traceId, qe.getMessage());
			// 동의어 조회 자체가 실패해도 원본 키워드로는 검색이 계속되어야 하므로 예외를 전파하지 않음
		} catch (Exception e) {
			log.error("[{}] Synonym lookup failed : {}", traceId, e.getMessage());
			// idx_88_menu_syno 인덱스가 없거나 ES 호출이 실패해도 메뉴검색 자체는 원본 키워드로 계속 동작해야 함
		}
		
		return expandedList;
	}
	
	// ================================================================================
	// 최근 검색어(ES 공유저장) - 신규 별도 메서드
	// 운영서버 2대(로드밸런싱, sticky session 없음, Redis 세션클러스터링 없음) 구조에서
	// 서버가 어디로 라우팅되든 동일한 결과를 보장하기 위해, 두 서버가 공통으로 바라보는
	// ES를 최근검색어 저장소로 사용한다. 문서 ID는 클라이언트 쿠키(RSID)로 식별한다.
	// ================================================================================
	
	private static final String RECENT_SEARCH_INDEX = "idx_recent_search";
	private static final int RECENT_SEARCH_MAX_COUNT = 10;
	
	@SuppressWarnings("unchecked")
	@Override
	public List<String> getRecentKeywordList(String recentSearchId, String traceId) {
		
		List<String> recentList = new ArrayList<>();
		
		if (recentSearchId == null || recentSearchId.trim().isEmpty()) {
			return recentList;
		}
		
		try {
			Map<String, Object> termMap = new HashMap<>();
			termMap.put("_id", recentSearchId);
			
			Map<String, Object> queryMap = new HashMap<>();
			queryMap.put("term", termMap);
			
			Map<String, Object> bodyMap = new HashMap<>();
			bodyMap.put("query", queryMap);
			bodyMap.put("size", 1);
			bodyMap.put("_source", new String[] { "keywords" });
			
			SearchResultVO result = this.search(RECENT_SEARCH_INDEX + "/_search", GsonUtils.toJson(bodyMap));
			
			if (result != null && result.getResult() != null) {
				Map<String, Object> hitsMap = (Map<String, Object>) result.getResult().get("hits");
				
				if (hitsMap != null && hitsMap.get("hits") != null) {
					List<Map<String, Object>> hitsList = (List<Map<String, Object>>) hitsMap.get("hits");
					
					if (!hitsList.isEmpty()) {
						Map<String, Object> source = (Map<String, Object>) hitsList.get(0).get("_source");
						
						if (source != null && source.get("keywords") != null) {
							List<String> keywords = (List<String>) source.get("keywords");
							recentList.addAll(keywords);
						}
					}
				}
			}
			
		} catch (Exception e) {
			log.error("[{}] Recent keyword list lookup failed : {}", traceId, e.getMessage());
			// idx_recent_search 조회가 실패해도 검색 자체는 계속 동작해야 하므로 예외를 전파하지 않음
		}
		
		return recentList;
	}
	
	@Override
	public List<String> saveRecentKeyword(String recentSearchId, String keyword, String traceId) {
		
		if (recentSearchId == null || recentSearchId.trim().isEmpty() || keyword == null || keyword.trim().isEmpty()) {
			return getRecentKeywordList(recentSearchId, traceId);
		}
		
		String trimmedKeyword = keyword.trim();
		
		// 현재 목록 조회 후, 중복 제거 + 맨 앞으로 이동 + 최대개수 유지
		List<String> recentList = getRecentKeywordList(recentSearchId, traceId);
		recentList.remove(trimmedKeyword);
		recentList.add(0, trimmedKeyword);
		
		if (recentList.size() > RECENT_SEARCH_MAX_COUNT) {
			recentList = new ArrayList<>(recentList.subList(0, RECENT_SEARCH_MAX_COUNT));
		}
		
		return persistRecentKeywordList(recentSearchId, recentList, traceId);
	}
	
	@Override
	public List<String> deleteRecentKeyword(String recentSearchId, String keyword, String traceId) {
		
		List<String> recentList = getRecentKeywordList(recentSearchId, traceId);
		
		if (recentSearchId == null || recentSearchId.trim().isEmpty() || keyword == null) {
			return recentList;
		}
		
		recentList.remove(keyword.trim());
		
		return persistRecentKeywordList(recentSearchId, recentList, traceId);
	}
	
	@Override
	public void clearRecentKeywords(String recentSearchId, String traceId) {
		if (recentSearchId == null || recentSearchId.trim().isEmpty()) {
			return;
		}
		persistRecentKeywordList(recentSearchId, new ArrayList<>(), traceId);
	}
	
	/**
	 * 최근 검색어 목록을 ES(idx_recent_search)에 upsert 저장
	 * - POST {index}/_update/{id} (doc_as_upsert=true) 사용
	 *   → 이 프로젝트의 공통 search() 메서드가 항상 POST로만 ES를 호출하도록 되어 있어(WebClientHelper),
	 *     별도의 PUT 클라이언트를 추가하지 않고 기존 인프라를 그대로 재사용하기 위함
	 */
	private List<String> persistRecentKeywordList(String recentSearchId, List<String> recentList, String traceId) {
		try {
			Map<String, Object> docMap = new HashMap<>();
			docMap.put("keywords", recentList);
			docMap.put("updated_at", SearchUtil.getDate("yyyy-MM-dd'T'HH:mm:ss"));
			
			Map<String, Object> updateBodyMap = new HashMap<>();
			updateBodyMap.put("doc", docMap);
			updateBodyMap.put("doc_as_upsert", true);
			
			this.search(RECENT_SEARCH_INDEX + "/_update/" + recentSearchId, GsonUtils.toJson(updateBodyMap));
			
		} catch (Exception e) {
			log.error("[{}] Recent keyword persist failed : {}", traceId, e.getMessage());
			// 저장이 실패해도 이번 요청의 응답에는 갱신된 목록을 그대로 보여준다 (다음 요청에서 다시 저장 시도됨)
		}
		
		return recentList;
	}
}
