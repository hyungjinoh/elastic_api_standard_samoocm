package com.rayful.elastic.api.loader.index.loader;

import com.rayful.elastic.api.loader.domain.ProjectDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * 데모용 프로젝트 마스터 샘플 데이터.
 * "워크시트필터_프로젝트다중선택_검색_UI" 목업의 프로젝트 검색 팝업(페이징) 데모를 위해
 * 목업에 등장하는 9개 프로젝트 + 페이징 데모용 자동생성 프로젝트로 총 {@link #TOTAL_COUNT}건을 구성한다.
 * 실제 색인 소스(프로젝트 관리 시스템 RDBMS)로 교체되는 지점.
 *
 * SampleDataLoader(문서)가 이 클래스가 반환하는 프로젝트 목록을 그대로 참조해서 파일 문서를 생성하므로,
 * 여기서 건수를 바꾸면 문서 쪽 생성량도 함께 따라간다(단일 진실 공급원).
 */
public final class ProjectSampleDataLoader {

    private ProjectSampleDataLoader() { }

    /** 프로젝트 마스터 전체 건수. 프로젝트 검색 팝업(size=5)에서 100/5=20페이지 데모. */
    public static final int TOTAL_COUNT = 100;

    private static final String[] REGIONS = {
            "서울특별시", "부산광역시", "대구광역시", "인천광역시", "광주광역시",
            "대전광역시", "울산광역시", "세종특별자치시", "경기도", "강원특별자치도",
            "충청북도", "충청남도", "전라북도", "전라남도", "경상북도", "경상남도", "제주특별자치도",
    };
    private static final String[] CONSTRUCTION_TYPES = {
            "산업", "공공", "민간", "주택", "개발", "리모델링", "용역/조사", "기타",
    };
    private static final String[] CLIENT_TYPE_CODES = {
            "기업건", "관공서(정부)", "공기업", "관공서(지자체)", "기타",
    };
    private static final String[] STATUSES = { "진행", "완료" };
    private static final String[] CLIENTS = {
            "삼성전자(주)", "무주군청", "공주시청", "성균관대학교", "수협중앙회",
            "한국전력공사", "인천국제공항공사", "LH한국토지주택공사", "현대자동차(주)", "국방부",
    };

    public static List<ProjectDocument> sampleProjects() {
        List<ProjectDocument> list = new ArrayList<>();

        // --- 목업에 명시적으로 등장하는 프로젝트 (다중선택 데모용) ---
        list.add(p("23079S", "23-25년 행복 Hook up 관리(삼성 평택)", "삼성전자(주)", "진행",
                "경기도", "산업", "기업건", 2023));
        list.add(p("23078M", "무주군 노후하수관로 개량 통합 CM", "무주군청", "진행",
                "전라북도", "공공", "관공서(지자체)", 2023));
        list.add(p("23077M", "우성바움 하수도 증설 통합 CM(공주)", "공주시청", "진행",
                "충청남도", "공공", "관공서(지자체)", 2023));
        list.add(p("20011M", "성균관대 제2경영관 신축 CM", "성균관대학교", "완료",
                "서울특별시", "민간", "기업건", 2021));
        list.add(p("08044D", "노량진 수산물도매시장 CM", "수협중앙회", "완료",
                "서울특별시", "공공", "공기업", 2019));

        // 기존 SearchDocument 샘플과 이어지는 프로젝트도 마스터에 포함(정합성 유지)
        list.add(p("20059D", "양재 로데오 패션타운 신축 감리", "현대자동차(주)", "완료",
                "서울특별시", "민간", "기업건", 2002));
        list.add(p("20204M", "0000 신축공사", "한국전력공사", "진행",
                "경기도", "산업", "공기업", 2020));
        list.add(p("08028D", "충주의료원 이전신축 BTL CM", "국방부", "완료",
                "충청북도", "공공", "관공서(정부)", 2008));
        list.add(p("090020D", "법원공무원교육원 생활관", "국방부", "완료",
                "세종특별자치시", "공공", "관공서(정부)", 2009));

        // --- 페이징 데모용 자동 생성 (총 TOTAL_COUNT 건에 맞춰 나머지 채움) ---
        int named = list.size();
        int autoCount = TOTAL_COUNT - named;
        for (int i = 1; i <= autoCount; i++) {
            String no = String.format("AUTO-%04d", i);
            String region = REGIONS[i % REGIONS.length];
            String ctype = CONSTRUCTION_TYPES[i % CONSTRUCTION_TYPES.length];
            String clientCode = CLIENT_TYPE_CODES[i % CLIENT_TYPE_CODES.length];
            String status = STATUSES[i % STATUSES.length];
            String client = CLIENTS[i % CLIENTS.length];
            int year = 2015 + (i % 10);
            list.add(p(no, String.format("%s %d년 자동생성 프로젝트 %03d", region, year, i),
                    client, status, region, ctype, clientCode, year));
        }

        return list;
    }

    private static ProjectDocument p(String no, String name, String client, String status,
                                      String region, String constructionType, String clientTypeCode,
                                      int year) {
        ProjectDocument d = new ProjectDocument();
        d.setProjectNo(no);
        d.setProjectName(name);
        d.setClientName(client);
        d.setProjectStatus(status);
        d.setRegion(region);
        d.setConstructionType(constructionType);
        d.setClientTypeCode(clientTypeCode);
        d.setProjectYear(year);
        d.setSubordinate(false);
        d.setRegDate("2023-01-01T09:00:00");
        return d;
    }
}
