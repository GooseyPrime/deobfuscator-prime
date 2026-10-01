@echo off
REM ==============================================================================
REM Java Deobfuscator Launcher (Windows)
REM ==============================================================================
setlocal EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
set "JAR_FILE=%SCRIPT_DIR%target\deobfuscator-1.0.0.jar"

where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] 'java' command not found in PATH.
    echo Please install Java 8 or higher (JDK 17 or 21 recommended) and add it to your PATH.
    pause
    exit /b 1
)

if not exist "%JAR_FILE%" (
    echo [INFO] Shaded JAR not found. Building project with Maven...
    cd /d "%SCRIPT_DIR%"
    call mvn clean package -DskipTests
    if errorlevel 1 (
        set "BUILD_ERROR=!ERRORLEVEL!"
        echo [ERROR] Maven build failed.
        pause
        exit /b !BUILD_ERROR!
    )
)

REM -Xss128m prevents StackOverflowError during complex control flow deobfuscation
REM -Xmx2G provides 2GB heap memory
set JVM_OPTS=-Xss128m -Xmx2G

echo [INFO] Starting Java Deobfuscator...
java %JVM_OPTS% -jar "%JAR_FILE%" %*
endlocal
