@echo off
echo SlotMachine Kliens - Port Forwarding
echo ====================================
echo.

echo Szerver beallitasok:
echo - PC Szerver: http://46.139.211.149:8081
echo - Laptop Szerver: http://46.139.211.149:8082
echo.

echo MEGJEGYZES: 
echo - A kliens automatikusan észleli a futó szervert
echo - Először próbálja a PC szervert (8081), majd a laptop szervert (8082)
echo - Nincs szükség külön "helyi" vagy "külső" verzióra
echo.

echo Kliens inditasa...
echo.

echo Kliens inditasa Maven-nel...
mvn javafx:run

