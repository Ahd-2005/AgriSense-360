@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements. See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership. The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License. You may obtain a copy of the License at
@REM
@REM https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied. See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM
@REM Required ENV vars:
@REM JAVA_HOME - location of a JDK home dir
@REM ----------------------------------------------------------------------------

@echo off
title %0

if "%HOME%" == "" (set "HOME=%HOMEDRIVE%%HOMEPATH%")

set MAVEN_PROJECTBASEDIR=%~dp0
IF NOT "%MAVEN_BASEDIR%"=="" set MAVEN_PROJECTBASEDIR=%MAVEN_BASEDIR%

set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

IF NOT EXIST %WRAPPER_JAR% (
  echo Downloading maven-wrapper.jar...
  set DOWNLOAD_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar
  powershell -Command "&{[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object Net.WebClient).DownloadFile('%DOWNLOAD_URL%', '%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar')}"
)

set MAVEN_JAVA_EXE="%JAVA_HOME%\bin\java.exe"
"%JAVA_HOME%\bin\java.exe" ^
  -classpath %WRAPPER_JAR% ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  %WRAPPER_LAUNCHER% %*
if ERRORLEVEL 1 goto error
goto end
:error
set ERROR_CODE=1
:end
exit /B %ERROR_CODE%
