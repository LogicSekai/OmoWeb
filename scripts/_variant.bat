@echo off
rem Maps a variant name to Gradle task, APK path and package. Sets OMO_TASK, OMO_APK, OMO_PKG.
if /i "%~1"=="release" (
  set "OMO_TASK=:app:assembleRelease"
  set "OMO_APK=%OMO_ROOT%\app\build\outputs\apk\release\app-release.apk"
  set "OMO_PKG=com.logicsekai.omoweb"
  exit /b 0
)
if /i "%~1"=="debug" (
  set "OMO_TASK=:app:assembleDebug"
  set "OMO_APK=%OMO_ROOT%\app\build\outputs\apk\debug\app-debug.apk"
  set "OMO_PKG=com.logicsekai.omoweb.debug"
  exit /b 0
)
echo [OmoWeb] Varian tidak dikenal: %~1 (pakai debug atau release)
exit /b 1
