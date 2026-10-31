@echo off
@rem Runs the @SKILL@ CLI @VERSION@ from the sources in ..\cli with the bundled
@rem Kotlin Toolchain wrapper. No Java installation is required: a Java @MIN_JAVA@+
@rem runtime that is already installed is reused, otherwise the wrapper provisions one.
setlocal EnableExtensions DisableDelayedExpansion

for %%i in ("%~dp0..\cli") do set "cli_dir=%%~fi"
set "min_java=@MIN_JAVA@"

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
set "build_dir=%BOOTSTRAP_CACHE_DIR%\@VERSION@\@SKILL@-%install_key%"
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

@rem Sets install_key to a short hash of cli_dir. Concurrent launchers start with the
@rem same %RANDOM% sequence, so the scratch directory is claimed with an atomic mkdir.
:install_key
set "install_key="
set /a key_tries=0
:install_key_dir
set /a key_tries+=1
if %key_tries% GTR 50 set "install_key=unkeyed" & exit /b 0
set "key_dir=%TEMP%\bootstrap-key-%RANDOM%-%RANDOM%"
mkdir "%key_dir%" 2>nul || goto install_key_dir
<nul set /p "=%cli_dir%" > "%key_dir%\path.txt"
for /f "skip=1 delims=" %%h in ('certutil -hashfile "%key_dir%\path.txt" MD5') do if not defined install_key set "install_key=%%h"
rmdir /s /q "%key_dir%" >nul 2>&1
if not defined install_key set "install_key=unkeyed"
set "install_key=%install_key: =%"
echo(%install_key%| findstr /r /x "[0-9a-fA-F]*" >nul || set "install_key=unkeyed"
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
