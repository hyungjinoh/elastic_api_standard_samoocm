package com.rayful.elastic.api.standard.service;

import java.util.HashSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.rayful.elastic.api.common.builder.DSLQueryBuilder;
import com.rayful.elastic.api.common.builder.FullTextQueryBuilder;
import com.rayful.elastic.api.common.config.SearchConfig;
import com.rayful.elastic.api.common.exception.QueryBuilderException;
import com.rayful.elastic.api.common.service.ICommonService;
import com.rayful.elastic.api.common.util.SearchUtil;
import com.rayful.elastic.api.common.vo.SearchResultVO;
import com.rayful.elastic.api.standard.vo.RequestAutoParamVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Primary
public class StandardAutoServiceImpl implements IStandardAutoService {
	
	private static final String SEARCH_TYPE01 = "/_search";
	private static final String SEARCH_SEPARATOR01 = ","; 			// 설정파일 구분자(1차구분자)
	private static final String SEARCH_SEPARATOR02 = ":"; 			// 설정파일 구분자(2치구분자)
	
	@Autowired
	private SearchConfig searchConfig;
	
	@Autowired
	private ICommonService commonService;
	
	/**
	 * 자동완성
	 * @throws SearchExcepiton 
	 */
	@Override
	public SearchResultVO getAutoList(RequestAutoParamVO paramVO, String traceId) {
		SearchResultVO searchResult = new SearchResultVO();
		String query = null;
		String searchUri = null;
		
		try {

			searchUri = searchConfig.getIndexName().get(paramVO.getCollapseCd()) + SEARCH_TYPE01;
			query = buildSingleSearchQuery(paramVO, searchConfig.getIndexName().get(paramVO.getCollapseCd()));
			
			log.debug("[{}] AutoCompleteUri : {}", traceId, searchUri);
			log.debug("[{}] Query : {}", traceId, query);
			
			searchResult = this.commonService.search(searchUri, query);
			searchResult.setCollapseCd(paramVO.getCollapseCd());
			searchResult.setKeyword(paramVO.getKwd());
			log.debug("[{}] AutoComplete : {}", traceId, searchResult);
			
		} catch (Exception e) {
			this.commonService.handleSearchException(e);
		}
		
		return searchResult;
	}
	
	/**
	 * _search 검색쿼리 생성 시 createAutoQuery 함수 호출
	 */
	private String buildSingleSearchQuery(RequestAutoParamVO paramVO, String index) throws QueryBuilderException {
		DSLQueryBuilder qb = createAutoQuery(paramVO, index);

        return qb.buildJson();
    }
	
	/**
	 * 자동완성 쿼리 생성
	 */
	private DSLQueryBuilder createAutoQuery(RequestAutoParamVO params, String index) throws QueryBuilderException {
		
		FullTextQueryBuilder fullTextQueryBuilder = new FullTextQueryBuilder();
	    
	    // 검색쿼리
		fullTextQueryBuilder = buildAutoFullTextQuery(params, index);
	    
        // 전체 쿼리 구성
        DSLQueryBuilder dslQueryBuilder = new DSLQueryBuilder()
                .builder()
                .trackTotalHits(true)
                .from((params.getPage() - 1) * params.getSize())
                .size(params.getSize())
                .sort(searchConfig.getSort().get(index).get(params.getSort()), SEARCH_SEPARATOR01, SEARCH_SEPARATOR02)
                .query(fullTextQueryBuilder)
                .highlight(searchConfig.getHighlight().get(index).split(SEARCH_SEPARATOR01), searchConfig.getHighlightOption())
                .source(searchConfig.getIncludes().get(index).split(SEARCH_SEPARATOR01))
                .build();
        
        return dslQueryBuilder;
	}
	
	/**
	 * multimatch 검색어 쿼리 생성 - 자동완성
	 * @param params
	 * @param index
	 * @param mainBoolQuery
	 * @throws QueryBuilderException
	 */
	private FullTextQueryBuilder buildAutoFullTextQuery(RequestAutoParamVO params, String index) throws QueryBuilderException {
        
		List<String> searchAreaList = params.getSrchArea();
    	StringBuilder sb = new StringBuilder();
    	
    	boolean isChosung = SearchUtil.isChosungOnly(params.getKwd());
    	HashSet<String> fieldList = new HashSet<>();
    	
    	for (String searchArea : searchAreaList) {
    		
    		String searchAreaField = searchConfig.getFields().get(index).get(searchArea);
    		
    		if(searchAreaField != null && searchAreaField.length() > 0) {
    			String [] arrSearchAreaField = searchAreaField.split(SEARCH_SEPARATOR01);
    			
    			if(arrSearchAreaField != null && arrSearchAreaField.length > 0) {
    				for(String searchField : arrSearchAreaField) {
    					fieldList.add(searchField);
    				}
    			}
    		}
        }
    	
    	if(fieldList != null && fieldList.size() > 0) {
    		for (String field : fieldList) {
    			if(!isChosung) {
    				if(field.indexOf(".cho") > -1) {
    					continue;
    				}
    			}
    			
    			if (sb.length() > 0) {
                    sb.append(SEARCH_SEPARATOR01).append(field);
                } else {
                    sb.append(field);
                }
    		}
    	}
    	
    	return  new FullTextQueryBuilder()
                .multiMatch(
                		sb.toString().split(SEARCH_SEPARATOR01),
                		params.getKwd(),
                		searchConfig.getQueryOption().get("operator"),
                        searchConfig.getQueryOption().get("type")
                );
    }
	
}
