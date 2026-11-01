@echo off
setlocal
rem Build a debug APK (package com.logicsekai.omoweb.debug) into dist\
call "%~dp0_env.bat" || exit /b 1
call "%~dp0_gradle.bat" :app:assembleDebug || goto :fail
if not exist "%OMO_ROOT%\dist" mkdir "%OMO_ROOT%\dist"
copy /y "%OMO_ROOT%\app\build\outputs\apk\debug\app-debug.apk" "%OMO_ROOT%\dist\OmoWeb-%OMO_VERSION%-debug.apk" >nul
echo.
echo [OmoWeb] Selesai: dist\OmoWeb-%OMO_VERSION%-debug.apk
exit /b 0
:fail
echo [OmoWeb] Build debug GAGAL.
exit /b 1
