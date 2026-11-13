# =============================================================================
# elastic_api_standard - 런타임 전용 이미지 (사전 빌드된 WAR 복사 방식)
#
# 이 프로젝트는 <packaging>war</packaging> 이지만 spring-boot-maven-plugin:repackage 가
# provided 스코프(내장 Tomcat)를 WEB-INF/lib-provided 에 넣어 실행 가능한 WAR로 만든다.
# 따라서 외장 톰캣 없이 `java -jar elastic_api_standard.war` 로 임베디드 Tomcat 11 기동이 가능하다.
#
# [사전 준비] 먼저 WAR를 빌드해 둔다(폐쇄망이면 Nexus가 닿는 빌드머신에서):
#     mvn -DskipTests clean package
#   → target/elastic_api_standard.war 생성
#
# [폐쇄망] eclipse-temurin:17-jre 는 사내 도커 레지스트리에 미러링한 뒤 그 경로로 바꿔 사용.
#     예) FROM harbor.internal/library/eclipse-temurin:17-jre
# =============================================================================
FROM eclipse-temurin:17-jre

# ---- 메타/기본값 ----
ENV APP_HOME=/app \
    SERVER_PORT=8002 \
    # start.sh 의 Java 17 G1GC 옵션 + 힙(-Xms/-Xmx). 필요 시 docker run -e JAVA_OPTS=... 로 override
    JAVA_OPTS="-Xms1024m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=256m -XX:MaxDirectMemorySize=512m -XX:+DisableExplicitGC -Duser.timezone=Asia/Seoul"

WORKDIR ${APP_HOME}

# 로그 디렉토리(logback log.path 기본값 ./logs → /app/logs). 볼륨 마운트 대상.
RUN mkdir -p ${APP_HOME}/logs

# 비루트 사용자로 실행(보안)
RUN groupadd -r appuser && useradd -r -g appuser appuser \
    && chown -R appuser:appuser ${APP_HOME}

# 사전 빌드된 실행 가능 WAR 복사
COPY --chown=appuser:appuser target/elastic_api_standard.war ${APP_HOME}/app.war

USER appuser

EXPOSE 8002

# 포트 오픈 여부 기반 헬스체크(추가 바이너리 불필요, bash /dev/tcp 사용).
# 실제 애플리케이션 응답 검증은 호스트에서 `curl http://localhost:8002/` 로 수행(아래 README 참고).
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=10 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/${SERVER_PORT}' || exit 1

# exec 로 java 를 PID 1 로 실행(시그널 전달). 프로파일은 SPRING_PROFILES_ACTIVE 환경변수로 지정.
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar ${APP_HOME}/app.war"]