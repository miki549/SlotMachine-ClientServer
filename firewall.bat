@echo off
echo SlotMachine Server - Windows Tuzfal Beallitas
echo ===========================================
echo.
echo FIGYELEM: Ez a script adminisztratori jogosultsagot igenyel!
echo.

:device_selection
echo Melyik eszkozon futtatod a szervert?
echo.
echo 1. PC (8081-es port)
echo 2. Laptop (8082-es port)
echo.
set /p choice="Valassz opciot (1 vagy 2): "

if "%choice%"=="1" (
    set PORT=8081
    set DEVICE=PC
    goto :setup_firewall
) else if "%choice%"=="2" (
    set PORT=8082
    set DEVICE=Laptop
    goto :setup_firewall
) else (
    echo.
    echo Ervenytelen valasztas! Kerdlek valassz 1-et vagy 2-t.
    echo.
    goto :device_selection
)

:setup_firewall
echo.
echo Tuzfal szabaly hozzaadasa a %PORT%-as porthoz (%DEVICE%)...
netsh advfirewall firewall add rule name="SlotMachine Server %DEVICE%" dir=in action=allow protocol=TCP localport=%PORT%

echo.
echo Sikeres! A %PORT%-as port most mar elerheto kulso halozatrol.
echo.
echo Beallitott szabaly:
echo - Nev: "SlotMachine Server %DEVICE%"
echo - Port: %PORT%
echo - Protokol: TCP
echo - Irany: Bejovo
echo - Muvelet: Engedelyez
