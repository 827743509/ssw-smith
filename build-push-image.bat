@echo off
setlocal

set HTTP_PROXY=http://127.0.0.1:9567
set HTTPS_PROXY=http://127.0.0.1:9567

set IMAGE_NAME=ssw-smith
set IMAGE_TAG=latest
set DOCKERHUB_IMAGE=zhou827743509/ssw-smith:%IMAGE_TAG%

@REM docker builder prune -f
@REM docker image prune -f

echo Build image: %IMAGE_NAME%:%IMAGE_TAG%
docker build ^
  --build-arg HTTP_PROXY=http://host.docker.internal:9567 ^
  --build-arg HTTPS_PROXY=http://host.docker.internal:9567 ^
  --build-arg "MAVEN_OPTS=-Dhttp.proxyHost=host.docker.internal -Dhttp.proxyPort=9567 -Dhttps.proxyHost=host.docker.internal -Dhttps.proxyPort=9567" ^
  -t %IMAGE_NAME%:%IMAGE_TAG% .

if errorlevel 1 (
  echo Build image failed.
  exit /b 1
)

echo Tag image: %DOCKERHUB_IMAGE%
docker tag %IMAGE_NAME%:%IMAGE_TAG% %DOCKERHUB_IMAGE%

if errorlevel 1 (
  echo Tag image failed.
  exit /b 1
)

echo Login Docker Hub.
docker login

if errorlevel 1 (
  echo Docker Hub login failed.
  exit /b 1
)

echo Push image: %DOCKERHUB_IMAGE%
docker push %DOCKERHUB_IMAGE%

if errorlevel 1 (
  echo Push image failed.
  exit /b 1
)

echo Push image done: %DOCKERHUB_IMAGE%
endlocal
