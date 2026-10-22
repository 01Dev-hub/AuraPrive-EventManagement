@echo off
cd /d "%~dp0"
javac -cp "lib/mysql-connector-j-8.3.0.jar;src/main/java" src/main/java/com/auraprive/exception/*.java src/main/java/com/auraprive/model/*.java src/main/java/com/auraprive/service/*.java src/main/java/com/auraprive/dao/*.java src/main/java/com/auraprive/SystemVerification.java
java -cp "lib/mysql-connector-j-8.3.0.jar;src/main/java" com.auraprive.SystemVerification
pause