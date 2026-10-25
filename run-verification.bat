@echo off
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out -cp "lib/mysql-connector-j-8.3.0.jar;src/main/java" src/main/java/com/auraprive/exception/*.java src/main/java/com/auraprive/model/*.java src/main/java/com/auraprive/service/*.java src/main/java/com/auraprive/dao/*.java src/main/java/com/auraprive/SystemVerification.java
if errorlevel 1 (
    echo [BUILD FAILED] Compilation error detected.
    pause
    exit /b 1
)
java -cp "lib/mysql-connector-j-8.3.0.jar;out" com.auraprive.SystemVerification
pause