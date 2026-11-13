package com.rayful.elastic.api.standard.controller;

import tools.jackson.databind.json.JsonMapper;
import com.rayful.elastic.api.common.config.SearchConfig;
import com.rayful.elastic.api.common.util.SearchUtil;
import com.rayful.elastic.api.common.vo.ApiResultExceptVO;
import com.rayful.elastic.api.common.vo.ResponseVO;

import lombok.extern.slf4j.Slf4j;

//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Slf4j
@Component
public class CommonInterceptor implements HandlerInterceptor {

//    @Autowired
//    private SearchConfig searchConfig;
    private final SearchConfig searchConfig;

    // 생성자 주입
    public CommonInterceptor(SearchConfig searchConfig) {
        this.searchConfig = searchConfig;
    }

    private static final String RETURN_TYPE = "application/json";
    private static final String SERVICE_TYPE = "interceptor";
    // Jackson 3: JSON 처리는 JsonMapper 사용 (ObjectMapper는 불변/추상화됨)
    private static final JsonMapper objectMapper = new JsonMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        HttpHeaders resHeaders = SearchUtil.getHttpHeaders(RETURN_TYPE);
        
        // 홈 경로("/")는 버전확인용으로 그냥 통과
        if ("/".equals(request.getRequestURI())) {
            return true;
        }

        String clientIp = getClientIp(request);
        String apiKeyHeader = request.getHeader("X-API-KEY");

        // IP 허용 체크
        if ("Y".equalsIgnoreCase(searchConfig.getAllowIp().getUse()) &&
                !searchConfig.getAllowIp().getIp().contains(clientIp)) {
            String msg = "Unauthorized IP: " + clientIp;
            log.error("[{}] {}", clientIp, msg);

            writeErrorResponse(response, HttpStatus.BAD_REQUEST.value(), msg, resHeaders);
            return false;
        }

        // API Key 체크
        if (!searchConfig.getApiKey().contains(apiKeyHeader)) {
            String msg = "Invalid API Key";
            log.error("[{}] {}", clientIp, msg);

            writeErrorResponse(response, HttpStatus.BAD_REQUEST.value(), msg, resHeaders);
            return false;
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = null;

        String[] headerNames = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headerNames) {
            ip = request.getHeader(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                ip = ip.split(",")[0].trim(); // 여러 IP가 있을 수 있음
                break;
            }
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }

    private void writeErrorResponse(HttpServletResponse response, int statusCode, String message, HttpHeaders resHeaders) throws IOException {
        ApiResultExceptVO failVO = new ApiResultExceptVO(statusCode, message, RETURN_TYPE, SERVICE_TYPE);
        ResponseVO responseBody = new ResponseVO(failVO);

        response.setStatus(HttpStatus.OK.value());  // 응답은 200이지만 내부 상태코드는 failVO에 포함됨
        response.setContentType(RETURN_TYPE);
        response.setCharacterEncoding("UTF-8");

        String json = objectMapper.writeValueAsString(responseBody);
        response.getWriter().write(json);
    }

}
