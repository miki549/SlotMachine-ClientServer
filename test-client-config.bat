@echo off
echo SlotMachine Kliens - Szerver Beallitas Teszt
echo ==========================================
echo.

echo Alapertelmezett szerver beallitasok:
echo - Szerver IP: 46.139.211.149
echo - Port: 8080
echo - Teljes URL: http://46.139.211.149:8080
echo.

echo Kliens inditasa...
echo.
echo FONTOS: A kliens most automatikusan a 46.139.211.149:8080 szervert fogja hasznalni!
echo.

pause

echo Kliens inditasa Maven-nel...
mvn javafx:run

pause
