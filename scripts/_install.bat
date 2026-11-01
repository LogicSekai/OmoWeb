@echo off
rem Installs %OMO_APK% and launches the app. Args: adb device selector (-d  or  -s ip:port)
set "SEL=%*"
echo [OmoWeb] Memasang %OMO_PKG% ...
"%ADB%" %SEL% install -r "%OMO_APK%" > "%TEMP%\omoweb_install.txt" 2>&1
type "%TEMP%\omoweb_install.txt"
findstr /c:"Success" "%TEMP%\omoweb_install.txt" >nul && goto :launch
findstr /c:"INSTALL_FAILED_UPDATE_INCOMPATIBLE" /c:"signatures do not match" "%TEMP%\omoweb_install.txt" >nul || goto :failed
echo.
echo [OmoWeb] Versi di HP ditandatangani kunci lain. Uninstall dulu? (data aplikasi akan terhapus)
choice /c YN /n /m "Uninstall lalu pasang ulang [Y/N]? "
if errorlevel 2 goto :failed
"%ADB%" %SEL% uninstall %OMO_PKG%
"%ADB%" %SEL% install -r "%OMO_APK%" || goto :failed

:launch
"%ADB%" %SEL% shell am start -n %OMO_PKG%/com.logicsekai.omoweb.MainActivity >nul
echo [OmoWeb] Terpasang dan dijalankan.
exit /b 0
:failed
echo [OmoWeb] Pemasangan GAGAL.
exit /b 1
