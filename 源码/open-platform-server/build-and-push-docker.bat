@echo off
setlocal enabledelayedexpansion

REM 切换到当前 bat 文件所在目录，也就是 open-platform-server 目录
cd /d "%~dp0"

set IMAGE_NAME=docker-hosted.example.invalid/nstl/open-platform/service-backend
set IMAGE_TAG=1.0.0
set DOCKER_HOST=tcp://127.0.0.1:2375
set FULL_IMAGE=%IMAGE_NAME%:%IMAGE_TAG%

echo [1/3] Maven package...
call mvn -B -DskipTests clean package
if errorlevel 1 (
    echo Maven package failed.
    exit /b 1
)

echo [2/3] Docker build: %FULL_IMAGE%
docker -H %DOCKER_HOST% build -t %FULL_IMAGE% .
if errorlevel 1 (
    echo Docker build failed.
    exit /b 1
)

echo [3/3] Docker push: %FULL_IMAGE%
docker -H %DOCKER_HOST% push %FULL_IMAGE%
if errorlevel 1 (
    echo Docker push failed.
    exit /b 1
)

echo Done: %FULL_IMAGE%
endlocal
