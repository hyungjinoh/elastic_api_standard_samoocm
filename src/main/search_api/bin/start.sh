#!/bin/bash
set -e

# 현재 스크립트의 위치를 기준으로 setEnv.sh 경로 설정
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
SET_ENV_PATH="$SCRIPT_DIR/setEnv.sh"

# setEnv.sh 실행
if [ -f "$SET_ENV_PATH" ]; then
    source "$SET_ENV_PATH"
else
    echo "ERROR: setEnv.sh not found at $SET_ENV_PATH"
    exit 1
fi

# 환경 변수 확인
if [ -z "$WAR_FILE_PATH" ]; then
    echo "WAR_FILE_PATH is not set. Please ensure setEnv.sh is correct."
    exit 1
fi

if [ -z "$DEPLOY_DIR" ]; then
    echo "DEPLOY_DIR is not set. Please ensure setEnv.sh is correct."
    exit 1
fi

if [ -z "$SERVER_ENV" ]; then
    # default : "local"
    SERVER_ENV="local"
fi

echo "Server environment set to $SERVER_ENV"

# 스프링 부트 애플리케이션 실행 여부 확인
PID_FILE="$DEPLOY_DIR/app.pid"

echo "Checking if the Spring Boot application is already running..."

if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat "$PID_FILE")

    if ps -p $OLD_PID > /dev/null 2>&1; then
        echo "Application already running with PID $OLD_PID. Deployment aborted."
        exit 1
    else
        echo "Stale PID file found. Process $OLD_PID is not running. Removing PID file..."
        rm -f "$PID_FILE"
    fi
fi

# pgrep 사용한 중복 프로세스 확인 (backup)
#RUNNING_PID=$(pgrep -f 'java.*elastic_api_standard.war')
RUNNING_PID=$(pgrep -f 'java.*elastic_api_standard.war' || true)
if [ -n "$RUNNING_PID" ]; then
    echo "Application is already running with PID $RUNNING_PID. Deployment aborted."
    exit 1
fi

# 배포 디렉토리 생성 (없으면)
if [ ! -d "$DEPLOY_DIR" ]; then
    echo "Creating deployment directory..."
    mkdir -p "$DEPLOY_DIR"
fi

# 기존 파일 삭제
echo "Deleting old deployment files..."
rm -rf "$DEPLOY_DIR"/*

# WAR 파일 복사
echo "Copying WAR file..."
cp "$WAR_FILE_PATH" "$DEPLOY_DIR"

# Java 버전 확인
JAVA_VERSION=$("$JAVA_HOME/bin/java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo "Detected Java version: $JAVA_VERSION"

# GC 설정
if [[ $JAVA_VERSION == 1.8* ]]; then
    # Java 8용 GC 설정
    JAVA_OPTS="-XX:+UseParallelGC -XX:InitiatingHeapOccupancyPercent=90 -XX:MaxGCPauseMillis=200 -XX:ParallelGCThreads=2"
    GC_LOG_OPTIONS="-Xloggc:$DEPLOY_DIR/gc_$(date +'%Y%m%d_%H%M%S').log -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=50M"
    echo "INFO: Using ParallelGC with options for Java 8: $JAVA_OPTS and GC log options: $GC_LOG_OPTIONS"

elif [[ $JAVA_VERSION == 17* ]]; then
    # Java 17 이상용 GC 설정
    JAVA_OPTS="-XX:SurvivorRatio=8 -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=256m -XX:+DisableExplicitGC -XX:MaxDirectMemorySize=512m -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:InitiatingHeapOccupancyPercent=40 -XX:ConcGCThreads=2 -XX:MaxGCPauseMillis=200 -XX:G1ReservePercent=10 -XX:G1HeapRegionSize=32m"
    GC_LOG_OPTIONS="-Xlog:gc*:file=$DEPLOY_DIR/gc_%t_%p.log:time,uptimemillis,level:filecount=5,filesize=50M"
    echo "INFO: Using G1GC with options for Java 17+: $JAVA_OPTS and GC log options: $GC_LOG_OPTIONS"

else
    echo "ERROR: Unsupported Java version: $JAVA_VERSION. Exiting."
    exit 1
fi

# 스프링 부트 애플리케이션 실행
echo "Starting Spring Boot application with GC logging enabled..."
cd "$DEPLOY_DIR"
echo $API_HOME
echo $SERVER_ENV

# [진단용 수정] 기존 코드는 stdout/stderr 를 /dev/null 로 버려서, JVM이 로깅 시스템을 초기화하기도
# 전에 죽으면(클래스패스 오류/포트충돌/설정로딩 실패 등) 원인을 전혀 알 수 없었다. 문제를 찾을
# 때까지는 아래처럼 $DEPLOY_DIR/stdout.log 파일로 받도록 바꿔서 직접 확인한다.
# 추가로: 1) $GC_LOG_OPTIONS 가 계산만 되고 실제 커맨드엔 빠져 있던 것을 추가했고,
#        2) spring.config.location 에 빠져 있던 file: 접두사를 추가했다(주석 처리된 예전 버전엔 있었음).
STDOUT_LOG="$DEPLOY_DIR/stdout.log"
nohup "$JAVA_HOME/bin/java" $JAVA_HEAP $JAVA_OPTS $GC_LOG_OPTIONS \
    -Dspring.config.location=file:$API_HOME/config/application-$SERVER_ENV.yml \
    -Dlogging.config=file:$API_HOME/config/logback-spring.xml \
    -jar elastic_api_standard.war --spring.profiles.active=$SERVER_ENV \
    > "$STDOUT_LOG" 2>&1 &

# 애플리케이션 PID 가져오기
PID=$!
echo $PID > app.pid

# [진단용 수정] 백그라운드로 던지기만 하면(기존 코드) JVM이 그 즉시 죽어도 스크립트는 그냥
# "완료"라고 찍고 끝나버린다. 몇 초 기다렸다가 프로세스가 살아있는지 확인하고, 죽었으면
# stdout.log 내용을 바로 화면에 보여준다.
sleep 3
if ps -p $PID > /dev/null 2>&1; then
    echo "[CHECK] PID $PID 가 3초 뒤에도 살아있습니다 — 정상적으로 기동 중일 가능성이 높습니다."
    echo "[CHECK] 계속 로그를 보려면: tail -f $STDOUT_LOG"
else
    echo "[CHECK][ERROR] PID $PID 가 이미 종료되었습니다! JVM이 즉시 죽었습니다."
    echo "[CHECK][ERROR] ----- $STDOUT_LOG 내용 -----"
    cat "$STDOUT_LOG"
    echo "[CHECK][ERROR] --------------------------------"
    rm -f app.pid
fi

echo "Deployment completed for $SERVER_ENV environment."