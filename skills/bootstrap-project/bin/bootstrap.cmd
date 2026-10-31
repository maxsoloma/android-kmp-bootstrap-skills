@echo off
@rem Runs the bootstrap-project CLI 3.4.0 from the sources in ..\cli with the bundled
@rem Kotlin Toolchain wrapper. No Java installation is required: a Java 25+
@rem runtime that is already installed is reused, otherwise the wrapper provisions one.
setlocal EnableExtensions DisableDelayedExpansion

for %%i in ("%~dp0..\cli") do set "cli_dir=%%~fi"
set "min_java=25"

if defined KOTLIN_CLI_JAVA_HOME goto run
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" call :probe "%JAVA_HOME%\bin\java.exe"
if defined KOTLIN_CLI_JAVA_HOME goto run
for /f "delims=" %%j in ('where java.exe 2^>nul') do (
  call :probe "%%j"
  if defined KOTLIN_CLI_JAVA_HOME goto run
)

:run
@rem Each installation gets its own build directory: copies sharing one would recompile
@rem each other's output, and a recompiling `run` writes to stdout.
call :install_key
if not defined BOOTSTRAP_CACHE_DIR set "BOOTSTRAP_CACHE_DIR=%LOCALAPPDATA%\bootstrap-projects"
set "build_dir=%BOOTSTRAP_CACHE_DIR%\3.4.0\bootstrap-project-%install_key%"
if not exist "%build_dir%" mkdir "%build_dir%"
@rem A trailing backslash (drive root) would escape the closing quote of --working-dir.
set "work_dir=%CD%"
if "%work_dir:~-1%"=="\" set "work_dir=%work_dir%."
@rem Run the pinned wrapper version even when the caller's directory is inside another
@rem Kotlin Toolchain project, whose own wrapper would otherwise select the version.
set "KOTLIN_CLI_WRAPPER_ALWAYS_USE_INTRINSIC_VERSION=1"
set "KOTLIN_CLI_NO_WELCOME_BANNER=1"
@rem Compile first with Toolchain output on stderr, so stdout carries only CLI output.
call "%cli_dir%\kotlin.bat" --log-level=error build "--project-dir=%cli_dir%" "--build-dir=%build_dir%" 1>&2
if not "%ERRORLEVEL%"=="0" exit /b %ERRORLEVEL%
@rem No `call`: it would expand % and ^ in the forwarded arguments a second time.
"%cli_dir%\kotlin.bat" --log-level=error run "--project-dir=%cli_dir%" "--build-dir=%build_dir%" "--working-dir=%work_dir%" -- %*
@rem Reached only when kotlin.bat could not be started.
exit /b %ERRORLEVEL%

@rem Sets install_key to a short hash of cli_dir.
:install_key
set "install_key="
set "key_file=%TEMP%\bootstrap-key-%RANDOM%%RANDOM%.txt"
<nul set /p "=%cli_dir%" > "%key_file%"
for /f "skip=1 delims=" %%h in ('certutil -hashfile "%key_file%" MD5') do if not defined install_key set "install_key=%%h"
del "%key_file%" >nul 2>&1
set "install_key=%install_key: =%"
set "install_key=%install_key:~0,12%"
exit /b 0

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
