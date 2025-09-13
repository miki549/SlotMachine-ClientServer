@echo off
echo SlotMachine Server - Windows Tuzfal Beallitas
echo ===========================================
echo.
echo FIGYELEM: Ez a script adminisztratori jogosultsagot igenyel!
echo.
pause

echo Tuzfal szabaly hozzaadasa a 8081-as porthoz...
netsh advfirewall firewall add rule name="SlotMachine Server" dir=in action=allow protocol=TCP localport=8081

if %errorlevel% == 0 (
    echo.
    echo ✓ Sikeres! A 8081-as port most mar elerheto kulso halozatrol.
    echo.
    echo Kovetkezo lepesek:
    echo 1. Router port forwarding beallitasa
    echo 2. Nyilvanos IP cim meghatarozasa
    echo 3. Kliens konfiguralas
    echo.
    echo Reszletes utmutato: EXTERNAL-ACCESS-SETUP.md
) else (
    echo.
    echo ❌ Hiba tortent! Ellenorizd, hogy adminisztratorként futtatod-e a scriptet.
    echo.
    echo Jobb kattintas a fajlon -> "Futtatás rendszergazdaként"
)

echo.
pause
