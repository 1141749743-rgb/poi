@echo off
setlocal
cd /d "%~dp0"

rem 如果本地有密钥文件就先读进来（这个文件不进 git）
if exist "keys.local.bat" call "keys.local.bat"

rem 万一 mvn 不在 PATH 里，手动补上
where mvn >nul 2>nul
if errorlevel 1 set "PATH=%PATH%;D:\maven\apache-maven-3.9.9\bin"

echo ================================================
echo   校园生活圈 POI 平台 - Java 版
echo ================================================
echo.
echo [1/3] 检查 Java
java -version
echo.
echo [2/3] 检查 Maven
mvn -v
echo.
echo [3/3] 启动服务，等看到 "Tomcat started on port 8080"
echo        就用浏览器打开 http://127.0.0.1:8080
echo        停止服务：在本窗口按 Ctrl+C
echo.
mvn spring-boot:run
echo.
echo 服务已停止。
pause
