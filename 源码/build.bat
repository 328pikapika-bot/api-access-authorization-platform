@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ============================================================
::  接口平台 — 一键构建 & 推送脚本
::  支持三个服务：web-frontend / web-admin-frontend / service-backend
:: ============================================================

cd /d "%~dp0"

:: ---------- 读取上次版本号 ----------
set VERSION_FILE=%~dp0.last_version.txt
set LAST_VERSION=
if exist "%VERSION_FILE%" (
    set /p LAST_VERSION=<"%VERSION_FILE%"
)

:: ---------- 用户输入版本号 ----------
echo.
echo ================================================
echo   接口平台 Docker 镜像一键构建
echo ================================================
echo.
if defined LAST_VERSION (
    echo 上次构建版本: %LAST_VERSION%
) else (
    echo 未找到上次构建记录
)
echo.

set /p INPUT_VERSION="请输入版本号（如 1.0.2）[直接回车使用上次版本]: "

if "!INPUT_VERSION!"=="" (
    if defined LAST_VERSION (
        set VERSION=!LAST_VERSION!
        echo 使用上次版本: !VERSION!
    ) else (
        echo [错误] 未输入版本号且没有上次构建记录，退出。
        pause
        exit /b 1
    )
) else (
    set VERSION=!INPUT_VERSION!
)

:: ---------- 保存版本号 ----------
echo !VERSION!>"%VERSION_FILE%"

echo.
echo ================================================
echo   开始构建版本: !VERSION!
echo ================================================
echo.

set REGISTRY=docker-hosted.example.invalid/nstl/open-platform

:: ============================================================
::  1. 构建 web-frontend (用户端前端)
:: ============================================================
echo [1/3] 构建 web-frontend ...
cd /d "%~dp0open-platform-ui"
docker build -t %REGISTRY%/web-frontend:!VERSION! .
if errorlevel 1 (
    echo [失败] web-frontend 构建失败！
    pause
    exit /b 1
)
echo [完成] web-frontend:!VERSION!
echo.

:: ============================================================
::  2. 构建 web-admin-frontend (管理后台前端)
:: ============================================================
echo [2/3] 构建 web-admin-frontend ...
cd /d "%~dp0open-platform-admin"
docker build -t %REGISTRY%/web-admin-frontend:!VERSION! .
if errorlevel 1 (
    echo [失败] web-admin-frontend 构建失败！
    pause
    exit /b 1
)
echo [完成] web-admin-frontend:!VERSION!
echo.

:: ============================================================
::  3. 构建 service-backend (后端服务，需先 Maven 打包)
:: ============================================================
echo [3/3] 构建 service-backend ...
cd /d "%~dp0open-platform-server"
echo   -> Maven 打包中...
call mvn clean package -DskipTests -q
if errorlevel 1 (
    echo [失败] Maven 打包失败！
    pause
    exit /b 1
)
echo   -> Docker 构建中...
docker build -t %REGISTRY%/service-backend:!VERSION! .
if errorlevel 1 (
    echo [失败] service-backend 构建失败！
    pause
    exit /b 1
)
echo [完成] service-backend:!VERSION!
echo.

:: ============================================================
::  构建完成，输出推送命令
:: ============================================================
echo ================================================
echo   全部构建完成！版本: !VERSION!
echo ================================================
echo.
echo 本地镜像列表:
docker images %REGISTRY%/* --format "  {{.Repository}}:{{.Tag}}  {{.Size}}"
echo.
echo ================================================
echo  推送命令（复制执行）:
echo ================================================
echo.
echo   docker push %REGISTRY%/web-frontend:!VERSION!
echo   docker push %REGISTRY%/web-admin-frontend:!VERSION!
echo   docker push %REGISTRY%/service-backend:!VERSION!
echo.
echo ================================================
echo  一键推送（复制下面一整行执行）:
echo ================================================
echo.
echo   docker push %REGISTRY%/web-frontend:!VERSION! ^&^& docker push %REGISTRY%/web-admin-frontend:!VERSION! ^&^& docker push %REGISTRY%/service-backend:!VERSION!
echo.

cd /d "%~dp0"
pause
