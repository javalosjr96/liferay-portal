#!/bin/bash

CURRENT_DIR_NAME=$(dirname "${BASH_SOURCE[0]}")

source "${CURRENT_DIR_NAME}/../../../../env/common.sh"

DATA_ARCHIVE_TYPE="data-archive-portal"
PORTAL_VERSION="6.2.5"

function main {
	set -ex

	cd "${_PORTAL_PROJECT_DIR}"

	echo "Upgrade source: portal.version=${PORTAL_VERSION} data.archive.type=${DATA_ARCHIVE_TYPE}"
	echo "Provisioned by: project=${PLAYWRIGHT_PROJECT_NAME} dir=${CURRENT_DIR_NAME}"

	ant -f build-test.xml \
		-Ddata.archive.type="${DATA_ARCHIVE_TYPE}" \
		-Dkeep.cached.app.server.data=true \
		-Dportal.version="${PORTAL_VERSION}" \
		-Dskip.get.testcase.database.properties=true \
		rebuild-legacy-database

	ant -f build-test.xml upgrade-legacy-database

	assert_clean_upgrade_log

	default_set_up
}

main "${@}"
