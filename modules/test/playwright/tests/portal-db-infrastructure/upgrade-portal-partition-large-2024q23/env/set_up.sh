#!/bin/bash

CURRENT_DIR_NAME=$(dirname "${BASH_SOURCE[0]}")

echo "CURRENT_DIR_NAME=${CURRENT_DIR_NAME}"

source "${CURRENT_DIR_NAME}/../../../../env/common.sh"

CUSTOM_STARTUP_TIMEOUT=2700
CUSTOM_UPGRADE_PROPERTIES=$(cat "${CURRENT_DIR_NAME}/portal-ext.properties")
DATA_ARCHIVE_TYPE="data-archive-portal-partition-large"
PORTAL_VERSION="2024.q2.3"

function main {
	set -ex

	update_portal_ext_properties

	rebuild_legacy_database "${DATA_ARCHIVE_TYPE}" "${PORTAL_VERSION}"

	ant -f build-test.xml \
		-Dcustom.startup.timeout="${CUSTOM_STARTUP_TIMEOUT}" \
		-Dcustom.upgrade.properties="${CUSTOM_UPGRADE_PROPERTIES}" \
		-Dportal.version="${PORTAL_VERSION}" \
		-Dtest.class=playwright \
		upgrade-legacy-database

	print_upgrade_report "${LIFERAY_HOME}/tools/portal-tools-db-upgrade-client/reports/upgrade_report.txt"

	assert_clean_upgrade_log

	default_set_up
}

main "${@}"