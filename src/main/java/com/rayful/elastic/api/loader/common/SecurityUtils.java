package com.rayful.elastic.api.loader.common;

/**
 * [wjg-loader 병합] 화이트리스트 기반 입력 검증 유틸(wjg-backend 원본 그대로).
 * 페이지/사이즈 상한 검증(대량 조회로 인한 자원 고갈 방지)에 사용.
 */
public final class SecurityUtils {

    private SecurityUtils() { }

    public static int clampSize(int size, int max) {
        if (size < 1) return 1;
        return Math.min(size, max);
    }

    public static int clampPage(int page) {
        return Math.max(page, 0);
    }
}
