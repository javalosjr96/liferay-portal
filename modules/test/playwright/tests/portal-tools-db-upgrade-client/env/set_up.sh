#!/bin/bash

CURRENT_DIR_NAME=$(dirname ${BASH_SOURCE[0]})

echo CURRENT_DIR_NAME=${CURRENT_DIR_NAME}

source ${CURRENT_DIR_NAME}/../../../env/common.sh

function main {
	database_upgrade_client_set_up
}

function database_upgrade_client_set_up {
	cd ${LIFERAY_HOME}/tools/portal-tools-db-upgrade-client/

	/bin/bash

}


main "${@}"