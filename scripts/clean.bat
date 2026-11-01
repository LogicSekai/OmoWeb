@echo off
setlocal
call "%~dp0_env.bat" || exit /b 1
call "%~dp0_gradle.bat" clean
if exist "%OMO_ROOT%\dist" rmdir /s /q "%OMO_ROOT%\dist"
echo [OmoWeb] Bersih.
exit /b 0
