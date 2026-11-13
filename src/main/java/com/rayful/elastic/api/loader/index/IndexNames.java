package com.rayful.elastic.api.loader.index;

import java.util.List;

/**
 * 색인 정의.
 *
 * [탭별 개별 인덱스 분리] 처음엔 단일 인덱스(idx_wjg_documents) + area 필드로 탭을 구분했는데,
 * elastic_api_standard 의 collapseCd 설정을 다음처럼(탭마다 별도 인덱스 + 통합검색은 콤마로 묶은
 * 멀티 인덱스) 쓰고 싶다는 요청에 맞춰 탭마다 실제로 분리된 인덱스를 쓰도록 바꿨다:
 *   "90000": idx_wjg_documents1,idx_wjg_documents2,idx_wjg_documents3,idx_wjg_documents4  (통합검색)
 *   "90001": idx_wjg_documents1   (프로젝트)
 *   "90002": idx_wjg_documents2   (지식정보)
 *   "90003": idx_wjg_documents3   (전사문서)
 *   "90004": idx_wjg_documents4   (부서문서)
 * (StandardSearchServiceImpl 이 collapseCd 에 매핑된 index-name 값을 ','로 split 해서 멀티서치하는
 *  기능을 이미 표준으로 지원하고 있어서, 설정값만 이렇게 바꾸면 코드 수정 없이 통합검색이 4개 인덱스를
 *  동시에 조회한다 — 자세한 내용은 DESIGN.md 참고.)
 */
public final class IndexNames {

    private IndexNames() { }

    public static final String DOCUMENTS_PROJECT = "idx_wjg_documents1";
    public static final String DOCUMENTS_KNOWLEDGE = "idx_wjg_documents2";
    public static final String DOCUMENTS_COMPANY = "idx_wjg_documents3";
    public static final String DOCUMENTS_DEPT = "idx_wjg_documents4";

    /** 4개 전부(색인 생성/삭제 등 일괄 작업에 사용). 순서는 config 의 90000 콤마 나열 순서와 맞춤. */
    public static final List<String> ALL_DOCUMENT_INDICES = List.of(
            DOCUMENTS_PROJECT, DOCUMENTS_KNOWLEDGE, DOCUMENTS_COMPANY, DOCUMENTS_DEPT);

    /** 문서의 area 값(PROJECT/KNOWLEDGE/COMPANY/DEPT)에 맞는 실제 색인명을 돌려준다. */
    public static String documentsIndexFor(String area) {
        if (area == null) return DOCUMENTS_PROJECT;
        return switch (area) {
            case "PROJECT" -> DOCUMENTS_PROJECT;
            case "KNOWLEDGE" -> DOCUMENTS_KNOWLEDGE;
            case "COMPANY" -> DOCUMENTS_COMPANY;
            case "DEPT" -> DOCUMENTS_DEPT;
            default -> DOCUMENTS_PROJECT;
        };
    }

    /**
     * 문서의 area 값에 맞는 elastic_api_standard collapseCd 를 돌려준다
     * (application-local.yml 의 search.index-name 설정과 반드시 일치해야 함: 90001=프로젝트 … 90004=부서문서).
     * 색인 시점에 각 문서의 collapsedCd 필드를 채워 넣는 데 사용(IndexingService 참고).
     */
    public static String collapseCdFor(String area) {
        if (area == null) return "90001";
        return switch (area) {
            case "PROJECT" -> "90001";
            case "KNOWLEDGE" -> "90002";
            case "COMPANY" -> "90003";
            case "DEPT" -> "90004";
            default -> "90001";
        };
    }

    /**
     * 프로젝트 마스터 색인 (워크시트필터_프로젝트다중선택_검색_UI: 프로젝트 검색 팝업 전용).
     * 문서 인덱스들의 projectNo 는 이 색인의 문서를 가리키는 참조키다. 지역/공사구분 등은 색인 시점에
     * 이 마스터에서 문서 쪽으로 비정규화하여 복사한다. 탭 분리와 무관하게 단일 인덱스로 유지.
     */
    public static final String PROJECTS = "wjg-projects";
}
