@echo off

pushd "%~dp0"

path %PATH%;%JAVA_HOME%\bin

java --add-opens java.base/java.lang=ALL-UNNAMED -jar com.liferay.portal.tools.db.upgrade.client.jar %*

popd

@echo on