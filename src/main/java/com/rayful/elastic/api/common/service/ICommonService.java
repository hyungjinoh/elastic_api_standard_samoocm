package com.rayful.elastic.api.common.service;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.rayful.elastic.api.common.exception.SearchException;
import com.rayful.elastic.api.common.vo.ApiAutoResultVO;
import com.rayful.elastic.api.common.vo.ApiPopResultVO;
import com.rayful.elastic.api.common.vo.ApiResultExceptVO;
import com.rayful.elastic.api.common.vo.ApiResultVO;
import com.rayful.elastic.api.common.vo.SearchResultVO;
import com.rayful.elastic.api.standard.vo.RequestAutoParamVO;
import com.rayful.elastic.api.standard.vo.RequestPopParamVO;
import com.rayful.elastic.api.standard.vo.RequestSearchParamVO;

public interface ICommonService {
	
	/**
	 * 검색
	 * @param paramVO
	 * @param searchUri 
	 * @param query
	 * @return
	 */
	SearchResultVO search(String searchUri, String query);

	/**
	 * 로그 등록
	 * @param w
	 * @param searchResult
	 * @return
	 */
	CompletableFuture<Void> insertLog(String appId, RequestSearchParamVO paramVO, SearchResultVO searchResult, String traceId);
	
	/**
	 * 사용자 exception 시 데이터 처리
	 * @param se
	 * @param request
	 * @param returnType
	 * @param serviceType
	 * @return
	 */
	ApiResultExceptVO searchExceptionResponse(SearchException se, HttpServletRequest request, String returnType, String serviceType);
	
	/**
	 * exception 시 데이터 처리
	 * @param e
	 * @param request
	 * @param response
	 * @param returnType
	 * @param serviceType
	 * @return
	 */
	ApiResultExceptVO exceptionResponse(Exception e, HttpServletRequest request, HttpServletResponse response, String returnType, String serviceType);

	/**
	 * 
	 * @param e
	 */
	void handleSearchException(Exception e);
	 
	/**
	 * 검색결과 파싱
	 * @param searchResult
	 * @param paramVO
	 * @param returnType
	 * @param traceId
	 * @return
	 */
	ApiResultVO resultSearchParse(SearchResultVO searchResult, RequestSearchParamVO paramVO, String returnType, String traceId);
	
	/**
	 * 자동완성결과 파싱
	 * @param searchResult
	 * @param paramVO
	 * @param returnType
	 * @param traceId
	 * @return
	 */
	ApiAutoResultVO resultAutoParse(SearchResultVO searchResult, RequestAutoParamVO paramVO, String returnType, String traceId);
	
	/**
	 * 인기 검색어 파싱
	 * @param popularKeywordFile
	 * @param paramVO
	 * @param returnType
	 * @param traceId
	 * @return
	 */
	ApiPopResultVO resultPopParse(File popularKeywordFile, RequestPopParamVO paramVO, String returnType, String traceId);
	
	/**
	 * 메뉴(title 전용) 검색결과 파싱
	 * - ES highlight 없이 _source.title 값에 앱 레벨(SearchUtil.getHighlightingStringMulti)로 하이라이팅 적용
	 * - 기존 resultSearchParse()와는 완전히 분리된 별도 메서드
	 * @param searchResult
	 * @param paramVO
	 * @param returnType
	 * @param traceId
	 * @return
	 */
	ApiResultVO resultMenuSearchParse(SearchResultVO searchResult, RequestSearchParamVO paramVO, String returnType, String traceId);
	
	/**
	 * 메뉴 검색 키워드에 대한 동의어 확장 목록 조회 - 신규 추가
	 * - 별도의 동의어 인덱스(idx_09_menu_syno)에서 kwdList와 정확히 일치하는 keyword 문서를 찾아
	 *   해당 문서의 synonyms 필드를 확장 키워드로 추가한다.
	 * - 원본 키워드는 조회 결과와 무관하게 항상 포함된다(원본에 매칭되는 동의어가 없어도 원본 자체로는 검색되어야 하므로).
	 * @param kwdList 원본 키워드 목록 (검색어 + 재검색 시 이전 검색어)
	 * @param traceId
	 * @return 원본 + 동의어가 합쳐진 키워드 목록 (중복 제거)
	 */
	List<String> getSynonymKeywordList(List<String> kwdList, String traceId);
	
	/**
	 * 최근 검색어 목록 조회 - 신규 추가
	 * - HttpSession 대신 별도의 ES 인덱스(idx_recent_search)에 저장한다.
	 *   운영서버가 로드밸런싱 2대(sticky session 없음, Redis 세션클러스터링 없음) 구조이므로,
	 *   두 서버가 공통으로 접근하는 ES를 공유 저장소로 사용해 서버가 바뀌어도 동일하게 동작하도록 한다.
	 * @param recentSearchId 클라이언트 식별용 쿠키 값 (RecentSearchUtil.resolveRecentSearchId()로 발급/조회)
	 * @param traceId
	 * @return
	 */
	List<String> getRecentKeywordList(String recentSearchId, String traceId);
	
	/**
	 * 검색어를 최근 검색어 목록에 반영(중복제거+맨앞이동+최대개수 유지)하고 ES에 저장한다.
	 * @param recentSearchId
	 * @param keyword
	 * @param traceId
	 * @return 갱신된 최근 검색어 목록
	 */
	List<String> saveRecentKeyword(String recentSearchId, String keyword, String traceId);
	
	/**
	 * 최근 검색어 목록에서 특정 키워드만 삭제
	 * @param recentSearchId
	 * @param keyword
	 * @param traceId
	 * @return 갱신된 최근 검색어 목록
	 */
	List<String> deleteRecentKeyword(String recentSearchId, String keyword, String traceId);
	
	/**
	 * 최근 검색어 목록 전체 삭제
	 * @param recentSearchId
	 * @param traceId
	 */
	void clearRecentKeywords(String recentSearchId, String traceId);
	
}
