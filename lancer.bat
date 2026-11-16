@echo off
chcp 65001 >nul
title GECIMMO - lancement
cd /d "%~dp0backend"

echo ============================================================
echo   GECIMMO - lancement complet (Docker uniquement)
echo ============================================================
echo.

docker info >nul 2>&1
if errorlevel 1 (
  echo [ERREUR] Docker ne repond pas. Ouvrez Docker Desktop, attendez qu'il soit pret, puis relancez ce fichier.
  pause
  exit /b 1
)

echo [1/3] Construction et demarrage des conteneurs...
echo       Premier lancement : 10 a 20 minutes (telechargements Maven, npm et images Docker).
echo.
docker compose --profile apps up -d --build
if errorlevel 1 (
  echo.
  echo [ERREUR] Le lancement a echoue. Copiez les messages ci-dessus pour diagnostiquer le probleme.
  pause
  exit /b 1
)

echo.
echo [2/3] Attente de la gateway (http://localhost:8080)...
set /a essais=0
:attente
set /a essais+=1
curl -s -o nul -f http://localhost:8080/actuator/health
if not errorlevel 1 goto pret
if %essais% geq 60 goto lent
timeout /t 5 /nobreak >nul
goto attente

:lent
echo Les services mettent du temps a demarrer. Etat des conteneurs :
docker compose --profile apps ps
goto ouvrir

:pret
echo Gateway prete.

:ouvrir
echo.
echo [3/3] Ouverture de GECIMMO : http://localhost:5173
echo       Utilisateurs : adv1, comptable1, resp1, daf1, direction1, admin1, sav1, commercial1
echo       Mot de passe : Gecimmo2026!
echo       (laissez 1 minute aux services pour s'enregistrer si une page affiche "service indisponible")
start "" http://localhost:5173
echo.
echo Autres consoles : Eureka http://localhost:8761  -  RabbitMQ http://localhost:15672 (gecimmo/gecimmo)  -  Keycloak http://localhost:8180 (admin/admin)
echo Pour tout arreter : arreter.bat
pause
