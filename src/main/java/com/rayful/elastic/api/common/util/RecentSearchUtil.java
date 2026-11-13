package com.rayful.elastic.api.common.util;

import java.time.Duration;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 최근 검색어 식별자(RSID) 쿠키 관리 유틸
 *
 * 운영서버가 로드밸런싱 2대 구성이고 sticky session이 아니며 Redis 세션클러스터링도 없는
 * 환경에서는, WAS(Tomcat) 자체의 HttpSession에 상태를 두면 요청이 매번 다른 서버로 튈 때
 * 최근검색어가 사라지거나 뒤섞인다.
 *
 * 그래서 이 유틸은 서버에 어떤 상태도 남기지 않는다. 대신:
 * 1) 클라이언트를 식별하는 UUID 하나를 쿠키(RSID)에 담아 클라이언트가 들고 다니게 한다.
 *    (쿠키 값 자체가 식별정보 전부이므로, 어느 서버가 요청을 받아도 동일하게 해석 가능)
 * 2) 실제 최근검색어 "목록"은 두 서버가 공통으로 접근하는 ES(idx_recent_search)에 저장한다.
 *    (CommonServiceImpl.getRecentKeywordList()/saveRecentKeyword() 참고)
 *
 * 즉 "식별"은 클라이언트가 들고 다니고(쿠키), "상태"는 공유 저장소(ES)에 둔다 — 이 둘을
 * 분리하는 것이 sticky session이나 Redis 없이도 다중 서버에서 정상 동작하게 만드는 핵심이다.
 */
public class RecentSearchUtil {
	
	public static final String RECENT_SEARCH_COOKIE_NAME = "RSID";
	private static final int COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 365; // 1년
	
	private RecentSearchUtil() {
	}
	
	/**
	 * 요청 쿠키에서 최근검색어 식별자(RSID)를 조회한다. 없으면 새로 발급해서
	 * 응답 쿠키로 심어주고, 그 값을 반환한다.
	 * @param request
	 * @param response
	 * @return
	 */
	public static String resolveRecentSearchId(HttpServletRequest request, HttpServletResponse response) {
		
		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				if (RECENT_SEARCH_COOKIE_NAME.equals(cookie.getName())
						&& cookie.getValue() != null && !cookie.getValue().trim().isEmpty()) {
					return cookie.getValue();
				}
			}
		}
		
		String newId = UUID.randomUUID().toString();
		
		// [시큐어코딩] 쿠키 보안속성 강화
		// - HttpOnly: 스크립트 접근 차단(XSS로 인한 쿠키 탈취 방지)
		// - Secure: HTTPS 요청일 때 자동 적용(request.isSecure()). 운영(HTTPS)에서 평문 전송 방지.
		//           리버스 프록시(오프로딩) 뒤라면 X-Forwarded-Proto가 반영되도록 server.forward-headers-strategy 설정 권장.
		// - SameSite=Lax: CSRF 완화(교차 사이트 요청 시 쿠키 미전송 기본화)
		// jakarta.servlet.http.Cookie는 SameSite 표준 API가 없어 Spring ResponseCookie로 헤더 발급.
		ResponseCookie cookie = ResponseCookie.from(RECENT_SEARCH_COOKIE_NAME, newId)
				.path("/")
				.maxAge(Duration.ofSeconds(COOKIE_MAX_AGE_SECONDS))
				.httpOnly(true)
				.secure(request.isSecure())
				.sameSite("Lax")
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
		
		return newId;
	}
	
}
