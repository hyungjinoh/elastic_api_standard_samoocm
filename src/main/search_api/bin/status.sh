#!/bin/bash
set -e

# setEnv.sh 실행
source ./setEnv.sh

PID_FILE="$DEPLOY_DIR/app.pid"

# PID 파일 존재 확인
if [ ! -f "$PID_FILE" ]; then
    echo "[STATUS] No PID file found. Application is not running."
    exit 1
fi

PID=$(cat "$PID_FILE")

# PID 값이 숫자인지 체크
if ! [[ "$PID" =~ ^[0-9]+$ ]]; then
    echo "[STATUS] Invalid PID found in PID file: $PID"
    exit 1
fi

# 프로세스 상태 확인
if ps -p $PID > /dev/null 2>&1; then
    echo "[STATUS] Application is RUNNING (PID: $PID)"
else
    echo "[STATUS] PID file exists but process is NOT running."
    echo "[STATUS] Cleaning up stale PID file..."
    rm -f "$PID_FILE"
    echo "[STATUS] Stale PID file removed."
fi
