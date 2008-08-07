@ECHO OFF
echo Starting YajHFC...

SetLocal EnableDelayedExpansion

%~d0
cd %~p0

if not exist yajhfc.jar (
	echo yajhfc.jar not found.
	exit /B 2
)

if not exist FOPPlugin.jar (
	echo FOPPlugin.jar not found.
	exit /B 2
)

if "%CLASSPATH%"=="" set CLASSPATH=

for %%J in (lib\*.jar) do set CLASSPATH=!CLASSPATH!;%%J

set CLASSPATH=%CLASSPATH%;yajhfc.jar

start javaw yajhfc.Launcher --loadplugin=FOPPlugin.jar %*

EndLocal
