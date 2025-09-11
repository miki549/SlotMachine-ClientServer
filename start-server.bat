@echo off
echo Slot Machine Server indítása...
cd /d "%~dp0"
mvn spring-boot:run -Dspring-boot.run.main-class=com.example.slotmachine.server.SlotMachineServerApplication
pause
