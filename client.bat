@echo off
echo SlotMachine Client - Port Forwarding
echo ====================================
echo.

echo Server settings:
echo - PC Server: http://46.139.211.149:8081
echo - Laptop Server: http://46.139.211.149:8082
echo.

echo NOTE: 
echo - Client automatically detects running server
echo - First tries PC server (8081), then laptop server (8082)
echo - No need for separate "local" or "external" versions
echo.

echo Starting client...
echo.

echo Starting client with Maven...
mvn javafx:run

