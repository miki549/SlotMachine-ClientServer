@echo off
echo SlotMachine Kliens - Helyi Szerver Teszt
echo =======================================
echo.

echo Helyi szerver beallitasok:
echo - Szerver IP: localhost (127.0.0.1)
echo - Port: 8080
echo - Teljes URL: http://localhost:8080
echo.

echo FONTOS: Eloszor inditsd el a szervert (server.bat), majd ezt a klienst!
echo.

echo Helyi szerver ellenorzese...
curl -s http://localhost:8080/api/auth/health >nul 2>&1
if %errorlevel% == 0 (
    echo ✅ Helyi szerver elérhető!
) else (
    echo ❌ Helyi szerver nem érhető el! Ellenőrizd, hogy fut-e a szerver.
    echo    Indítsd el a server.bat fájlt egy másik parancssorban.
    pause
    exit /b 1
)
echo.

echo Kliens inditasa...
echo.

pause

echo Kliens inditasa Maven-nel...
mvn javafx:run

pause
