@echo off
echo SlotMachine Server - Halozati Informaciok
echo =========================================
echo.

echo 1. Helyi IP cim:
echo ----------------
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| find "IPv4"') do echo %%a
echo.

echo 2. Aktiv halozati kapcsolatok (8080-as port):
echo ----------------------------------------------
netstat -an | find "8080"
echo.

echo 3. Tuzfal szabalyok ellenorzese:
echo --------------------------------
netsh advfirewall firewall show rule name="SlotMachine Server" dir=in
echo.

echo 4. Nyilvanos IP cim lekerese (internet kapcsolat szukseges):
echo ----------------------------------------------------------
echo Latogasd meg: https://whatismyipaddress.com/
echo Vagy hasznald: nslookup myip.opendns.com resolver1.opendns.com
echo.

echo 5. Router admin felulet:
echo ------------------------
echo Altalanos router IP cimek:
echo - 192.168.1.1
echo - 192.168.0.1
echo - 10.0.0.1
echo.

echo 6. Port ellenorzes kulso eszkozon:
echo ----------------------------------
echo Hasznald: https://www.portchecker.co/
echo Port: 8080
echo IP: [nyilvanos IP cimed]
echo.

pause
