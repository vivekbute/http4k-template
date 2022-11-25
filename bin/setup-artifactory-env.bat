@echo off

WHERE vault
IF %ERRORLEVEL% NEQ 0 GOTO :errornovault

ECHO Reading Artifactory credentials from Vault and setting environment variables
FOR /f %%i in ('vault kv get -field=username springernature/shared/artifactory') do (
  set ARTIFACTORY_USERNAME=%%i
  setx ARTIFACTORY_USERNAME %%i)

FOR /f %%i in ('vault kv get -field=password springernature/shared/artifactory') do (
  set ARTIFACTORY_PASSWORD=%%i
  setx ARTIFACTORY_PASSWORD %%i)

EXIT /B

:errornovault
ECHO vault programm was not found
EXIT /B 1