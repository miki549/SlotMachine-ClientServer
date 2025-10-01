@echo off
echo Starting Slot Machine Server...
cd /d "%~dp0"
mvn spring-boot:run -Dspring-boot.run.main-class=com.example.slotmachine.server.SlotMachineServerApplication
pause
