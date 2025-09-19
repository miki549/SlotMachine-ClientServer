@echo off
echo SlotMachine Server - Network Information
echo =========================================
echo.

echo 1. Local IP address:
echo ----------------
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| find "IPv4"') do echo %%a
echo.

echo 2. PC server test (46.139.211.149:8081):
echo -------------------------------------------
curl -s -w "HTTP Status: %%{http_code}\n" http://46.139.211.149:8081/api/auth/health 2>nul
if %errorlevel% == 0 (
    echo ✅ PC server available (port 8081)!
) else (
    echo ❌ PC server not available (port 8081)
)
echo.

echo 3. Laptop server test (46.139.211.149:8082):
echo ----------------------------------------------
curl -s -w "HTTP Status: %%{http_code}\n" http://46.139.211.149:8082/api/auth/health 2>nul
if %errorlevel% == 0 (
    echo ✅ Laptop server available (port 8082)!
) else (
    echo ❌ Laptop server not available (port 8082)
)
echo.

echo 4. Active network connections (ports 8081 and 8082):
echo --------------------------------------------------------
netstat -an | find "8081"
netstat -an | find "8082"
echo.

echo 5. Firewall rules check:
echo --------------------------------
netsh advfirewall firewall show rule name="SlotMachine Server" dir=in
echo.

echo 6. Public IP address lookup (internet connection required):
echo ----------------------------------------------------------
echo Visit: https://whatismyipaddress.com/
echo Or use: nslookup myip.opendns.com resolver1.opendns.com
echo.

echo 7. Router admin interface:
echo ------------------------
echo Common router IP addresses:
echo - 192.168.1.1
echo - 192.168.0.1
echo - 10.0.0.1
echo.

echo 8. Port check on external device:
echo ----------------------------------
echo Use: https://www.portchecker.co/
echo Ports: 8081 (PC), 8082 (laptop)
echo IP: 46.139.211.149
echo.

pause
