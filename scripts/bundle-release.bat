@echo off
setlocal
rem Build the Android App Bundle (AAB) for the Google Play production track.
call "%~dp0_env.bat" || exit /b 1
call :require_keystore || exit /b 1
call "%~dp0_gradle.bat" :app:bundleRelease -Pomo.distribution=play || goto :fail
if not exist "%OMO_ROOT%\dist" mkdir "%OMO_ROOT%\dist"
copy /y "%OMO_ROOT%\app\build\outputs\bundle\release\app-release.aab" "%OMO_ROOT%\dist\OmoWeb-%OMO_VERSION%-%OMO_VERSION_CODE%-release.aab" >nul
echo.
echo [OmoWeb] Selesai: dist\OmoWeb-%OMO_VERSION%-%OMO_VERSION_CODE%-release.aab
echo           Upload ke Play Console - Produksi. Naikkan omo.versionCode di gradle.properties untuk upload berikutnya.
exit /b 0
:fail
echo [OmoWeb] Bundle release GAGAL.
exit /b 1

:require_keystore
if exist "%OMO_ROOT%\keystore.properties" exit /b 0
echo [OmoWeb] keystore.properties belum ada. AAB untuk Play harus ditandatangani kunci upload kamu.
echo           Jalankan scripts\signing-setup.bat terlebih dahulu.
exit /b 1
