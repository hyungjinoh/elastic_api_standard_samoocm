package com.rayful.elastic.api.standard.vo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class RequestSearchParamVO {

    private String kwd;             	// 검색어
    private String kwdOption;           // 검색어 옵션(and, or, phrase), 값이 없다면 설정파일의 query-option > operator 의 값을 기본값으로 한다.
    private List<String> srchArea;      // 검색영역(일반검색/결과내검색은 AREA01로 고정)
	                                    // AREA01:전체
	                                    // AREA02:제목
	                                    // AREA03:본문
	                                    // AREA04:첨부(파일명+파일내용)
    private String reSrchYn;        	// 결과 내 재검색 여부(Y/N)
    private List<String> prevKwd;   	// 이전 검색어(reSrchYn="Y" 일 경우에만 사용)
    private String collapseCd;      	// 탭(메뉴) 구분
	                                    // 99999:통합
	                                    // 10000:문서관리
	                                    // 20000:전자결재
	                                    // 30000:게시판
	                                    // 40000:사규
	                                    // 50000:조직정보
	                                    // 60000:사이트
	                                    // 70000:카테고리
    private Map<String, List<String>> acl01;     	// 권한정보 1
    private Map<String, List<String>> acl02;     	// 권한정보 2
    private String sort;            	// 정렬정보
                                    	// 10:정확도순(기본 DESC)
                                    	// 20:최신순  (기본 DESC, 21은 ASC), reg_dtm 필드 기준
                                    	// 30:가나다순(기본 DESC, 31은 ASC), title 필드 기준
                                    	// 40:조회수순(기본 DESC, 41은 ASC), 현재필드 없음
                                    	// 50:추천순  (기본 DESC, 51은 ASC), 현재필드 없음 
    private String regDtmYn;        	// 기간여부(Y/N)
                                    	// Y:range filter 사용, regStartDtm, regEndDtm 활용
                                    	// N:전체, range filter 생성X
    private String regStartDtm;     	// 기간정보(시작일)
    private String regEndDtm;       	// 기간정보(종료일)
    private String period;          	// 수정일(upd_dtm) 기준 기간 구분
    // 1:최근1주일, 2:최근1개월, 4:전체기간(필터없음), 99:직접입력(periodStartDtm, periodEndDtm 활용)
    // 값이 없거나 4이면 필터 미적용
    private String periodStartDtm;  	// 기간정보(시작일, period="99" 일 때만 사용, yyyy-MM-dd)
    private String periodEndDtm;    	// 기간정보(종료일, period="99" 일 때만 사용, yyyy-MM-dd)
    private List<String> attachedType; 	// 첨부파일 형식
	                                    // ATTACHED01:아래한글(.hwp)
	                                    // ATTACHED02:워드(.doc, .docx)
	                                    // ATTACHED03:파워포인트(.ppt, .pptx)
	                                    // ATTACHED04:엑셀(.xls, .xlsx)
	                                    // ATTACHED05:PDF(.pdf)
	                                    // ATTACHED06:텍스트파일(.txt)
	                                    // ATTACHED99:기타(ATTACHED01~06 을 제외한 모든 파일)
    private int size;               	// 검색결과 카운트
    private int page;               	// 검색결과 페이지
    private Map<String, Object> filterOption; // 필터 조건
	                                    // 예제
									    // Case 1 : 무조건 and
    									//"filterOption": { 
								    	//   "reg_year.keyword": ["2002", "2003"], 
								    	//  "attach_info.attach_ext": ["hwp", "doc"] 
								    	//} 
    									// Case2 : operator 으로 and, or 선택 가능
									    //"filterOption": {
								    	//  "reg_year.keyword": { "operator": "or", "values": ["2002", "2003"] },
								    	//  "attach_info.attach_ext": { "operator": "and", "values": ["hwp", "doc"] }
								    	//}

    @Override
    public String toString() {
        return "RequestSearchParamVO [kwd=" + kwd 
                + ", kwdOption=" + kwdOption 
                + ", srchArea=" + srchArea 
                + ", reSrchYn=" + reSrchYn 
                + ", prevKwd=" + prevKwd 
                + ", collapseCd=" + collapseCd 
                + ", acl01=" + acl01 
                + ", acl02=" + acl02 
                + ", sort=" + sort 
                + ", regDtmYn=" + regDtmYn 
                + ", regStartDtm=" + regStartDtm 
                + ", regEndDtm=" + regEndDtm 
                + ", attachedType=" + attachedType 
                + ", size=" + size 
                + ", page=" + page 
                + ", filterOption=" + filterOption 
                + "]";
    }

    public String validateAndSetDefaults() {
        // [버그 수정] kwd(검색어) 가 비어있으면 무조건 거부하던 규칙을 없앴다. 이 메서드는
        // StandardSearchController 가 모든 검색 요청에서 가장 먼저 호출하는데, 이 규칙 때문에
        // "검색어 없이 상세검색/사이드바 필터만으로 조회"가 ES 까지 가지도 못하고 전부 막혀 있었다
        // (프로젝트 탭에서 키워드 없이 ③ 폴더경로만 선택해 "조건필터 적용"을 눌러도 동작 안 하던 원인 —
        // 그리고 사실 사이드바 연도 클릭, 정렬 변경 등 키워드를 아직 안 친 상태의 모든 필터 조작이
        // 똑같이 막혀 있었다). kwd 가 비어있는 건 이제 "검색어 제한 없음, 필터만 적용"이라는
        // 정상적인 입력으로 취급한다. 길이 제한(100자)은 값이 있을 때만 검사한다.
        if (kwd != null && kwd.length() > 100) {
            return "kwd는 100자 이하로 입력해야 합니다.";
        }
    	// kwdOption 유효성 검사
        if (kwdOption != null && !kwdOption.isEmpty()) {
            List<String> allowedOptions = Arrays.asList("and", "or", "phrase");
            if (!allowedOptions.contains(kwdOption.toLowerCase())) {
                return "kwdOption은 and, or, phrase 중 하나여야 합니다.";
            }
        } 
        if ("Y".equals(reSrchYn) && (prevKwd == null || prevKwd.isEmpty())) {
            return "reSrchYn이 'Y'일 경우 prevKwd는 필수 파라미터입니다.";
        }
        if ("Y".equals(regDtmYn) && (regStartDtm == null || regStartDtm.isEmpty()) && (regEndDtm == null || regEndDtm.isEmpty())) {
            return "regDtmYn이 'Y'일 경우 regStartDtm 또는 regEndDtm는 필수 파라미터입니다.";
        }
        if (size <= 0) {
            size = 10; // 기본값 설정
        }
        if (page <= 0) {
            page = 1; // 기본값 설정
        }
        // 10,000건 초과 요청 방지
        if (size * page > 10000) {
        	return "요청 결과는 최대 10,000건 이하로 제한됩니다. size와 page 값을 조정해주세요.";
        }
        if (size > 1000) {
        	return "검색 요청 결과는 최대 1,000건을 초과 할 수 없습니다.";
        }
        if (collapseCd == null || collapseCd.isEmpty()) {
            collapseCd = "99999"; // 기본값 설정
        }
        if (sort == null || sort.isEmpty()) {
        	sort = "10"; // 기본값 설정
        }
        if (srchArea == null || srchArea.isEmpty()) {
            srchArea = Arrays.asList("AREA01"); // 기본값 설정
        }
        // period 유효성 검사 (수정일 upd_dtm 기간 필터)
        if (period != null && !period.isEmpty()) {
            List<String> allowedPeriods = Arrays.asList("1", "2", "4", "99");
            if (!allowedPeriods.contains(period)) {
                return "period는 1, 2, 4, 99 중 하나여야 합니다.";
            }
            if ("99".equals(period) && (periodStartDtm == null || periodStartDtm.isEmpty()) && (periodEndDtm == null || periodEndDtm.isEmpty())) {
                return "period가 '99'일 경우 periodStartDtm 또는 periodEndDtm는 필수 파라미터입니다.";
            }
        }

        return null;
    }

}
