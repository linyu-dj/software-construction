@echo off
chcp 65001 >nul
cd /d "%~dp0"

rem 单元测试跑在独立临时目录，避免写坏正式的 user.txt / resource.txt
if not exist testtmp mkdir testtmp
if exist testtmp\user.txt del /q testtmp\user.txt
if exist testtmp\resource.txt del /q testtmp\resource.txt

rem 参数文件只写相对路径：javac 按系统默认编码读 @文件，绝对路径里的中文会变乱码
setlocal enabledelayedexpansion
set "PREFIX=%CD%\"
break > sources.txt
for /f "delims=" %%i in ('dir /s /b src\*.java test\*.java') do (
    set "line=%%i"
    set "line=!line:%PREFIX%=!"
    echo !line!>> sources.txt
)

javac -encoding UTF-8 -d out @sources.txt
if errorlevel 1 (
    echo [ERROR] 编译失败，请先看上面的报错
    del /q sources.txt
    pause
    exit /b 1
)

pushd testtmp
java -Dfile.encoding=UTF-8 -cp ..\out campus.test.TestRunner
set TEST_RESULT=%ERRORLEVEL%
popd

del /q sources.txt
echo.
if "%TEST_RESULT%"=="0" (
    echo ALL UNIT TESTS PASSED
) else (
    echo SOME UNIT TESTS FAILED
)
pause