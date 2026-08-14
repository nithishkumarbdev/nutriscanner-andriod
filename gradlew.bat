@rem Gradle startup script for Windows. See gradlew for notes on the wrapper jar.
@if "%DEBUG%"=="" @echo off
set DIRNAME=%~dp0
set APP_HOME=%DIRNAME%
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

if not exist "%CLASSPATH%" (
    echo gradle-wrapper.jar not found at %CLASSPATH%
    echo Run: gradle wrapper --gradle-version 8.7 (with a local Gradle install) to generate it.
    exit /b 1
)

if defined JAVA_HOME (
    set JAVA_EXE=%JAVA_HOME%\bin\java.exe
) else (
    set JAVA_EXE=java.exe
)

"%JAVA_EXE%" -Xmx64m -Xms64m -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
