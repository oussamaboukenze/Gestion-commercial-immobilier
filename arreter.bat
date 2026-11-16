@echo off
chcp 65001 >nul
cd /d "%~dp0backend"
echo Arret de GECIMMO (les donnees sont conservees)...
docker compose --profile apps down
pause
