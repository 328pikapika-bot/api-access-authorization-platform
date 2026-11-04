@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul

if /I "%~1"=="__backend__" goto backend_mode
if /I "%~1"=="__portal__" goto portal_mode
if /I "%~1"=="__admin__" goto admin_mode
goto launcher_mode

:launcher_mode
set "ROOT=%~dp0"
if not "%ROOT:~-1%"=="\" set "ROOT=%ROOT%\"
cd /d "%ROOT%" || goto cannot_cd_root

set "BACKEND_PORT=17300"
set "PORTAL_PORT=18300"
set "ADMIN_PORT=18301"
set "ACCESS_HOST=www.nstl.gov.cn"
set "ALT_ACCESS_HOST=nstl.gov.cn"
set "LOGIN_HOST=login.nstl.gov.cn"
set "PS_CMD=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
set "BACKEND_DIR=%ROOT%open-platform-server"
set "PORTAL_DIR=%ROOT%open-platform-ui"
set "ADMIN_DIR=%ROOT%open-platform-admin"
set "SCRIPTS_DIR=%ROOT%scripts"
set "PORT_CHECK_SCRIPT=%SCRIPTS_DIR%\ensure-port-available.ps1"
set "SETUP_NSTL_SCRIPT=%SCRIPTS_DIR%\setup-nstl-local-dev.ps1"
set "NSTL_CERT_PATH=%ROOT%certs\www.nstl.gov.cn.pfx"
set "MVNW_CMD=%ROOT%mvnw.cmd"
set "MVN_CMD=mvn.cmd"
set "BACKEND_TITLE=NSTL Interface Backend"
set "PORTAL_TITLE=NSTL Interface Portal"
set "ADMIN_TITLE=NSTL Interface Admin"
set "TEST_DB_HOST=127.0.0.1"
set "TEST_DB_PORT=3306"
set "TEST_REDIS_HOST=127.0.0.1"
set "TEST_REDIS_PORT=6381"
set "TEST_MINIO_HOST=127.0.0.1"
set "TEST_MINIO_PORT=9000"
set "PORTAL_URL=https://%ACCESS_HOST%:%PORTAL_PORT%/contents"
set "ADMIN_URL=https://%ACCESS_HOST%:%ADMIN_PORT%/login"
set "BACKEND_API_URL=http://127.0.0.1:%BACKEND_PORT%/api/v1/service/open_platform"

echo ============================================
echo   Interface Platform Domain Startup
echo ============================================
echo.
echo Access host        : %ACCESS_HOST%
echo Login service host : %LOGIN_HOST%
echo Portal URL         : %PORTAL_URL%
echo Admin URL          : %ADMIN_URL%
echo.

call :require_dir "%BACKEND_DIR%" "Backend directory open-platform-server" || goto launcher_error
call :require_dir "%PORTAL_DIR%" "Portal directory open-platform-ui" || goto launcher_error
call :require_dir "%ADMIN_DIR%" "Admin directory open-platform-admin" || goto launcher_error
call :require_file "%PORT_CHECK_SCRIPT%" "Port cleanup script" || goto launcher_error
call :require_file "%SETUP_NSTL_SCRIPT%" "NSTL local HTTPS setup script" || goto launcher_error
call :require_command "npm.cmd" "npm.cmd" || goto launcher_error
call :require_command "node.exe" "node.exe" || goto launcher_error
call :require_command "java.exe" "java.exe" || goto launcher_error

if exist "%MVNW_CMD%" (
  set "BACKEND_RUNNER=%MVNW_CMD%"
) else (
  call :require_command "%MVN_CMD%" "mvn.cmd" || goto launcher_error
  set "BACKEND_RUNNER=%MVN_CMD%"
)

echo [0/6] Ensuring local NSTL HTTPS host...
"%PS_CMD%" -NoProfile -ExecutionPolicy Bypass -File "%SETUP_NSTL_SCRIPT%" || goto launcher_error
if exist "%NSTL_CERT_PATH%" (
  echo     Local HTTPS certificate is ready: %NSTL_CERT_PATH%
)
call :verify_local_host "%ACCESS_HOST%" || goto launcher_error
echo.

echo [1/6] Closing previous interface-platform windows...
call :kill_window "%BACKEND_TITLE%"
call :kill_window "%PORTAL_TITLE%"
call :kill_window "%ADMIN_TITLE%"
timeout /t 2 /nobreak >nul
echo.

echo [2/6] Reclaiming project ports...
call :run_port_check %BACKEND_PORT% "open-platform-server" || goto launcher_error
call :run_port_check %PORTAL_PORT% "open-platform-ui" || goto launcher_error
call :run_port_check %ADMIN_PORT% "open-platform-admin" || goto launcher_error
echo.

echo [3/6] Clearing Vite cache...
if exist "%PORTAL_DIR%\node_modules\.vite" rd /s /q "%PORTAL_DIR%\node_modules\.vite"
if exist "%ADMIN_DIR%\node_modules\.vite" rd /s /q "%ADMIN_DIR%\node_modules\.vite"
echo.

echo [4/6] Checking test dependencies...
call :check_port %TEST_DB_HOST% %TEST_DB_PORT% "MariaDB"
call :check_port %TEST_REDIS_HOST% %TEST_REDIS_PORT% "Redis"
call :check_port %TEST_MINIO_HOST% %TEST_MINIO_PORT% "MinIO"
echo.

echo [5/6] Starting backend and both frontends...
call :launch_backend_window "%BACKEND_TITLE%" || goto launcher_error
call :launch_portal_window "%PORTAL_TITLE%" || goto launcher_error
call :launch_admin_window "%ADMIN_TITLE%" || goto launcher_error
timeout /t 8 /nobreak >nul
echo.

echo [6/6] Opening portal...
echo.

echo ============================================
echo   Startup complete.
echo ============================================
echo   Portal      : %PORTAL_URL%
echo   Admin       : %ADMIN_URL%
echo   Backend API : %BACKEND_API_URL%
echo.
echo   Required hosts
echo   127.0.0.1 %ACCESS_HOST%
echo   127.0.0.1 %ALT_ACCESS_HOST%
echo   127.0.0.1 %LOGIN_HOST%
echo.
echo   Access these URLs with HTTPS.
echo   Windows are started minimized by design.
echo ============================================
start "" %PORTAL_URL%
pause
exit /b 0

:require_dir
set "CHECK_PATH=%~1"
set "CHECK_LABEL=%~2"
if exist "%CHECK_PATH%\" exit /b 0
call :fail "%CHECK_LABEL% is missing: %CHECK_PATH%"
exit /b 1

:require_file
set "CHECK_PATH=%~1"
set "CHECK_LABEL=%~2"
if exist "%CHECK_PATH%" exit /b 0
call :fail "%CHECK_LABEL% is missing: %CHECK_PATH%"
exit /b 1

:require_command
set "CHECK_CMD=%~1"
set "CHECK_LABEL=%~2"
where.exe "%CHECK_CMD%" >nul 2>&1
if errorlevel 1 (
  call :fail "%CHECK_LABEL% was not found in PATH."
  exit /b 1
)
exit /b 0

:launch_backend_window
set "WINDOW_TITLE=%~1"
start "%WINDOW_TITLE%" /min "%ComSpec%" /k call "%~f0" __backend__
exit /b 0

:launch_portal_window
set "WINDOW_TITLE=%~1"
start "%WINDOW_TITLE%" /min "%ComSpec%" /k call "%~f0" __portal__
exit /b 0

:launch_admin_window
set "WINDOW_TITLE=%~1"
start "%WINDOW_TITLE%" /min "%ComSpec%" /k call "%~f0" __admin__
exit /b 0

:kill_window
set "TARGET_TITLE=%~1"
taskkill /FI "WINDOWTITLE eq %TARGET_TITLE%" /T /F >nul 2>&1
if errorlevel 1 (
  echo     No previous window found for %TARGET_TITLE%
) else (
  echo     Closed previous window: %TARGET_TITLE%
)
exit /b 0

:check_port
set "TARGET_HOST=%~1"
set "TARGET_PORT=%~2"
set "TARGET_NAME=%~3"
"%PS_CMD%" -NoProfile -Command "$client = [Net.Sockets.TcpClient]::new(); $async = $client.BeginConnect('%TARGET_HOST%', [int]'%TARGET_PORT%', $null, $null); if (-not $async.AsyncWaitHandle.WaitOne(3000, $false)) { $client.Close(); exit 1 }; try { $client.EndConnect($async); $client.Close(); exit 0 } catch { $client.Close(); exit 1 }" >nul 2>&1
if errorlevel 1 (
  echo     %TARGET_NAME% is not reachable on %TARGET_HOST%:%TARGET_PORT%
  exit /b 1
)
echo     %TARGET_NAME% is reachable on %TARGET_HOST%:%TARGET_PORT%
exit /b 0

:verify_local_host
set "VERIFY_HOST=%~1"
set "RESOLVED_IP="
set "HOST_CHECK_FILE=%TEMP%\interface-platform-host-check.txt"
if exist "%HOST_CHECK_FILE%" del /q "%HOST_CHECK_FILE%" >nul 2>&1
"%PS_CMD%" -NoProfile -Command "$result = Resolve-DnsName '%VERIFY_HOST%' -ErrorAction SilentlyContinue | Where-Object { $_.IPAddress } | Select-Object -First 1 -ExpandProperty IPAddress; if ($result) { [System.IO.File]::WriteAllText('%HOST_CHECK_FILE%', $result) }" >nul 2>&1
if exist "%HOST_CHECK_FILE%" (
  set /p RESOLVED_IP=<"%HOST_CHECK_FILE%"
  del /q "%HOST_CHECK_FILE%" >nul 2>&1
)
if /I "%RESOLVED_IP%"=="127.0.0.1" (
  echo     %VERIFY_HOST% resolves to %RESOLVED_IP%
  exit /b 0
)
if /I "%RESOLVED_IP%"=="::1" (
  echo     %VERIFY_HOST% resolves to %RESOLVED_IP%
  exit /b 0
)
echo [ERROR] %VERIFY_HOST% currently resolves to %RESOLVED_IP%
echo         Local NSTL login mode requires %VERIFY_HOST% to resolve to 127.0.0.1.
echo         Please switch your local hosts/SwitchHosts entry before retrying.
exit /b 1

:run_port_check
set "TARGET_PORT=%~1"
set "MATCH_TEXT=%~2"
"%PS_CMD%" -NoProfile -ExecutionPolicy Bypass -File "%PORT_CHECK_SCRIPT%" -Port %TARGET_PORT% -MatchText "%MATCH_TEXT%" -RootPath "%ROOT%"
exit /b %ERRORLEVEL%

:fail
echo [ERROR] %~1
exit /b 1

:run_backend_command
set "RUNNER_VALUE=%~1"
if exist "%RUNNER_VALUE%" (
  call "%RUNNER_VALUE%" spring-boot:run -Dspring-boot.run.profiles=test
  exit /b %ERRORLEVEL%
)
call %RUNNER_VALUE% spring-boot:run -Dspring-boot.run.profiles=test
exit /b %ERRORLEVEL%

:backend_mode
set "ROOT=%~dp0"
if not "%ROOT:~-1%"=="\" set "ROOT=%ROOT%\"
set "BACKEND_DIR=%ROOT%open-platform-server"
if exist "%ROOT%mvnw.cmd" (
  set "BACKEND_RUNNER=%ROOT%mvnw.cmd"
) else (
  set "BACKEND_RUNNER=mvn.cmd"
)

cd /d "%BACKEND_DIR%" || goto child_cd_error
set "APP_ENV=test"
set "SPRING_PROFILES_ACTIVE=test"
set "TEST_DB_HOST=127.0.0.1"
set "TEST_DB_PORT=3306"
set "TEST_DB_USERNAME=nstl_istic"
rem Set TEST_DB_PASSWORD in the environment before starting.
set "TEST_REDIS_CLUSTER_NODES=127.0.0.1:6381,127.0.0.1:6382,127.0.0.1:6383"
set "TEST_MINIO_ENDPOINT=http://127.0.0.1:9000"
set "TEST_MINIO_ACCESS_KEY=minio"
rem Set TEST_MINIO_SECRET_KEY in the environment before starting.
set "TEST_MOCKOON_URL=http://127.0.0.1:17301"
set "NSTL_LOCAL_FALLBACK_ENABLED=true"

echo ============================================
echo   NSTL Interface Backend
echo ============================================
echo Directory : %BACKEND_DIR%
echo Runner    : %BACKEND_RUNNER%
echo Profile   : %SPRING_PROFILES_ACTIVE%
echo.
call :run_backend_command "%BACKEND_RUNNER%"
set "CHILD_EXIT=%ERRORLEVEL%"
goto child_pause

:portal_mode
set "ROOT=%~dp0"
if not "%ROOT:~-1%"=="\" set "ROOT=%ROOT%\"
set "FRONTEND_DIR=%ROOT%open-platform-ui"
cd /d "%FRONTEND_DIR%" || goto child_cd_error

echo ============================================
echo   NSTL Interface Portal
echo ============================================
echo Directory : %FRONTEND_DIR%
echo URL       : https://www.nstl.gov.cn:18300/contents
echo.
set "VITE_PROXY_BACKEND=http://127.0.0.1:17300"
if not exist "node_modules" (
  echo node_modules not found. Installing dependencies...
  call npm.cmd install
  if errorlevel 1 (
    set "CHILD_EXIT=%ERRORLEVEL%"
    goto child_pause
  )
)
call npm.cmd run dev:nstl
set "CHILD_EXIT=%ERRORLEVEL%"
goto child_pause

:admin_mode
set "ROOT=%~dp0"
if not "%ROOT:~-1%"=="\" set "ROOT=%ROOT%\"
set "FRONTEND_DIR=%ROOT%open-platform-admin"
cd /d "%FRONTEND_DIR%" || goto child_cd_error

echo ============================================
echo   NSTL Interface Admin
echo ============================================
echo Directory : %FRONTEND_DIR%
echo URL       : https://www.nstl.gov.cn:18301/login
echo.
set "VITE_PROXY_BACKEND=http://127.0.0.1:17300"
if not exist "node_modules" (
  echo node_modules not found. Installing dependencies...
  call npm.cmd install
  if errorlevel 1 (
    set "CHILD_EXIT=%ERRORLEVEL%"
    goto child_pause
  )
)
call npm.cmd run dev:nstl
set "CHILD_EXIT=%ERRORLEVEL%"
goto child_pause

:child_cd_error
echo Failed to enter the requested working directory.
set "CHILD_EXIT=1"
goto child_pause

:child_pause
echo.
echo ============================================
echo   Window will remain open for diagnostics.
echo ============================================
pause
exit /b %CHILD_EXIT%

:cannot_cd_root
echo Failed to enter project root: %~dp0
goto pause_on_error

:launcher_error
goto pause_on_error

:pause_on_error
echo.
echo ============================================
echo   Launcher failed. Review the message above.
echo ============================================
pause
exit /b 1
