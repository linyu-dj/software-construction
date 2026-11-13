@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
if errorlevel 1 (
    echo Build failed.
    pause
    exit /b 1
)
java -Dfile.encoding=UTF-8 -cp out campus.SystemMain
pause