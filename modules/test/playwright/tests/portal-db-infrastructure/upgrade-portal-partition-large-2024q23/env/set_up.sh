#!/bin/bash

CURRENT_DIR_NAME=$(dirname "${BASH_SOURCE[0]}")

echo "CURRENT_DIR_NAME=${CURRENT_DIR_NAME}"

source "${CURRENT_DIR_NAME}/../../../../env/common.sh"

CUSTOM_UPGRADE_PROPERTIES=$(cat "${CURRENT_DIR_NAME}/portal-ext.properties")
DATA_ARCHIVE_TYPE="data-archive-portal-partition-large"
PORTAL_VERSION="2024.q2.3"

function main {
	set -ex

	update_portal_ext_properties

	upgrade_legacy_database_set_up "${DATA_ARCHIVE_TYPE}" "${PORTAL_VERSION}" "${CUSTOM_UPGRADE_PROPERTIES}"

	print_upgrade_report "${LIFERAY_HOME}/tools/portal-tools-db-upgrade-client/reports/upgrade_report.txt"
}

main "${@}"