@echo off
echo Admin konzol indítása...
cd /d "%~dp0"
echo.
echo Ellenőrzés, hogy a szerver fut-e...
echo Próbálkozás PC szerverrel (8081-es port)...
curl -s http://46.139.211.149:8081/api/auth/health >nul 2>&1
if %errorlevel% == 0 (
    echo ✅ PC szerver elérhető (8081-es port)
    goto :start_admin
)
echo Próbálkozás laptop szerverrel (8082-es port)...
curl -s http://46.139.211.149:8082/api/auth/health >nul 2>&1
if %errorlevel% == 0 (
    echo ✅ Laptop szerver elérhető (8082-es port)
    goto :start_admin
)
echo ❌ HIBA: Egyik szerver sem fut! Indítsd el a szervert.
pause
exit /b 1

:start_admin
echo.
java -cp "target/classes;target/dependency/*" com.example.slotmachine.admin.ConsoleAdminApp
pause
