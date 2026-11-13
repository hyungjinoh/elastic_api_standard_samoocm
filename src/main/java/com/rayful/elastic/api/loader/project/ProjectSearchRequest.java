package com.rayful.elastic.api.loader.project;

/**
 * [wjg-loader 병합] 프로젝트 검색 팝업 요청 (워크시트 필터 > "프로젝트명 검색" 버튼 전용, wjg-backend 원본 그대로).
 */
public record ProjectSearchRequest(
        String keyword,
        Integer page,
        Integer size
) {
    public int pageOrDefault() {
        return page == null ? 0 : page;
    }

    public int sizeOrDefault() {
        return size == null ? 10 : size;
    }
}
