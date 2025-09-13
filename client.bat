@echo off
echo SlotMachine Kliens - Kulso Szerver
echo ===================================
echo.

echo Kulso szerver beallitasok:
echo - Szerver IP: 46.139.211.149
echo - Port: 8081
echo - Teljes URL: http://46.139.211.149:8081
echo.

echo MEGJEGYZES: 
echo - Helyi szerver teszteléshez használd: client-local.bat
echo - A kliens automatikusan észleli a helyi szervert, ha elérhető
echo.

echo Kliens inditasa...
echo.

pause

echo Kliens inditasa Maven-nel...
mvn javafx:run

pause
