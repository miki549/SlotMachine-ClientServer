@echo off
echo SlotMachine Server - Halozati Informaciok
echo =========================================
echo.

echo 1. Helyi IP cim:
echo ----------------
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| find "IPv4"') do echo %%a
echo.

echo 2. PC szerver teszt (46.139.211.149:8081):
echo -------------------------------------------
curl -s -w "HTTP Status: %%{http_code}\n" http://46.139.211.149:8081/api/auth/health 2>nul
if %errorlevel% == 0 (
    echo ✅ PC szerver elérhető (8081-es port)!
) else (
    echo ❌ PC szerver nem érhető el (8081-es port)
)
echo.

echo 3. Laptop szerver teszt (46.139.211.149:8082):
echo ----------------------------------------------
curl -s -w "HTTP Status: %%{http_code}\n" http://46.139.211.149:8082/api/auth/health 2>nul
if %errorlevel% == 0 (
    echo ✅ Laptop szerver elérhető (8082-es port)!
) else (
    echo ❌ Laptop szerver nem érhető el (8082-es port)
)
echo.

echo 4. Aktiv halozati kapcsolatok (8081-es és 8082-es port):
echo --------------------------------------------------------
netstat -an | find "8081"
netstat -an | find "8082"
echo.

echo 5. Tuzfal szabalyok ellenorzese:
echo --------------------------------
netsh advfirewall firewall show rule name="SlotMachine Server" dir=in
echo.

echo 6. Nyilvanos IP cim lekerese (internet kapcsolat szukseges):
echo ----------------------------------------------------------
echo Latogasd meg: https://whatismyipaddress.com/
echo Vagy hasznald: nslookup myip.opendns.com resolver1.opendns.com
echo.

echo 7. Router admin felulet:
echo ------------------------
echo Altalanos router IP cimek:
echo - 192.168.1.1
echo - 192.168.0.1
echo - 10.0.0.1
echo.

echo 8. Port ellenorzes kulso eszkozon:
echo ----------------------------------
echo Hasznald: https://www.portchecker.co/
echo Portok: 8081 (PC), 8082 (laptop)
echo IP: 46.139.211.149
echo.

pause
