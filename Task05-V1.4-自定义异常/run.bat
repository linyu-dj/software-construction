@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out src\campus\SystemMain.java src\campus\entity\User.java src\campus\utils\FileUtil.java src\campus\exception\BusinessException.java src\campus\exception\UserExistsException.java src\campus\exception\UserNotFoundException.java src\campus\exception\WrongPasswordException.java src\campus\exception\EmptyParamException.java
if errorlevel 1 (
    echo Build failed.
    pause
    exit /b 1
)
java -Dfile.encoding=UTF-8 -cp out campus.SystemMain
pause