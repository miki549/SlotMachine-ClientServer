@echo off
echo SlotMachine Server - Windows Firewall Setup
echo ===========================================
echo.
echo WARNING: This script requires administrator privileges!
echo.

:device_selection
echo Which device are you running the server on?
echo.
echo 1. PC (port 8081)
echo 2. Laptop (port 8082)
echo.
set /p choice="Choose option (1 or 2): "

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
    echo Invalid choice! Please choose 1 or 2.
    echo.
    goto :device_selection
)

:setup_firewall
echo.
echo Adding firewall rule for port %PORT% (%DEVICE%)...
netsh advfirewall firewall add rule name="SlotMachine Server %DEVICE%" dir=in action=allow protocol=TCP localport=%PORT%

echo.
echo Success! Port %PORT% is now accessible from external networks.
echo.
echo Configured rule:
echo - Name: "SlotMachine Server %DEVICE%"
echo - Port: %PORT%
echo - Protocol: TCP
echo - Direction: Inbound
echo - Action: Allow
