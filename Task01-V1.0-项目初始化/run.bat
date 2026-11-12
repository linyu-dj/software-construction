@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out src\campus\SystemMain.java
if errorlevel 1 (
    echo Build failed.
    pause
    exit /b 1
)
java -Dfile.encoding=UTF-8 -cp out campus.SystemMain
pause
