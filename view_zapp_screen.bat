@echo off
title ZorApp - 2016 Cadillac CTS Screen Mirror
set SCRCPY_EXE="%LOCALAPPDATA%\Microsoft\WinGet\Packages\Genymobile.scrcpy_Microsoft.Winget.Source_8wekyb3d8bbwe\scrcpy-win64-v4.1\scrcpy.exe"
echo Opening live ZorApp window...
%SCRCPY_EXE% --window-title "ZorApp - 2016 Cadillac CTS (800x480)"
pause
