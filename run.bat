@echo off
echo ============================
echo  Game of Life on GPU
echo ============================
echo.

set JAVA_HOME=C:\Program Files\Apache NetBeans\jdk
set PATH=%JAVA_HOME%\bin;%PATH%

REM --- Compile ---
echo Compiling...
"%JAVA_HOME%\bin\javac.exe" --release 17 -cp "dist\lib\core-4.5.2.jar;dist\lib\jogl-all-2.6.0.jar;dist\lib\gluegen-rt-2.6.0.jar" -d build\classes src\game\of\life\on\gpu\*.java
if %ERRORLEVEL% neq 0 (
    echo COMPILE FAILED!
    pause
    exit /b 1
)

REM --- Sync data files (shaders) to dist ---
echo Syncing data files...
xcopy /Y /Q "data\*.*" "dist\data\" >nul 2>&1

REM --- Build JAR ---
echo Building JAR...
(
echo Manifest-Version: 1.0
echo Class-Path: lib/core-4.5.2.jar lib/gluegen-rt-2.6.0.jar lib/gluegen-rt-2.6.0-natives-windows-amd64.jar lib/gluegen-rt-main-2.6.0.jar lib/jogl-all-2.6.0.jar lib/jogl-all-2.6.0-natives-windows-amd64.jar lib/jogl-all-main-2.6.0.jar
echo Main-Class: Main
echo.
) > dist\MANIFEST.MF
"%JAVA_HOME%\bin\jar.exe" cfm dist\Game_Of_Life_On_GPU.jar dist\MANIFEST.MF -C build\classes .
del dist\MANIFEST.MF

REM --- Launch ---
echo Launching...
"%JAVA_HOME%\bin\java.exe" ^
    --enable-native-access=ALL-UNNAMED ^
    --add-exports=java.desktop/sun.awt=ALL-UNNAMED ^
    --add-exports=java.desktop/sun.java2d=ALL-UNNAMED ^
    --add-opens=java.desktop/sun.awt=ALL-UNNAMED ^
    --add-opens=java.desktop/sun.awt.windows=ALL-UNNAMED ^
    --add-opens=java.desktop/sun.java2d=ALL-UNNAMED ^
    --add-opens=java.base/java.lang=ALL-UNNAMED ^
    -jar dist\Game_Of_Life_On_GPU.jar
