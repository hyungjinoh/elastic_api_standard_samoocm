package com.rayful.elastic.api.standard.service;

import com.rayful.elastic.api.common.vo.SearchResultVO;
import com.rayful.elastic.api.standard.vo.RequestSearchParamVO;

public interface IStandardSearchService {
	
	/**
	 * 검색
	 * @param paramVO
	 * @param traceId
	 * @return
	 */
	SearchResultVO getSearchList(RequestSearchParamVO paramVO, String traceId);
	
	/**
	 * 메뉴(title 전용) 검색
	 * - title / title.ngram / title.keyword 3가지 서브필드 중 하나라도 매칭되면 결과에 포함
	 * - 기존 getSearchList()와는 완전히 분리된 별도 메서드
	 * @param paramVO
	 * @param traceId
	 * @return
	 */
	SearchResultVO getMenuSearchList(RequestSearchParamVO paramVO, String traceId);
	
	/**
	 * 통합검색 화면에서 개별 인덱스(섹션)의 정렬방식 변경(정확도/최신순) 시 사용
	 * - collapseCd에는 통합검색 코드(99999)가 아니라, 정렬을 바꾸려는 그 인덱스의 개별 collapseCd가 들어온다.
	 * - size는 클라이언트가 보낸 값을 그대로 쓴다(기본 3, "더보기" 시 더 큰 값으로 재요청 가능).
	 * - 내부적으로 기존 단일 인덱스 검색 경로(getSearchList의 else 분기)와 완전히 동일한 로직을 타므로,
	 *   신규 쿼리 생성 로직을 따로 만들지 않고 그대로 위임한다.
	 * @param paramVO
	 * @param traceId
	 * @return
	 */
	SearchResultVO getSectionSearchList(RequestSearchParamVO paramVO, String traceId);
}
