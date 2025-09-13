@echo off
echo Admin konzol indítása...
cd /d "%~dp0"
echo.
echo Ellenőrzés, hogy a szerver fut-e...
curl -s http://localhost:8081/api/auth/validate >nul 2>&1
if errorlevel 1 (
    echo HIBA: A szerver nem fut! Indítsd el először: start-server.bat
    pause
    exit /b 1
)
echo Szerver fut - OK
echo.
java -cp "target/classes;target/dependency/*" com.example.slotmachine.admin.ConsoleAdminApp
pause
