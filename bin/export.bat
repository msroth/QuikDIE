@echo off
cls
echo.
echo.

REM ==============================
REM CHANGE THESE PATHS AS NECESSARY

set CONFIG_DIR=c:\documentum\config
set DFC_JAR=c:\program files\documentum\dctm.jar

REM ===============================

set DFC_PROPERTIES=%CONFIG_DIR%/dfc.properties

REM make sure Export properties file exists
if exist export.properties goto check_config_dir
echo Cannot find export.properties file in this directory.  Export may not run properly.

REM check that the dfc.properties file can be found
:check_config_dir
if exist %DFC_PROPERTIES% goto run_export
echo Cannot find dfc.properties file in %CONFIG_DIR%.  Please change the value of the CONFIG_DIR variable in this batch file and try Export again.
goto end

REM run Export
:run_export
java -classpath "%CLASSPATH%;%CONFIG_DIR%;Export.jar;lib/dmRecordSet.jar;lib/DCTMBasics.jar;lib/jdom-2.0.6.jar;%DFC_JAR%" com.dm_misc.QuikDIE.Export

:end
echo.
echo.
pause
