@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist out mkdir out

rem 生成参数文件时去掉绝对路径前缀，只留 src\... 这样的相对路径。
rem 原因：javac 读 @参数文件用的是系统默认编码，中文目录名会被解成乱码后报"找不到文件"。
setlocal enabledelayedexpansion
set "PREFIX=%CD%\"
break > sources.txt
for /f "delims=" %%i in ('dir /s /b src\*.java') do (
    set "line=%%i"
    set "line=!line:%PREFIX%=!"
    echo !line!>> sources.txt
)

javac -encoding UTF-8 -d out @sources.txt
if errorlevel 1 (
    echo Build failed.
    pause
    exit /b 1
)

java -Dfile.encoding=UTF-8 -cp out campus.SystemMain
pause