package com.rayful.elastic.api.loader.project;

import com.rayful.elastic.api.loader.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * [wjg-loader 병합] 프로젝트 검색 팝업 API (워크시트 필터 > "프로젝트명 검색" 버튼 전용, wjg-backend 원본 그대로).
 * 프런트엔드의 projectApi.js 가 그대로 호출하는 경로(/api/projects/search)를 유지하기 위해
 * 경로/요청·응답 모양을 한 글자도 바꾸지 않고 이 패키지로 이식했다.
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectSearchService projectSearchService;

    public ProjectController(ProjectSearchService projectSearchService) {
        this.projectSearchService = projectSearchService;
    }

    @GetMapping("/search")
    public ApiResponse<ProjectSearchResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        var req = new ProjectSearchRequest(keyword, page, size);
        return ApiResponse.ok(projectSearchService.search(req));
    }
}
