@rem
@rem Gradle startup script for Windows.
@rem
@rem NOTE: gradle/wrapper/gradle-wrapper.jar is not included in this generated project
@rem (binary jars cannot be produced by this build assistant). Run
@rem   gradle wrapper --gradle-version 8.7
@rem with a local Gradle install to generate it, or use `gradle` directly as CI does.
@rem

@echo off
set DIRNAME=%~dp0
set APP_HOME=%DIRNAME%

set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

if not exist "%CLASSPATH%" (
    echo ERROR: gradle\wrapper\gradle-wrapper.jar is missing.
    echo Run "gradle wrapper --gradle-version 8.7" to generate it, or use "gradle" directly.
    exit /b 1
)

if defined JAVA_HOME (
    set JAVA_EXE=%JAVA_HOME%\bin\java.exe
) else (
    set JAVA_EXE=java.exe
)

"%JAVA_EXE%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
