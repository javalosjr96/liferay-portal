#!/bin/bash
CURRENT_DIR_NAME=$(dirname ${BASH_SOURCE[0]})
function update_db_client_ext_properties {

file_location="${LIFERAY_HOME}/tools/portal-tools-db-upgrade-client/app-server.properties"

app_server_properties=(
"dir=${LIFERAY_HOME}/${APP_SERVER_TYPE}-9.0.87"
"extra.lib.dirs=bin"
"global.lib.dir=lib"
"portal.dir=webapps/ROOT"
"server.detector.server.id=${APP_SERVER_TYPE}"
)
> "$file_location"
for prop in "${app_server_properties[@]}"; do
  printf "%s\n" "$prop" >> "$file_location"
done

echo "Properties file created at: $file_location"

file_location="${LIFERAY_HOME}/tools/portal-tools-db-upgrade-client/portal-upgrade-database.properties"

portal_upgrade_database_properties=(
jdbc.default.driverClassName=${DATABASE_DRIVER}
jdbc.default.url=${DATABASE_URL}
jdbc.default.username=${DATABASE_USERNAME}
jdbc.default.password=${DATABASE_PASSWORD}
)
> "$file_location"
for prop in "${portal_upgrade_database_properties[@]}"; do
  printf "%s\n" "$prop" >> "$file_location"
done

echo "Properties file created at: $file_location"

file_location="${LIFERAY_HOME}/tools/portal-tools-db-upgrade-client/portal-upgrade-ext.properties"

portal_upgrade_ext_properties=(
liferay.home=${LIFERAY_HOME}
)
> "$file_location"
for prop in "${portal_upgrade_ext_properties[@]}"; do
  printf "%s\n" "$prop" >> "$file_location"
done

echo "Properties file created at: $file_location"

cd "${LIFERAY_HOME}/tools/portal-tools-db-upgrade-client/"

./db_upgrade_client.sh -j "-Dfile.encoding=UTF8 -Duser.country=US -Duser.language=en -Duser.timezone=GMT -Xmx4096m" &

sleep 5

db_upgrader_pid=$(ps aux | grep DBUpgrader | grep -v grep | awk '{print $2}')

jinfo_output=$(jinfo $db_upgrader_pid)

echo ${jinfo_output}

}

