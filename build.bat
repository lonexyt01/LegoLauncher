@echo off
cd /d "%~dp0"
echo === LegoLauncher APK (Windows) ===
echo Kerak: Git for Windows, JDK 17, Android SDK (ANDROID_HOME belgilangan)
where git >nul 2>&1 || (echo XATO: Git yo'q & pause & exit /b 1)
where java >nul 2>&1 || (echo XATO: JDK 17 yo'q & pause & exit /b 1)
if not defined ANDROID_HOME (echo XATO: ANDROID_HOME belgilanmagan & pause & exit /b 1)
if not exist upstream git clone --depth 1 -b v3_openjdk --recurse-submodules https://github.com/PojavLauncherTeam/PojavLauncher upstream
for /f "delims=" %%G in ('where git') do set "GITEXE=%%G"
set "GB=%GITEXE:\cmd\git.exe=\bin\bash.exe%"
"%GB%" rebrand.sh upstream
set "P=%ANDROID_HOME:\=/%"
echo sdk.dir=%P%> upstream\local.properties
cd upstream
call gradlew.bat assembleDebug --no-daemon
cd ..
for /r upstream %%F in (*-debug.apk) do copy /y "%%F" LegoLauncher.apk >nul
if exist LegoLauncher.apk (echo TAYYOR: LegoLauncher.apk) else (echo XATO - yuqoridagi matnni o'qing)
pause
