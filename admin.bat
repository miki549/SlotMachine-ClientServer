@echo off
echo Starting admin console...
cd /d "%~dp0"
echo.
echo Checking if server is running...
echo Trying PC server (port 8081)...
curl -s http://46.139.211.149:8081/api/auth/health >nul 2>&1
if %errorlevel% == 0 (
    echo ✅ PC server available (port 8081)
    goto :start_admin
)
echo Trying laptop server (port 8082)...
curl -s http://46.139.211.149:8082/api/auth/health >nul 2>&1
if %errorlevel% == 0 (
    echo ✅ Laptop server available (port 8082)
    goto :start_admin
)
echo ❌ ERROR: No server is running! Start the server.
pause
exit /b 1

:start_admin
echo.
java -cp "target/classes;target/dependency/*" com.example.slotmachine.admin.ConsoleAdminApp
pause
