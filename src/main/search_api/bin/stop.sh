#!/bin/bash
set -e

# 환경 설정 파일 로드
source ./setEnv.sh

PID_FILE="$DEPLOY_DIR/app.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "[STOP] No running application found (PID file not exists)."
    exit 1
fi

PID=$(cat "$PID_FILE")

# PID 유효성 체크
if ! ps -p $PID > /dev/null 2>&1; then
    echo "[STOP] Process with PID $PID is not running."
    rm -f "$PID_FILE"
    exit 0
fi

echo "[STOP] Stopping application (PID: $PID)"

# 정상 종료 시도
kill $PID

# 10초간 종료 기다림
for i in {1..10}; do
    if ! ps -p $PID > /dev/null 2>&1; then
        echo "[STOP] Application stopped gracefully."
        rm -f "$PID_FILE"
        exit 0
    fi
    sleep 1
done

echo "[STOP] Graceful shutdown failed. Force killing..."

# 강제 종료
kill -9 $PID

if ps -p $PID > /dev/null 2>&1; then
    echo "[STOP] Failed to kill process (PID: $PID)"
    exit 1
fi

echo "[STOP] Process force-stopped successfully."
rm -f "$PID_FILE"
