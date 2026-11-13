package com.rayful.elastic.api.standard.vo;

import java.util.Arrays;
import java.util.List;

import lombok.Data;

@Data
public class RequestAutoParamVO {
	
	private String kwd;					// 검색어
	private List<String> srchArea;      // 검색영역(일반검색/결과내검색은 AREA01로 고정)
										// AREA01:전체
										// AREA02:앞단어매칭 - 미정
										// AREA03:끝단어매칭 - 미정
	private String collapseCd;			// 탭(메뉴) 구분
										// 90000:자동완성
	private String sort;				// 정렬정보
										// 10:정확도순(기본 DESC)
										// 30:가나다순(기본 DESC, 31은 ASC), word 필드 기준
	private int size;					// 검색결과 카운트
	private int page;					// 검색결과 페이지

	@Override
	public String toString() {
		return "RequestAutoParamVO [kwd=" + kwd 
				+ ", srchArea=" + srchArea 
				+ ", collapseCd=" + collapseCd 
				+ ", sort=" + sort 
				+ ", size=" + size 
				+ ", page=" + page 
				+ "]";
	}
	
	public String validateAndSetDefaults() {
        if (kwd == null || kwd.isEmpty()) {
            return "kwd는 필수 파라미터입니다.";
        }
        if (kwd.length() > 100) {
            return "kwd는 100자 이하로 입력해야 합니다.";
        }
        if (size == 0) {
            size = 10; // 기본값 설정
        }
        if (page == 0) {
            page = 1; // 기본값 설정
        }
        // 10,000건 초과 요청 방지
        if (size * page > 10000) {
        	return "요청 결과는 최대 10,000건 이하로 제한됩니다. size와 page 값을 조정해주세요.";
        }
        if (size > 100) {
        	return "자동완성 요청 결과는 최대 100건을 초과 할 수 없습니다.";
        }
        if (collapseCd == null || collapseCd.isEmpty()) {
            collapseCd = "90000"; // 기본값 설정
        }
        if (sort == null || sort.isEmpty()) {
        	sort = "10"; // 기본값 설정
        }
        if (srchArea == null || srchArea.isEmpty()) {
            srchArea = Arrays.asList("AREA01"); // 기본값 설정
        }
        return null;
    }
}
