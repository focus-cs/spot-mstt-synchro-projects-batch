@echo off
setlocal

chcp 65001
cls

rem Get arguments
cd ..
set BATCH_PATH=%cd%
set BATCH_MAIN=com.schneider.api.cost_codes.Runner


rem Set folder path
set ROOT_DIR=%BATCH_PATH%
set LIB_DIR=%ROOT_DIR%/lib
set CONF_DIR=%ROOT_DIR%/conf
set INPUT_DIR=%ROOT_DIR%/requests

cd %INPUT_DIR%
set /a NB_FILE=0
for /f %%a in ('dir %INPUT_DIR=% /A-D /B /S ') do set /a NB_FILE+=1
if %NB_FILE% == 0 GOTO end
echo Directory %INPUT_DIR% contains %NB_FILE% files to process ...

rem Open root directory
cd %ROOT_DIR%

rem Set Java arguments with log4j configuration file
set JAVA_ARGS=-showversion
set JAVA_ARGS=%JAVA_ARGS% -Djava.awt.headless=true
set JAVA_ARGS=%JAVA_ARGS% -Dlog4j.overwrite=true
set JAVA_ARGS=%JAVA_ARGS% -Xmx6000m
set JAVA_ARGS=%JAVA_ARGS% -Dfile.encoding=UTF-8
set JAVA_ARGS=%JAVA_ARGS% -Duse_description=true
set JAVA_ARGS=%JAVA_ARGS% -Dlog4j.configuration=%CONF_DIR%/log4j.properties
set JAVA_ARGS=%JAVA_ARGS% -cp "%LIB_DIR%/*"

rem Launch the API with 2 arguments: psconnect.properties and log4j.properties files paths.
java %JAVA_ARGS% %BATCH_MAIN% %CONF_DIR%/psconnect.properties %CONF_DIR%/log4j.properties

:end
pause