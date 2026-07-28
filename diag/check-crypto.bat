@echo off
echo === FIPS policy registry ===
C:\Windows\System32\reg.exe query "HKLM\System\CurrentControlSet\Control\Lsa\FipsAlgorithmPolicy" /v Enabled
echo.
echo === CSP list (certutil -csplist) ===
C:\Windows\System32\certutil.exe -csplist
echo.
echo === .NET Framework versions installed ===
C:\Windows\System32\reg.exe query "HKLM\SOFTWARE\Microsoft\NET Framework Setup\NDP" /s /v Version 2>nul | C:\Windows\System32\findstr.exe /i version
echo.
echo === PowerShell PSVersionTable attempt ===
C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe -NoProfile -Command "$PSVersionTable" 2>&1
echo.
echo === PowerShell 7 (pwsh) presence check ===
C:\Windows\System32\where.exe pwsh
