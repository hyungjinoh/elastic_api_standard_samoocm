#!/bin/bash
set -e

# 환경 변수 설정
export API_HOME="/DATA/app/search_api"
export WAR_FILE_PATH="$API_HOME/elastic_api_standard.war"
export DEPLOY_DIR="$API_HOME/deploy"
export JAVA_HOME="/DATA/jdk-17"
export JAVA_HEAP="-Xms1024m -Xmx1024m"

# SERVER_ENV 설정(local, dev, stg, prod)
export SERVER_ENV="dev"

# PATH 환경 변수에 JAVA_HOME 추가
export PATH="$JAVA_HOME/bin:$PATH"

# 환경 변수를 출력하여 확인
echo "Environment variables set:"
echo "WAR_FILE_PATH=$WAR_FILE_PATH"
echo "DEPLOY_DIR=$DEPLOY_DIR"
echo "JAVA_HOME=$JAVA_HOME"
echo "PATH=$PATH"
