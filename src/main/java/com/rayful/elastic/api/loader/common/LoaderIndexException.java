package com.rayful.elastic.api.loader.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 검색 도메인 예외 + 전역 핸들러.
 * (secure coding: printStackTrace 금지 → SLF4J 로거로만 기록)
 */
public class LoaderIndexException extends RuntimeException {

    public LoaderIndexException(String message) {
        super(message);
    }

    public LoaderIndexException(String message, Throwable cause) {
        super(message, cause);
    }

    // [병합 시 주의] basePackages 로 범위를 loader 패키지로 한정 — 지정하지 않으면 이 catch-all
    // Exception 핸들러가 앱 전체(표준 검색 컨트롤러 포함)에 걸려 기존 에러 처리 방식과 충돌할 수 있다.
    @RestControllerAdvice(basePackages = "com.rayful.elastic.api.loader")
    public static class Handler {

        private static final Logger log = LoggerFactory.getLogger(Handler.class);

        @ExceptionHandler(LoaderIndexException.class)
        public ResponseEntity<ApiResponse<Void>> handleSearch(LoaderIndexException e) {
            // 변경 후 (원인 스택까지 기록)
            log.warn("검색 처리 오류: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiResponse<Void>> handleIllegalArg(IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleEtc(Exception e) {
            // 스택트레이스는 로그로만, 클라이언트에는 상세 노출 금지
            log.error("서버 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("검색 처리 중 오류가 발생했습니다."));
        }
    }
}
