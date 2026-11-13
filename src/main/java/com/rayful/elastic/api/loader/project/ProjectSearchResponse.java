package com.rayful.elastic.api.loader.project;

import java.util.List;

/**
 * [wjg-loader 병합] 프로젝트 검색 팝업 응답 (wjg-backend 원본 그대로).
 */
public record ProjectSearchResponse(
        long total,
        int page,
        int size,
        List<ProjectItem> items
) {
    public record ProjectItem(
            String projectNo,
            String projectName,
            String clientName,
            String projectStatus,
            String region,
            String constructionType,
            Integer projectYear
    ) { }
}
