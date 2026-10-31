@echo off
@rem Runs the @SKILL@ CLI @VERSION@ from the sources in ..\cli with the bundled
@rem Kotlin Toolchain wrapper. No Java installation is required: a Java @MIN_JAVA@+
@rem runtime that is already installed is reused, otherwise the wrapper provisions one.
setlocal EnableExtensions DisableDelayedExpansion

set "cli_dir=%~dp0..\cli"
set "min_java=@MIN_JAVA@"

if defined KOTLIN_CLI_JAVA_HOME goto run
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" call :probe "%JAVA_HOME%\bin\java.exe"
if defined KOTLIN_CLI_JAVA_HOME goto run
for /f "delims=" %%j in ('where java.exe 2^>nul') do (
  call :probe "%%j"
  if defined KOTLIN_CLI_JAVA_HOME goto run
)

:run
if not defined BOOTSTRAP_BUILD_DIR set "BOOTSTRAP_BUILD_DIR=%LOCALAPPDATA%\bootstrap-projects\@VERSION@\@SKILL@"
if not exist "%BOOTSTRAP_BUILD_DIR%" mkdir "%BOOTSTRAP_BUILD_DIR%"
set "KOTLIN_CLI_NO_WELCOME_BANNER=1"
@rem Compile first with Toolchain output on stderr, so stdout carries only CLI output.
call "%cli_dir%\kotlin.bat" --log-level=error build "--project-dir=%cli_dir%" "--build-dir=%BOOTSTRAP_BUILD_DIR%" 1>&2
if errorlevel 1 exit /b %ERRORLEVEL%
call "%cli_dir%\kotlin.bat" --log-level=error run "--project-dir=%cli_dir%" "--build-dir=%BOOTSTRAP_BUILD_DIR%" "--working-dir=%CD%" -- %*
exit /b %ERRORLEVEL%

@rem Sets KOTLIN_CLI_JAVA_HOME when %1 is a functional runtime that meets the Toolchain CLI minimum.
:probe
set "probe_version="
set "probe_home="
for /f "tokens=1,* delims==" %%a in ('"%~1" -XshowSettings:properties -version 2^>^&1') do (
  for /f "tokens=*" %%k in ("%%a") do (
    if "%%k"=="java.specification.version " for /f "tokens=*" %%v in ("%%b") do set "probe_version=%%v"
    if "%%k"=="java.home " for /f "tokens=*" %%v in ("%%b") do set "probe_home=%%v"
  )
)
if not defined probe_version exit /b 0
if not defined probe_home exit /b 0
for /f "tokens=1 delims=." %%m in ("%probe_version%") do set "probe_major=%%m"
set /a probe_major=probe_major 2>nul
if %probe_major% GEQ %min_java% if exist "%probe_home%\bin\java.exe" set "KOTLIN_CLI_JAVA_HOME=%probe_home%"
exit /b 0
