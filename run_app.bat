@echo off
echo Starting School Website on Port 8081...
echo (Press Ctrl+C to stop)
echo.
call mvnw clean spring-boot:run
pause