package com.rayful.elastic.api.standard.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.rayful.elastic.api.common.builder.AggsQueryBuilder;
import com.rayful.elastic.api.common.builder.BoolQueryBuilder;
import com.rayful.elastic.api.common.builder.DSLQueryBuilder;
import com.rayful.elastic.api.common.builder.ExistsQueryBuilder;
import com.rayful.elastic.api.common.builder.FullTextQueryBuilder;
import com.rayful.elastic.api.common.builder.RangeQueryBuilder;
import com.rayful.elastic.api.common.builder.TermQueryBuilder;
import com.rayful.elastic.api.common.builder.WildcardQueryBuilder;
import com.rayful.elastic.api.common.config.SearchConfig;
import com.rayful.elastic.api.common.exception.QueryBuilderException;
import com.rayful.elastic.api.common.service.ICommonService;

import com.rayful.elastic.api.common.vo.SearchResultVO;
import com.rayful.elastic.api.standard.vo.RequestSearchParamVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Primary
public class StandardSearchServiceImpl implements IStandardSearchService {
	
	private static final String SEARCH_TYPE01 = "/_search";
	private static final String SEARCH_TYPE02 = "/_msearch";
	private static final String SEARCH_SEPARATOR01 = ","; 			// 설정파일 구분자(1차구분자)
	private static final String SEARCH_SEPARATOR02 = ":"; 			// 설정파일 구분자(2치구분자)
	private static final String ACL_NULL = "!@#NONE!@#"; 			// 권한체크하는 대상에 권한정보가 없을경우 입력할 문자열
	private static final String UPD_DTM_FIELD_DEFAULT = "upd_dtm"; 			// period 필터 기본 대상 필드(yml condition.<index>.upddtm 로 인덱스별 재정의 가능)
	private static final DateTimeFormatter PERIOD_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // period=1,2 자동계산 시 사용할 날짜 포맷
	
	@Autowired
	private SearchConfig searchConfig;
	
	@Autowired
	private ICommonService commonService;
	
	/**
	 * 검색목록 요청
	 * @param paramVO 
	 * @param traceId 
	 */
	@Override
	public SearchResultVO getSearchList(RequestSearchParamVO paramVO, String traceId) {
		SearchResultVO searchResult = new SearchResultVO();
		String query = null;
		String searchUri = null;
		
		try {
			// [우장각 커스텀] "collapseCd==99999" 대신 "이 collapseCd 의 index-name 설정값이 콤마를
			// 포함하는지"로 멀티/단일 검색 분기 조건을 일반화 — ConfigValidator 와 동일한 기준을 쓴다.
			String configuredIndexNames = searchConfig.getIndexName().get(paramVO.getCollapseCd());
			boolean isMultiIndex = configuredIndexNames != null && configuredIndexNames.contains(SEARCH_SEPARATOR01);

			if (isMultiIndex) {
				searchUri = SEARCH_TYPE02;
				query = buildMultiSearchQuery(paramVO, configuredIndexNames.split(SEARCH_SEPARATOR01));
			} else {
				searchUri = configuredIndexNames + SEARCH_TYPE01;
				query = buildSingleSearchQuery(paramVO, configuredIndexNames);
			}
			log.debug("[{}] SearchUri : {}", traceId, searchUri);
			log.debug("[{}] Query : {}", traceId, query);
			
			searchResult = this.commonService.search(searchUri, query);
			searchResult.setCollapseCd(paramVO.getCollapseCd());
			searchResult.setKeyword(paramVO.getKwd());
			log.debug("[{}] Search : {}", traceId, searchResult);
			
		} catch (Exception e) {
			this.commonService.handleSearchException(e);
		}
		
		return searchResult;
	}
	
	/**
	 * 통합검색 화면의 개별 섹션(인덱스) 정렬방식 변경용
	 * - collapseCd는 반드시 개별 인덱스 코드여야 함(99999 금지 - 통합검색은 getSearchList() 사용)
	 * - 기존 단일 인덱스 검색 경로를 그대로 재사용 (쿼리 생성 로직 중복 없음)
	 */
	@Override
	public SearchResultVO getSectionSearchList(RequestSearchParamVO paramVO, String traceId) {
		
		if ("99999".equals(paramVO.getCollapseCd())) {
			// 통합검색 코드가 잘못 들어온 경우 - 이 메서드는 개별 섹션 전용이므로 방어적으로 차단
			SearchResultVO errorResult = new SearchResultVO();
			this.commonService.handleSearchException(
					new IllegalArgumentException("getSectionSearchList()는 개별 인덱스(collapseCd)에만 사용할 수 있습니다. 통합검색은 getSearchList()를 사용하세요."));
			return errorResult;
		}
		
		// 개별 인덱스 검색은 기존 로직과 완전히 동일하므로 그대로 위임
		return getSearchList(paramVO, traceId);
	}
	
	/**
	 * _msearch 검색쿼리 생성 시 createQuery 함수 반복 수행하면서 쿼리문 append
	 *
	 * [UI 요구사항 반영]
	 * 통합검색(collapseCd=99999) 결과화면은 인덱스(섹션)마다 "위에서 아래로 3개씩" 미리보기만
	 * 보여주는 구조이므로, 클라이언트가 어떤 size를 보내든 여기서는 무시하고
	 * search.integrated-preview-size(기본 3)로 고정한다.
	 * (개별 인덱스를 더 보거나 정렬을 바꾸는 동작은 getSectionSearchList()가 별도로 처리하며,
	 *  그때는 클라이언트가 보낸 size를 그대로 사용한다 - 여기서 강제하는 것과 무관함)
	 */
	private String buildMultiSearchQuery(RequestSearchParamVO paramVO, String[] indices) throws QueryBuilderException {
        StringBuilder queries = new StringBuilder();

        RequestSearchParamVO previewParamVO = buildPreviewParamVO(paramVO);

        for (String index : indices) {
            DSLQueryBuilder qb = createQuery(previewParamVO, index);
            queries.append(String.format("{\"index\":\"%s\"}\n%s\n", index, qb.buildJson()));
        }

        return queries.toString();
    }
	
	/**
	 * 통합검색용 미리보기 paramVO 복제
	 * - size/page만 미리보기 규격(3개, 1페이지)으로 강제 교체하고, 나머지(검색어/필터/정렬 등)는 그대로 유지
	 * - RequestSearchParamVO 자체는 수정하지 않고 필요한 필드만 옮겨 담는다 (StandardSearchController.buildMenuParamVO()와 동일한 패턴)
	 */
	private RequestSearchParamVO buildPreviewParamVO(RequestSearchParamVO paramVO) {
		RequestSearchParamVO previewParamVO = new RequestSearchParamVO();
		previewParamVO.setKwd(paramVO.getKwd());
		previewParamVO.setKwdOption(paramVO.getKwdOption());
		previewParamVO.setSrchArea(paramVO.getSrchArea());
		previewParamVO.setReSrchYn(paramVO.getReSrchYn());
		previewParamVO.setPrevKwd(paramVO.getPrevKwd());
		previewParamVO.setCollapseCd(paramVO.getCollapseCd());
		previewParamVO.setAcl01(paramVO.getAcl01());
		previewParamVO.setAcl02(paramVO.getAcl02());
		previewParamVO.setSort(paramVO.getSort());
		previewParamVO.setRegDtmYn(paramVO.getRegDtmYn());
		previewParamVO.setRegStartDtm(paramVO.getRegStartDtm());
		previewParamVO.setRegEndDtm(paramVO.getRegEndDtm());
		previewParamVO.setAttachedType(paramVO.getAttachedType());
		previewParamVO.setFilterOption(paramVO.getFilterOption());
		
		// 여기만 강제 교체
		previewParamVO.setSize(searchConfig.getIntegratedPreviewSize());
		previewParamVO.setPage(1);
		
		return previewParamVO;
	}
	
	/**
	 * _search 검색쿼리 생성 시 createQuery 함수 호출
	 */
	private String buildSingleSearchQuery(RequestSearchParamVO paramVO, String index) throws QueryBuilderException {
		DSLQueryBuilder qb = createQuery(paramVO, index);

        return qb.buildJson();
    }

	/**
	 * 검색쿼리 생성
	 */
	private DSLQueryBuilder createQuery(RequestSearchParamVO params, String index) throws QueryBuilderException {
		
		// 검색어 목록 조회
		List<String> kwdList = getKeywordList(params); 
		
		// bool 쿼리 생성
	    BoolQueryBuilder mainBoolQuery = new BoolQueryBuilder().builder();
	    
	    // 메인 bool 쿼리 생성 - 검색어, 상세검색, 권한 조건등 추가
	    buildMainBoolQuery(params, index, kwdList, mainBoolQuery);
	    
        // 화면 내 어그리게이션 선택 시 조건 추가 
        addAggsFilters(params, mainBoolQuery, index);
        
        // 전체 쿼리 구성
        DSLQueryBuilder dslQueryBuilder = new DSLQueryBuilder()
                .builder()
                .trackTotalHits(true)
                .from((params.getPage() - 1) * params.getSize())
                .size(params.getSize())
                .sort(searchConfig.getSort().get(index).get(params.getSort()), SEARCH_SEPARATOR01, SEARCH_SEPARATOR02)
                .query(mainBoolQuery)
                .highlight(searchConfig.getHighlight().get(index).split(SEARCH_SEPARATOR01), searchConfig.getHighlightOption(), kwdList, params.getKwdOption())
                .aggregation(getAggsOptionMap(params, index))
                .source(searchConfig.getIncludes().get(index).split(SEARCH_SEPARATOR01))
                .build();
        
        return dslQueryBuilder;
	}
	
	/**
	 * 검색어 목록 조회
	 * @param params
	 * @return
	 */
	private List<String> getKeywordList(RequestSearchParamVO params) {
        List<String> kwdList = new ArrayList<>();

        if ("Y".equals(params.getReSrchYn()) && params.getPrevKwd() != null && !params.getPrevKwd().isEmpty()) {
            kwdList.add(params.getKwd());
            kwdList.addAll(params.getPrevKwd());
        } else {
            kwdList.add(params.getKwd());
        }

        return kwdList;
    }
	
	/**
	 * 메인 bool 검색어 쿼리 생성
	 * @param params
	 * @param index
	 * @param kwdList
	 * @param mainBoolQuery
	 * @throws QueryBuilderException
	 */
	private void buildMainBoolQuery(RequestSearchParamVO params, String index, List<String> kwdList, BoolQueryBuilder mainBoolQuery) throws QueryBuilderException {
        
		List<String> searchAreaList = params.getSrchArea();
    	StringBuilder sb = new StringBuilder();
    	
    	for (String searchArea : searchAreaList) {
    		
    		String searchAreaField = searchConfig.getFields().get(index).get(searchArea);
    		if (sb.length() > 0) {
                sb.append(SEARCH_SEPARATOR01).append(searchAreaField);
            } else {
                sb.append(searchAreaField);
            }
        }
    	
    	String kwdOption = null;
    	
    	if (params.getKwdOption() == null || params.getKwdOption().isEmpty()) {
    		kwdOption = searchConfig.getQueryOption().get("operator"); // 기본값 설정
        } else {
        	kwdOption = params.getKwdOption().toLowerCase();
        }
    	
    	for (String keyword : kwdList) {

    		// [버그 수정] keyword 가 비어있으면(검색어 없이 상세검색 필터만으로 조회하는 경우) 이 블록을
    		// 통째로 건너뛴다. 예전에는 빈 문자열로도 multi_match 를 만들어 mainBoolQuery.must(...) 로
    		// 추가했는데, ES 에서 빈 문자열 multi_match 는 분석 후 토큰이 하나도 없어 사실상 match_none
    		// 으로 동작한다 — 이게 must 절에 들어가면 다른 필터(예: ③ 폴더경로)가 다 맞아도 전체 쿼리가
    		// 무조건 0건이 됐다("프로젝트 탭에서 키워드 없이 폴더만 선택해 적용"이 동작 안 하던 원인).
    		// keyword 를 아예 안 넣으면 bool 쿼리에 필터 조건만 남아 그걸 만족하는 문서가 전부(기본 점수로)
    		// 매칭된다 — "검색어 없이 필터만으로 둘러보기"의 정상적인 ES 동작 방식.
    		if (keyword == null || keyword.trim().isEmpty()) {
    			continue;
    		}

    		FullTextQueryBuilder multiMatchQuery = null; 
    		
    		if (!"phrase".equalsIgnoreCase(kwdOption)) {
    			multiMatchQuery = new FullTextQueryBuilder()
                        .multiMatch(
                        		sb.toString().split(SEARCH_SEPARATOR01),
                                keyword,
                                kwdOption,
                                searchConfig.getQueryOption().get("type")
                        );
    	    } else {
    	    	multiMatchQuery = new FullTextQueryBuilder()
                        .multiMatch(
                        		sb.toString().split(SEARCH_SEPARATOR01),
                                keyword,
                                null,
                                searchConfig.getQueryPhraseOption().get("type")
                        );
    	    }
            
            
            FullTextQueryBuilder multiMatchPhraseQuery = new FullTextQueryBuilder()
                    .multiMatchPhrase(
                    		sb.toString().split(SEARCH_SEPARATOR01),
                            keyword,
                            Integer.parseInt(searchConfig.getQueryPhraseOption().get("boost")),
                            Integer.parseInt(searchConfig.getQueryPhraseOption().get("slop")),
                            searchConfig.getQueryPhraseOption().get("type")
                    );
            //BoolQueryBuilder innerBoolQuery = new BoolQueryBuilder().builder().should(multiMatchQuery);
            mainBoolQuery.must(multiMatchQuery);
            mainBoolQuery.should(multiMatchPhraseQuery);
        }
        
		String dateYn = searchConfig.getCondition().get(index).get("dateyn");
		String extYn = searchConfig.getCondition().get(index).get("extyn");
		String acl01Yn = searchConfig.getCondition().get(index).get("acl01yn");
		String acl02Yn = searchConfig.getCondition().get(index).get("acl02yn");
        
		// 첨부파일 조건 추가
		if(extYn != null && "Y".equalsIgnoreCase(extYn))
			addAttachmentFilters(params, mainBoolQuery, index);
        // 기간 조건 추가
		if(dateYn != null && "Y".equalsIgnoreCase(dateYn))
			addDateFilters(params, mainBoolQuery, index);

		// 기간 조건 추가(수정일 upd_dtm 기준, period 값에 따라 1주일/1개월/전체/직접입력)
		addPeriodFilters(params, mainBoolQuery, index);
        // 권한 조건 추가 - acl01
		if(acl01Yn != null && "Y".equalsIgnoreCase(acl01Yn))
			addAcl01Filters(params, mainBoolQuery, index);
        // 권한 조건 추가 - acl02
		if(acl02Yn != null && "Y".equalsIgnoreCase(acl02Yn))
			addAcl02Filters(params, mainBoolQuery, index);
    }
	
	/**
	 * 첨부파일 확장자 조건 생성
	 * @param params
	 * @param mainBoolQuery
	 * @throws QueryBuilderException
	 */
	private void addAttachmentFilters(RequestSearchParamVO params, BoolQueryBuilder mainBoolQuery, String index) throws QueryBuilderException {
        if (params.getAttachedType() != null && !params.getAttachedType().isEmpty()) {
        	
        	boolean chkEtc = false;
        	
            List<String> arrAttached = new ArrayList<>(params.getAttachedType());
            arrAttached.sort(Comparator.reverseOrder());

            StringBuilder sb = new StringBuilder();
            String etcStr = null;

            for (String attached : arrAttached) {
            	if ("ATTACHED99".equals(attached)) {
                	etcStr = searchConfig.getAttached().get(attached);
                	chkEtc = true;
                } else {
                    if (sb.length() > 0) {
                        sb.append(SEARCH_SEPARATOR01).append(searchConfig.getAttached().get(attached));
                    } else {
                        sb.append(searchConfig.getAttached().get(attached));
                    }
                }
            }
            
            // 기타 조건일 경우 - 첨부파일 확장자 필드가 존재하는 경우만 검색
            if(chkEtc) {
            	mainBoolQuery.must(
                	    new ExistsQueryBuilder()
                	        .field(searchConfig.getCondition().get(index).get("ext"))
            	);
            }
            
            // sb에 있는 확장자들을 etcStr에서 제거
            if (etcStr != null && etcStr.length() > 0) {
                Set<String> mustNotSet = new HashSet<>(Arrays.asList(etcStr.split(SEARCH_SEPARATOR01)));
                if (sb.length() > 0) {
                    String[] mustTypes = sb.toString().split(SEARCH_SEPARATOR01);
                    for (String type : mustTypes) {
                        mustNotSet.remove(type);
                    }
                }
                if (!mustNotSet.isEmpty()) {
                    TermQueryBuilder termQuery = new TermQueryBuilder()
                            .terms(searchConfig.getCondition().get(index).get("ext"), mustNotSet.toArray(new String[0]));
                    mainBoolQuery.mustNot(termQuery);
                }
            }

            if (sb.length() > 0) {
            	
            	if(!chkEtc) {
            		TermQueryBuilder termQuery = new TermQueryBuilder()
                            .terms(searchConfig.getCondition().get(index).get("ext"), sb.toString().split(SEARCH_SEPARATOR01));
                    mainBoolQuery.filter(termQuery);
            	}
            }
        }
    }
	
	/**
	 * 기간 조건 생성 
	 * @param params
	 * @param mainBoolQuery
	 * @throws QueryBuilderException
	 */
	private void addDateFilters(RequestSearchParamVO params, BoolQueryBuilder mainBoolQuery, String index) throws QueryBuilderException {
        if ("Y".equals(params.getRegDtmYn()) && (params.getRegStartDtm() != null || params.getRegEndDtm() != null)) {
        	RangeQueryBuilder rangeQuery = new RangeQueryBuilder()
                    .rangeStr(
                            searchConfig.getCondition().get(index).get("date"),
                            params.getRegStartDtm(),
                            params.getRegEndDtm(),
                            searchConfig.getRangeOption().get("dateformat")
                    );
            mainBoolQuery.filter(rangeQuery);
        }
    }

	/**
	 * 수정일(upd_dtm) 기준 기간 조건 생성
	 * period: 1=최근1주일, 2=최근1개월, 4=전체기간(필터없음, 기본값), 99=직접입력(periodStartDtm~periodEndDtm)
	 * 대상 필드는 yml의 condition.<index>.upddtm 으로 인덱스별 재정의 가능, 미지정 시 upd_dtm 기본 사용
	 * @param params
	 * @param mainBoolQuery
	 * @param index
	 * @throws QueryBuilderException
	 */
	private void addPeriodFilters(RequestSearchParamVO params, BoolQueryBuilder mainBoolQuery, String index) throws QueryBuilderException {
		String period = params.getPeriod();

		// 값이 없거나 "4"(전체기간)이면 필터를 적용하지 않는다.
		if (period == null || period.isEmpty() || "4".equals(period)) {
			return;
		}

		String startDtm;
		String endDtm;
		LocalDate today = LocalDate.now();

		switch (period) {
			case "1": // 최근 1주일
				startDtm = today.minusWeeks(1).format(PERIOD_DATE_FORMATTER);
				endDtm = today.format(PERIOD_DATE_FORMATTER);
				break;
			case "2": // 최근 1개월
				startDtm = today.minusMonths(1).format(PERIOD_DATE_FORMATTER);
				endDtm = today.format(PERIOD_DATE_FORMATTER);
				break;
			case "99": // 직접입력
				startDtm = params.getPeriodStartDtm();
				endDtm = params.getPeriodEndDtm();
				break;
			default:
				// validateAndSetDefaults()에서 이미 걸러지지만, 방어적으로 한 번 더 처리
				log.warn("Unsupported period value: {} - upd_dtm 기간 필터를 적용하지 않습니다.", period);
				return;
		}

		String updDtmField = searchConfig.getCondition().get(index).getOrDefault("upddtm", UPD_DTM_FIELD_DEFAULT);

		RangeQueryBuilder rangeQuery = new RangeQueryBuilder()
				.rangeStr(
						updDtmField,
						startDtm,
						endDtm,
						searchConfig.getRangeOption().get("dateformat")
				);
		mainBoolQuery.filter(rangeQuery);
	}
	
	/**
	 * 권한 조건 생성 1
	 * @param params
	 * @param index
	 * @param mainBoolQuery
	 * @throws QueryBuilderException
	 */
	private void addAcl01Filters(RequestSearchParamVO params, BoolQueryBuilder mainBoolQuery, String index) throws QueryBuilderException {
        String aclIndices = searchConfig.getAclIndices();
        String[] arrAclIndices = aclIndices.split(SEARCH_SEPARATOR01);

        for (String indices : arrAclIndices) {
            if (indices.equals(index)) {
            	
            	Map<String, List<String>> acl01Map = params.getAcl01();
            	if (acl01Map != null) {
            	    List<String> aclList = acl01Map.get(searchConfig.getSectionCode().get(index));

            	    if (aclList != null && !aclList.isEmpty()) {
            	        TermQueryBuilder termQuery = new TermQueryBuilder()
            	                .terms(searchConfig.getCondition().get(index).get("acl01"), aclList);
            	        mainBoolQuery.filter(termQuery);
            	    } else {
            	        TermQueryBuilder termQuery = new TermQueryBuilder()
            	                .term(searchConfig.getCondition().get(index).get("acl01"), ACL_NULL);
            	        mainBoolQuery.filter(termQuery);
            	    }
            	} else {
            	    // acl01 정보 자체가 없을 때도 기본 필터를 넣어줌
            	    TermQueryBuilder termQuery = new TermQueryBuilder()
            	            .term(searchConfig.getCondition().get(index).get("acl01"), ACL_NULL);
            	    mainBoolQuery.filter(termQuery);
            	}
            }
        }
    }
	
	/**
	 * 권한 조건 생성 2 -  현재 사용되지 않으며 기능적으로 acl  권한이 있을 경우, must_not  조건 수행
	 * @param params
	 * @param index
	 * @param mainBoolQuery
	 * @throws QueryBuilderException
	 */
	private void addAcl02Filters(RequestSearchParamVO params, BoolQueryBuilder mainBoolQuery, String index) throws QueryBuilderException {
        String aclIndices = searchConfig.getAclIndices();
        String[] arrAclIndices = aclIndices.split(SEARCH_SEPARATOR01);

        for (String indices : arrAclIndices) {
            if (indices.equals(index)) {
            	
            	Map<String, List<String>> acl02Map = params.getAcl02(); // null 체크 필요
                if (acl02Map != null) {
                    List<String> aclList = acl02Map.get(searchConfig.getSectionCode().get(index));

                    if (aclList != null && !aclList.isEmpty()) {
                        TermQueryBuilder termQuery = new TermQueryBuilder()
                                .terms(searchConfig.getCondition().get(index).get("acl02"), aclList);
                        mainBoolQuery.mustNot(termQuery);
                    } else {
                        TermQueryBuilder termQuery = new TermQueryBuilder()
                                .term(searchConfig.getCondition().get(index).get("acl02"), ACL_NULL);
                        mainBoolQuery.filter(termQuery);
                    }
                } else {
                    // acl02 정보 자체가 없을 때도 기본 필터 추가
                    TermQueryBuilder termQuery = new TermQueryBuilder()
                            .term(searchConfig.getCondition().get(index).get("acl02"), ACL_NULL);
                    mainBoolQuery.filter(termQuery);
                }
            }
        }
    }
	
	/**
	 * 어그리게이션 조건 추가
	 * @param params
	 * @param index
	 * @param mainBoolQuery
	 * @throws QueryBuilderException
	 */
//	private void addAggsFilters(RequestSearchParamVO params, BoolQueryBuilder mainBoolQuery) throws QueryBuilderException {
//		Map<String, List<String>> filterOption = params.getFilterOption();
//		
//		if (filterOption != null && filterOption.size() > 0) {
//			for (Map.Entry<String, List<String>> entry : filterOption.entrySet()) {
//	            String fieldName = entry.getKey();
//	            List<String> fieldValues = entry.getValue();
//
//	            if (fieldValues != null && !fieldValues.isEmpty()) {
//	            	
//	            	for (String value : fieldValues) {
//	            		TermQueryBuilder termQuery = new TermQueryBuilder()
//		    	                .term(fieldName, value);
//		            	
//		            	mainBoolQuery.filter(termQuery);
//	            	}
//	            }
//	        }
//		}
//    }

	/***
	 *  2026.10.23 커스텀 오퍼레이터 3종 추가함 :
	 *  	orFilter(values),다중선택 체크박스(지역, 업무구분 등)
	 *  	eqFilter(value), 단일선택 드롭다운(계약여부, 구조형식 등)
	 *  	containsFilter(value), 자유 텍스트 입력(참여자명, 자재검색 등)
	 *  	rangeFilter(gte, lte), 사업비 최소~최대
	 */
	@SuppressWarnings("unchecked")
	private void addAggsFilters(RequestSearchParamVO params, BoolQueryBuilder mainBoolQuery, String index) throws QueryBuilderException {
	    Map<String, Object> filterOption = params.getFilterOption();

	    if (filterOption == null || filterOption.isEmpty()) {
	        return;
	    }
	    
	    // 조건값 미리 가져오기 -  회사코드를 기본 필터조건을 사용할 경우 사용, application.yml 에 해당 필드가 없다면 무시될 대상
	    String compCdYn = searchConfig.getCondition().get(index).get("compcdyn"); 
	    String compCd   = searchConfig.getCondition().get(index).get("compcd"); 
	    
	    for (Map.Entry<String, Object> entry : filterOption.entrySet()) {
	        String fieldName = entry.getKey();
	        Object rawOption = entry.getValue();

	        if (rawOption == null) continue;
	        
	        // compCd 관련 필드 처리 여부 체크 - 회사코드를 기본 필터조건을 사용할 경우 사용, "반드시" 검색대상 인덱스에 compcdyn, compcd 에 값을 입력할것, 없다면 무시될 대상
	        if (compCd != null && fieldName.equalsIgnoreCase(compCd)) {
	            if ("N".equalsIgnoreCase(compCdYn)) {
	                // compCdYn "N" 이면 필터 패스
	                continue;
	            }
	        }
	    
	        // Case 1. 단순 배열 : 무조건 and 처리
	        if (rawOption instanceof List) {
	            List<String> values = (List<String>) rawOption;
	            if (values != null && !values.isEmpty()) {
	            	for (String value : values) {
	            		TermQueryBuilder termQuery = new TermQueryBuilder()
		    	                .term(fieldName, value);
		            	
		            	mainBoolQuery.filter(termQuery);
	                }
	            }
	        }
	        
	        // Case 2. operator + values(또는 value/gte/lte) 구조
	        else if (rawOption instanceof Map) {
	            Map<String, Object> optionMap = (Map<String, Object>) rawOption;
	            String operator = (String) optionMap.getOrDefault("operator", "and");

	            // ===== [커스텀 확장: 우장각 통합검색] =====
	            // 표준 filterOption은 term/terms(완전일치)만 지원하는데, 우장각 프로젝트 탭의
	            // "직접입력(텍스트)" 필드(내부/외부 참여자, 프로젝트자재검색, 프로젝트번호/명, 면적/규모 값)는
	            // 부분일치(contains)가, "사업비"는 숫자 범위(range)가 필요해서 operator 2종을 추가했다.
	            // 기존 and/or 분기와 완전히 분리된 별도 branch라 표준 동작에는 영향이 없다.
	            if ("contains".equalsIgnoreCase(operator)) {
	                // [버그 수정] 원래 FullTextQueryBuilder().match(fieldName, value, "and") 로 구현돼
	                // 있었는데, 이건 fieldName(예: path)이 형태소 분석(nori)되는 text 필드라서, "우장각"
	                // 처럼 사전에 없는 고유명사가 품사 분석 과정에서 통째로 걸러져 토큰이 0개가 되면
	                // match 쿼리가 아무것도 매칭 못 하는(match_none) 문제가 있었다 — "검색어 없이 ECM
	                // 폴더만 선택해 적용"이 항상 0건으로 나오던 원인. 분석을 안 타는 keyword 서브필드에
	                // 와일드카드(부분일치)로 바꿔서, 형태소 분석 결과와 무관하게 문자열 그대로 포함
	                // 여부만 확인하도록 했다. 이 fieldName 들은 전부 text + keyword 서브필드로 매핑돼
	                // 있다(path, internalParticipant, externalParticipant, materialKeywords, 면적/규모
	                // 필드 등 — index-documents.json 참고).
	                String value = (String) optionMap.get("value");
	                if (value != null && !value.isEmpty()) {
	                    WildcardQueryBuilder wildcardQuery = new WildcardQueryBuilder()
	                            .wildcardContains(fieldName + ".keyword", value);
	                    mainBoolQuery.filter(wildcardQuery);
	                }
	                continue;
	            }
	            if ("exclude".equalsIgnoreCase(operator)) {
	                // {"operator":"exclude","value":"초안"} → 제외(NOT). 지식정보/전사문서/부서문서 공통 상세검색의
	                // "결과에서 제외할 단어(NOT)" 용. kwd/kwdOption 표준 플로우엔 NOT 개념이 없어 filterOption 으로 확장.
	                String value = (String) optionMap.get("value");
	                if (value != null && !value.isEmpty()) {
	                    FullTextQueryBuilder matchQuery = new FullTextQueryBuilder()
	                            .match(fieldName, value, "and");
	                    mainBoolQuery.mustNot(matchQuery);
	                }
	                continue;
	            }
	            if ("range".equalsIgnoreCase(operator)) {
	                // {"operator":"range","gte":"1000000000","lte":"2000000000"} → 숫자/날짜 범위
	                // (RangeQueryBuilder.rangeStr은 문자열 그대로 ES range 쿼리에 싣는다 — 숫자 필드는 문자열 "1000000000"도
	                //  ES가 숫자로 캐스팅해 비교하므로 그대로 사용 가능)
	                String gte = optionMap.get("gte") != null ? String.valueOf(optionMap.get("gte")) : null;
	                String lte = optionMap.get("lte") != null ? String.valueOf(optionMap.get("lte")) : null;
	                if ((gte != null && !gte.isEmpty()) || (lte != null && !lte.isEmpty())) {
	                    RangeQueryBuilder rangeQuery = new RangeQueryBuilder()
	                            .rangeStr(fieldName, gte, lte, null);
	                    mainBoolQuery.filter(rangeQuery);
	                }
	                continue;
	            }
	            // ===== [커스텀 확장 끝] =====

	            List<String> values = (List<String>) optionMap.get("values");

	            if (values == null || values.isEmpty()) continue;

	            if ("or".equalsIgnoreCase(operator)) {
	                // operator = or : 하나라도 매칭되면 됨
	                TermQueryBuilder termQuery = new TermQueryBuilder()
	                		.terms(fieldName, values);
	                
	                mainBoolQuery.filter(termQuery);
	            } else {
	            	 // operator = and : 모든 값이 각각 매칭되어야 함(Case 1과 동일)
	                for (String value : values) {
	            		TermQueryBuilder termQuery = new TermQueryBuilder()
		    	                .term(fieldName, value);
		            	
		            	mainBoolQuery.filter(termQuery);
	                }
	            }
	        }
	        // 예외 케이스 (잘못된 타입)
	        else {
	            throw new QueryBuilderException("Unsupported filterOption format for field: " + fieldName);
	        }
	    }
	}
	
	/**
	 * 어그리게이션 옵션 조회
	 * @param params
	 * @param index
	 * @return
	 */
	private Map<String, Map<String, Object>> getAggsOptionMap(RequestSearchParamVO params, String index) {
	    Map<String, String> aggsConfig = searchConfig.getAggs().get(index);

	    Map<String, Map<String, Object>> arrMap = new HashMap<>();

	    if (aggsConfig != null && !aggsConfig.isEmpty()) {
	        String fieldStr = (String) aggsConfig.get("field");
	        String sortStr = (String) aggsConfig.get("sort");

	        if (fieldStr != null && !fieldStr.isEmpty()) {
	            String[] arrFields = fieldStr.split(SEARCH_SEPARATOR01);
	            String[] arrSorts = sortStr != null ? sortStr.split(SEARCH_SEPARATOR01) : new String[arrFields.length];

	            for (int i = 0; i < arrFields.length; i++) {
	                String field = arrFields[i].trim();
	                String sortOption = i < arrSorts.length ? arrSorts[i].trim() : null;

	                AggsQueryBuilder aggsQuery = new AggsQueryBuilder().terms(field, Integer.parseInt(searchConfig.getAggsMaxCount()));

	                if (sortOption != null && !sortOption.isEmpty()) {
	                    String[] sortParams = sortOption.split(SEARCH_SEPARATOR02);
	                    if (sortParams.length == 2) {
	                        Map<String, String> sortMap = new HashMap<>();
	                        sortMap.put(sortParams[0], sortParams[1]);
	                        aggsQuery.order(sortMap);
	                    }
	                }

	                arrMap.put(field, aggsQuery.build());
	            }
	        }
	    }

	    return arrMap;
	}
	
	// ================================================================================
	// 메뉴(title 전용) 검색 - 신규 별도 메서드
	// title / title.ngram / title.keyword 3가지 서브필드 중 하나라도 매칭되면 결과 포함.
	// 기존 getSearchList() 및 관련 private 메서드는 전혀 수정하지 않음.
	// ================================================================================
	
	private static final String MENU_TITLE_FIELD = "title";
	private static final String MENU_TITLE_NGRAM_FIELD = "title.ngram";
	private static final String MENU_TITLE_KEYWORD_FIELD = "title.keyword";
	
	/**
	 * 메뉴(title 전용) 검색목록 요청
	 * @param paramVO
	 * @param traceId
	 */
	@Override
	public SearchResultVO getMenuSearchList(RequestSearchParamVO paramVO, String traceId) {
		SearchResultVO searchResult = new SearchResultVO();
		String query = null;
		String searchUri = null;
		
		try {
			if (paramVO.getCollapseCd() != null && "99999".equals(paramVO.getCollapseCd())) {
				searchUri = SEARCH_TYPE02;
				query = buildMenuMultiSearchQuery(paramVO, searchConfig.getIndexName().get(paramVO.getCollapseCd()).split(SEARCH_SEPARATOR01), traceId);
			} else {
				searchUri = searchConfig.getIndexName().get(paramVO.getCollapseCd()) + SEARCH_TYPE01;
				query = buildMenuSingleSearchQuery(paramVO, searchConfig.getIndexName().get(paramVO.getCollapseCd()), traceId);
			}
			
			log.debug("[{}] Menu SearchUri : {}", traceId, searchUri);
			log.debug("[{}] Menu Query : {}", traceId, query);
			
			searchResult = this.commonService.search(searchUri, query);
			searchResult.setCollapseCd(paramVO.getCollapseCd());
			searchResult.setKeyword(paramVO.getKwd());
			log.debug("[{}] Menu Search : {}", traceId, searchResult);
			
		} catch (Exception e) {
			this.commonService.handleSearchException(e);
		}
		
		return searchResult;
	}
	
	/**
	 * 메뉴 검색 _msearch 쿼리 생성
	 */
	private String buildMenuMultiSearchQuery(RequestSearchParamVO paramVO, String[] indices, String traceId) throws QueryBuilderException {
		StringBuilder queries = new StringBuilder();

		for (String index : indices) {
			DSLQueryBuilder qb = createMenuQuery(paramVO, index, traceId);
			queries.append(String.format("{\"index\":\"%s\"}\n%s\n", index, qb.buildJson()));
		}

		return queries.toString();
	}
	
	/**
	 * 메뉴 검색 _search 쿼리 생성
	 */
	private String buildMenuSingleSearchQuery(RequestSearchParamVO paramVO, String index, String traceId) throws QueryBuilderException {
		DSLQueryBuilder qb = createMenuQuery(paramVO, index, traceId);

		return qb.buildJson();
	}
	
	/**
	 * 메뉴(title 전용) 검색쿼리 생성
	 * - ES highlight 요청은 보내지 않음 (하이라이팅은 CommonServiceImpl.resultMenuSearchParse()에서
	 *   SearchUtil.getHighlightingStringMulti()로 앱 레벨에서 처리)
	 * - _source는 title 필드 하나만 반환하도록 고정
	 * - aggregation은 사용하지 않음 (메뉴 검색은 필터/집계 대상이 아님)
	 * - 검색어는 동의어 인덱스(idx_09_menu_syno)로 확장한 뒤(원본 포함) bool/should 쿼리에 반영
	 */
	private DSLQueryBuilder createMenuQuery(RequestSearchParamVO params, String index, String traceId) throws QueryBuilderException {
		
		// 검색어 목록 조회 (기존 getKeywordList() 재사용 - 읽기 전용 호출이라 기존 코드 변경 없음)
		List<String> kwdList = getKeywordList(params);
		
		// 동의어 확장 (원본 키워드 + idx_09_menu_syno에서 조회된 동의어들)
		List<String> expandedKwdList = this.commonService.getSynonymKeywordList(kwdList, traceId);
		
		// title / title.ngram / title.keyword 중 하나라도 매칭되면 되는 bool/should 쿼리 생성
		BoolQueryBuilder menuBoolQuery = new BoolQueryBuilder().builder();
		buildMenuBoolQuery(expandedKwdList, menuBoolQuery);
		
		DSLQueryBuilder dslQueryBuilder = new DSLQueryBuilder()
				.builder()
				.trackTotalHits(true)
				.from((params.getPage() - 1) * params.getSize())
				.size(params.getSize())
				.sort(searchConfig.getSort().get(index).get(params.getSort()), SEARCH_SEPARATOR01, SEARCH_SEPARATOR02)
				.query(menuBoolQuery)
				// .highlight(...) 호출 없음 - 앱 레벨 하이라이팅으로 대체
				.source(searchConfig.getIncludes().get(index).split(SEARCH_SEPARATOR01))
				.build();
		
		return dslQueryBuilder;
	}
	
	/**
	 * 메뉴 검색용 bool/should 쿼리 구성
	 * - 키워드별로 title(match) / title.ngram(match) / title.keyword(wildcard) 3가지 조건을 should로 추가
	 * - bool 쿼리에 must 조건 없이 should만 있는 경우, ES 기본 동작상 minimum_should_match=1 이 자동 적용됨
	 *   (즉 3가지 조건 중 어떤 것으로든 하나라도 매칭되면 히트)
	 */
	private void buildMenuBoolQuery(List<String> kwdList, BoolQueryBuilder menuBoolQuery) throws QueryBuilderException {
		
		for (String keyword : kwdList) {
			
			if (keyword == null || keyword.trim().isEmpty()) {
				continue;
			}
			
			// 1) title (nori 분석기) - 형태소 단위 매칭
			//    "직할부"처럼 사전에 없는 문자열은 nori가 문맥 없이 분석하면서
			//    일부 형태소(예: "할")를 활용어미(E)로 오인해 stoptag로 제거하는 경우가 있다.
			//    match(operator=and)를 쓰면 남은 토큰들이 문서 어디에 흩어져 있든 매칭되어
			//    무관한 문서가 오탐(false positive)될 수 있으므로, 순서/인접성을 요구하는
			//    match_phrase로 매칭 범위를 좁힌다.
			FullTextQueryBuilder titleMatch = new FullTextQueryBuilder()
					.matchPhrase(MENU_TITLE_FIELD, keyword);
			menuBoolQuery.should(titleMatch);
			
			// 2) title.ngram - 부분(substring) 매칭
			FullTextQueryBuilder titleNgramMatch = new FullTextQueryBuilder()
					.match(MENU_TITLE_NGRAM_FIELD, keyword, "and");
			menuBoolQuery.should(titleNgramMatch);
			
			// 3) title.keyword - wildcard 완전 substring 매칭
			//    BoolQueryBuilder.should()는 IQueryBuilder(함수형 인터페이스)를 받으므로,
			//    별도의 빌더 클래스를 추가하지 않고 람다로 즉석 wildcard 쿼리를 구성
			final String wildcardKeyword = "*" + keyword.trim() + "*";
			menuBoolQuery.should(() -> {
				Map<String, Object> valueMap = new HashMap<>();
				valueMap.put("value", wildcardKeyword);
				valueMap.put("case_insensitive", true);
				
				Map<String, Object> fieldMap = new HashMap<>();
				fieldMap.put(MENU_TITLE_KEYWORD_FIELD, valueMap);
				
				Map<String, Object> wildcardMap = new HashMap<>();
				wildcardMap.put("wildcard", fieldMap);
				
				return wildcardMap;
			});
		}
	}
	
}
