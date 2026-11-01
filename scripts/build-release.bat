@echo off
setlocal
rem Build a release APK (minified). Signed with keystore.properties if present,
rem otherwise with the debug key (fine for sideloading, not for Google Play).
call "%~dp0_env.bat" || exit /b 1
if not exist "%OMO_ROOT%\keystore.properties" echo [OmoWeb] PERINGATAN: keystore.properties belum ada - APK ditandatangani kunci debug. Jalankan scripts\signing-setup.bat untuk kunci rilis.
call "%~dp0_gradle.bat" :app:assembleRelease || goto :fail
if not exist "%OMO_ROOT%\dist" mkdir "%OMO_ROOT%\dist"
copy /y "%OMO_ROOT%\app\build\outputs\apk\release\app-release.apk" "%OMO_ROOT%\dist\OmoWeb-%OMO_VERSION%-release.apk" >nul
echo.
echo [OmoWeb] Selesai: dist\OmoWeb-%OMO_VERSION%-release.apk
exit /b 0
:fail
echo [OmoWeb] Build release GAGAL.
exit /b 1
