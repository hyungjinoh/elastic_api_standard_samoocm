package com.rayful.elastic.api.common.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SHA-256 해시 유틸.
 *
 * [시큐어코딩 조치]
 * - (조치 전) 예외를 printStackTrace()로 콘솔에 그대로 노출(CWE-209 에러정보 노출) + 인코딩 미지정(CWE-176)
 * - (조치 후) 로거로 처리하고 스택트레이스를 표준 로그로만 남김, getBytes(UTF-8)로 인코딩 고정
 *
 * ※ 현재 이 클래스는 프로젝트 내에서 호출되지 않는 미사용(dead) 코드입니다.
 *   불필요하면 제거를 권장하며, 유지할 경우 아래와 같이 안전하게 둡니다.
 */
public class AuthMaker {

    private static final Logger log = LoggerFactory.getLogger(AuthMaker.class);

    private AuthMaker() {
    }

    // SHA-256 해시 함수를 사용하여 문자열을 해싱하는 메서드
    public static String hashWithSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // 바이트 배열을 16진수 문자열로 변환
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // 내부 오류만 로깅하고, 상세 예외 메시지는 호출측/응답으로 전파하지 않음
            log.error("SHA-256 해시 생성 실패", e);
            return null;
        }
    }
}
