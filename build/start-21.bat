@echo off
setlocal enableextensions

set "BASE_DIR=%~dp0"
set "MAIN_CLASS=me.n1ar4.jar.analyzer.starter.Application"
set "JAVA_EXE=%BASE_DIR%jre\bin\java.exe"
set "CORE_JAR="

if not exist "%JAVA_EXE%" (
    echo [-] bundled runtime not found: %JAVA_EXE%
    exit /b 1
)

"%JAVA_EXE%" --list-modules | findstr /b /c:"jcef@" >nul
if errorlevel 1 (
    "%JAVA_EXE%" --list-modules | findstr /x /c:"jcef" >nul
    if errorlevel 1 (
        echo [-] bundled runtime missing jcef module ^(JBR + JCEF required^)
        exit /b 1
    )
)

for %%f in ("%BASE_DIR%lib\jar-analyzer-*.jar") do (
    set "CORE_JAR=%%~f"
    goto :core_found
)

echo [-] core jar not found: %BASE_DIR%lib\jar-analyzer-*.jar
exit /b 1

:core_found
set "JAVA_ARGS=-XX:+UseZGC -Xms2g -Dfile.encoding=UTF-8"
set "EXTRA_JAVA_ARGS=%JA_JVM_OPTS%"

echo [*] JAVA: %JAVA_EXE%
echo [*] CORE JAR: %CORE_JAR%
"%JAVA_EXE%" %JAVA_ARGS% %EXTRA_JAVA_ARGS% -cp "%CORE_JAR%" %MAIN_CLASS% %*
