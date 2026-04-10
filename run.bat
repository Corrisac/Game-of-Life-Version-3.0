@echo off
echo ============================
echo  Game of Life on GPU
echo ============================
echo.

REM --- Auto-detect Java (uses system JAVA_HOME or PATH) ---
where javac >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ERROR: Java not found! Make sure JDK 17+ is installed and on your PATH.
    echo Download from: https://adoptium.net/
    pause
    exit /b 1
)

REM --- Compile ---
echo Compiling...
javac --release 17 -encoding UTF-8 -cp "dist\lib\core-4.5.2.jar;dist\lib\jogl-all-2.6.0.jar;dist\lib\gluegen-rt-2.6.0.jar" -d build\classes src\game\of\life\on\gpu\*.java
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
jar cfm dist\Game_Of_Life_On_GPU.jar dist\MANIFEST.MF -C build\classes .
del dist\MANIFEST.MF

REM --- Launch ---
echo Launching...
java ^
    --enable-native-access=ALL-UNNAMED ^
    --add-exports=java.desktop/sun.awt=ALL-UNNAMED ^
    --add-exports=java.desktop/sun.java2d=ALL-UNNAMED ^
    --add-opens=java.desktop/sun.awt=ALL-UNNAMED ^
    --add-opens=java.desktop/sun.awt.windows=ALL-UNNAMED ^
    --add-opens=java.desktop/sun.java2d=ALL-UNNAMED ^
    --add-opens=java.base/java.lang=ALL-UNNAMED ^
    -jar dist\Game_Of_Life_On_GPU.jar
