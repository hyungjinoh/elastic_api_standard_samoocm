package com.rayful.elastic.api.common.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * API 문서화 설정 (springdoc-openapi)
 *
 * [보안/버전 수정 이력]
 * - springfox-boot-starter(3.0.0) 제거: 2020년 이후 유지보수 중단, 내장 swagger-ui에
 *   CVE-2019-17495(CVSS 9.8) 등 심각한 XSS 취약점 확인되어 springdoc-openapi로 교체
 * - Spring Boot 3.x(jakarta) 승급에 맞춰 springdoc-openapi도 2.x 라인(jakarta 기반)으로 사용
 *   (1.x는 javax/Spring Boot 2.x 전용이라 3.x에서는 ClassNotFoundException 발생)
 * - Spring Boot 4.x 승급 시 springdoc 3.0.x 라인 사용(패키지 org.springdoc.core.models.*,
 *   io.swagger.v3.oas.models.* 는 2.x/3.x 동일하여 코드 변경 없음)
 *
 * [API Key 인증(Authorize 버튼)]
 * - CommonInterceptor가 모든 API 요청에서 헤더 X-API-KEY 를 검사하므로, Swagger UI의
 *   "Try it out" 으로 실제 엔드포인트를 호출하려면 요청에 X-API-KEY 헤더가 실려야 한다.
 * - 아래 APIKEY SecurityScheme + 전역 SecurityRequirement 를 추가하면 Swagger UI 상단에
 *   [Authorize] 버튼이 생기고, 입력한 값이 모든 요청에 X-API-KEY 헤더로 자동 첨부된다.
 * - 유효한 키는 application-*.yml 의 search.api-key 목록에 설정된 값이어야 한다.
 *   (local 은 기본이 비어 있으므로, 호출 테스트 전 search.api-key 에 값을 채워야 함)
 *
 * [기존 springfox 대비 달라진 점]
 * - Docket 빈이 필요 없음 - classpath에 springdoc-openapi-starter-webmvc-ui만 있으면 자동 설정됨
 * - Swagger UI 경로: /swagger-ui.html → /swagger-ui/index.html (springdoc 기본값)
 * - API 문서 JSON: /v2/api-docs → /v3/api-docs (OpenAPI 2.0 → 3.0)
 * - prod 프로파일에서 비활성화하는 방법도 @Profile 어노테이션이 아니라
 *   application-prod.yml의 springdoc.api-docs.enabled=false / springdoc.swagger-ui.enabled=false 로 이동
 */
@Configuration
public class SwaggerConfig {

    /** Swagger UI에서 인증에 사용할 스킴명 겸 실제 전송 헤더명. CommonInterceptor가 읽는 값과 동일해야 함. */
    private static final String API_KEY_HEADER = "X-API-KEY";

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("RAY.Search API")
                        .description("Elasticsearch 기반 통합검색 API")
                        .version("v1"))
                // [Authorize] 버튼용 APIKEY 스킴 등록 (헤더 방식)
                .components(new Components()
                        .addSecuritySchemes(API_KEY_HEADER,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name(API_KEY_HEADER)
                                        .description("CommonInterceptor가 검사하는 API Key. "
                                                + "application-*.yml 의 search.api-key 에 설정된 값을 입력하세요.")))
                // 전역 적용: 모든 엔드포인트에 X-API-KEY 헤더 요구 → Try it out 시 자동 첨부
                .addSecurityItem(new SecurityRequirement().addList(API_KEY_HEADER));
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                             .group("com.rayful.elastic")
                             .pathsToMatch("/**")
                             .packagesToScan("com.rayful.elastic")
                             .build();
    }
}
