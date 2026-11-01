@echo off
setlocal
rem Runs the JVM unit tests (URL handling, remote-control mapping).
call "%~dp0_env.bat" || exit /b 1
call "%~dp0_gradle.bat" :app:testDebugUnitTest || (
  echo [OmoWeb] Ada tes yang GAGAL. Laporan: app\build\reports\tests\
  exit /b 1
)
echo [OmoWeb] Semua tes lolos.
exit /b 0
