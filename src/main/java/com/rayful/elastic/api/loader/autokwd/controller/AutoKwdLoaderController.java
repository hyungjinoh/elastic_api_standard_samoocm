package com.rayful.elastic.api.loader.autokwd.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rayful.elastic.api.loader.autokwd.AutoKwdLoadResult;
import com.rayful.elastic.api.loader.autokwd.AutoKwdLoaderService;
import com.rayful.elastic.api.loader.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 자동완성 키워드(TB_AUTO_KWD) 일괄 적재 API. 초기 구축/재구축용(데모·일회성 적재 목적).
 */
@Tag(name = "자동완성 키워드 적재")
@RestController
@RequestMapping("/api/autokwd")
public class AutoKwdLoaderController {

    private static final Logger log = LoggerFactory.getLogger(AutoKwdLoaderController.class);

    private final AutoKwdLoaderService autoKwdLoaderService;

    public AutoKwdLoaderController(AutoKwdLoaderService autoKwdLoaderService) {
        this.autoKwdLoaderService = autoKwdLoaderService;
    }

    /**
     * 배포 이미지에 번들된 sql-batches/*.sql (batch_001.sql ~ batch_043.sql)을
     * 파일명 순서대로 isearch.TB_AUTO_KWD 에 적재한다.
     */
    @Operation(summary = "자동완성 키워드 일괄 적재(번들된 sql-batches 사용)")
    @PostMapping("/load")
    public ApiResponse<AutoKwdLoadResult> load() {
        log.info("[AutoKwdLoaderController] 자동완성 키워드 일괄 적재 시작 (classpath:sql-batches)");
        try {
            AutoKwdLoadResult result = autoKwdLoaderService.loadFromClasspath();
            return ApiResponse.ok(result);
        } catch (Exception e) {
            log.error("[AutoKwdLoaderController] 일괄 적재 실패: {}", e.getMessage(), e);
            return ApiResponse.fail(e.getMessage());
        }
    }

    /**
     * 서버 파일시스템의 외부 디렉터리에 있는 *.sql 파일을 파일명 순서대로 적재한다.
     * 예) POST /api/autokwd/load-dir?dir=/opt/isearch/sql_batches
     */
    @Operation(summary = "자동완성 키워드 일괄 적재(외부 디렉터리 지정)")
    @PostMapping("/load-dir")
    public ApiResponse<AutoKwdLoadResult> loadDir(@RequestParam("dir") String dir) {
        log.info("[AutoKwdLoaderController] 자동완성 키워드 일괄 적재 시작 (dir={})", dir);
        try {
            AutoKwdLoadResult result = autoKwdLoaderService.loadFromDirectory(dir);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            log.error("[AutoKwdLoaderController] 일괄 적재 실패: {}", e.getMessage(), e);
            return ApiResponse.fail(e.getMessage());
        }
    }

    /**
     * 적재 확인용 — SITE_ID 기준 TB_AUTO_KWD 건수 조회.
     * 예) GET /api/autokwd/count?siteId=0
     */
    @Operation(summary = "SITE_ID 기준 자동완성 키워드 건수 조회")
    @GetMapping("/count")
    public ApiResponse<Long> count(@RequestParam("siteId") String siteId) {
        try {
            long count = autoKwdLoaderService.countBySiteId(siteId);
            return ApiResponse.ok(count);
        } catch (Exception e) {
            log.error("[AutoKwdLoaderController] 건수 조회 실패: {}", e.getMessage(), e);
            return ApiResponse.fail(e.getMessage());
        }
    }
}
