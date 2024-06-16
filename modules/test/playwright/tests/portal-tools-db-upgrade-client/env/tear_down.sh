#!/bin/bash

CURRENT_DIR_NAME=$(dirname ${BASH_SOURCE[0]})

echo CURRENT_DIR_NAME=${CURRENT_DIR_NAME}

source ${CURRENT_DIR_NAME}/../../../env/common.sh

function main {
	tear_down_upgrade_client
}

function tear_down_upgrade_client {
cd test/playwright/tests/portal-tools-db-upgrade-client/ant || exit

ant -f build-test-db-upgrade-client.xml clean-database-upgrade-client
}

main "${@}"