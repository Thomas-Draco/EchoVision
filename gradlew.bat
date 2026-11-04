@echo off
setlocal EnableExtensions
set "ROOT=%~dp0"
set "WRAPPER=%ROOT%gradle\wrapper\gradle-wrapper.jar"
set "WRAPPER_URL=https://raw.githubusercontent.com/gradle/gradle/v8.10.2/gradle/wrapper/gradle-wrapper.jar"
set "EXPECTED_SHA256=2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046"

rem Le fichier JAR du wrapper n'est PAS disponible sous services.gradle.org/distributions/gradle-8.10.2-wrapper.jar.
rem Il est recupere depuis le depot officiel Gradle et compare au SHA-256 publie par Gradle.
powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -Command ^
 "$ErrorActionPreference='Stop';" ^
 "$path=$env:WRAPPER; $url=$env:WRAPPER_URL; $expected=$env:EXPECTED_SHA256;" ^
 "if (Test-Path -LiteralPath $path) { $actual=(Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant(); if ($actual -ne $expected) { Write-Host 'Wrapper JAR existant invalide: remplacement.'; Remove-Item -LiteralPath $path -Force; } };" ^
 "if (-not (Test-Path -LiteralPath $path)) { Write-Host 'Installation du wrapper Gradle 8.10.2...'; $tmp=$path+'.download'; try { Invoke-WebRequest -Uri $url -UseBasicParsing -UserAgent 'agent-local' -OutFile $tmp; $actual=(Get-FileHash -LiteralPath $tmp -Algorithm SHA256).Hash.ToLowerInvariant(); if ($actual -ne $expected) { throw ('SHA-256 inattendu pour gradle-wrapper.jar: ' + $actual) }; Move-Item -LiteralPath $tmp -Destination $path -Force; Write-Host 'Wrapper Gradle verifie.' } finally { if (Test-Path -LiteralPath $tmp) { Remove-Item -LiteralPath $tmp -Force } } }"
if errorlevel 1 (
    echo ERREUR: installation ou validation du wrapper Gradle impossible.
    exit /b 1
)

if defined JAVA_HOME (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA_EXE=java.exe"
)
"%JAVA_EXE%" -cp "%WRAPPER%" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
