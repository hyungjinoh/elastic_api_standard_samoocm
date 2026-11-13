package com.rayful.elastic.api.loader.index.controller;

import com.rayful.elastic.api.loader.common.ApiResponse;
import com.rayful.elastic.api.loader.index.IndexInitializer;
import com.rayful.elastic.api.loader.index.IndexingService;
import com.rayful.elastic.api.loader.index.ProjectIndexInitializer;
import com.rayful.elastic.api.loader.index.ProjectIndexingService;
import com.rayful.elastic.api.loader.index.loader.ProjectSampleDataLoader;
import com.rayful.elastic.api.loader.index.loader.SampleDataLoader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 색인 관리 API. 데모/초기 구축용.
 */
@Tag(name = "색인 관리")
@RestController
@RequestMapping("/api/index")
public class IndexController {

    private final IndexInitializer initializer;
    private final IndexingService indexingService;
    private final ProjectIndexInitializer projectInitializer;
    private final ProjectIndexingService projectIndexingService;

    public IndexController(IndexInitializer initializer, IndexingService indexingService,
                            ProjectIndexInitializer projectInitializer,
                            ProjectIndexingService projectIndexingService) {
        this.initializer = initializer;
        this.indexingService = indexingService;
        this.projectInitializer = projectInitializer;
        this.projectIndexingService = projectIndexingService;
    }

    @Operation(summary = "색인 생성(없으면)")
    @PostMapping("/init")
    public ApiResponse<String> init() {
        initializer.createIfAbsent();
        projectInitializer.createIfAbsent();
        return ApiResponse.ok("색인 준비 완료");
    }

    @Operation(summary = "색인 재생성(매핑 변경 시)")
    @PostMapping("/recreate")
    public ApiResponse<String> recreate() {
        initializer.recreate();
        projectInitializer.recreate();
        return ApiResponse.ok("색인 재생성 완료");
    }

    @Operation(summary = "샘플 데이터 색인 (문서 + 프로젝트 마스터)")
    @PostMapping("/sample")
    public ApiResponse<Integer> loadSample() {
        initializer.createIfAbsent();
        int count = indexingService.bulkIndex(SampleDataLoader.sampleDocuments());

        // 프로젝트 검색 팝업(다중선택)이 바로 동작하도록 프로젝트 마스터도 함께 부트스트랩
        projectInitializer.createIfAbsent();
        projectIndexingService.bulkIndex(ProjectSampleDataLoader.sampleProjects());

        return ApiResponse.ok(count);
    }

    @Operation(summary = "프로젝트 마스터만 재생성(매핑 변경 시)")
    @PostMapping("/projects/recreate")
    public ApiResponse<String> recreateProjects() {
        projectInitializer.recreate();
        return ApiResponse.ok("프로젝트 색인 재생성 완료");
    }

    @Operation(summary = "프로젝트 마스터 샘플만 재적재")
    @PostMapping("/projects/sample")
    public ApiResponse<Integer> loadProjectSample() {
        projectInitializer.createIfAbsent();
        int count = projectIndexingService.bulkIndex(ProjectSampleDataLoader.sampleProjects());
        return ApiResponse.ok(count);
    }
}
