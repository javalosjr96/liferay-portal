#!/bin/bash

CURRENT_DIR_NAME=$(dirname "${BASH_SOURCE[0]}")

echo "CURRENT_DIR_NAME=${CURRENT_DIR_NAME}"

source "${CURRENT_DIR_NAME}/../../../../env/common.sh"

DATA_ARCHIVE_TYPE="data-archive-portal-partition-large"
PORTAL_VERSION="2024.q2.3"

function main {
	set -ex

	update_portal_ext_properties

	rebuild_legacy_database "${DATA_ARCHIVE_TYPE}" "${PORTAL_VERSION}"

	default_set_up

	print_upgrade_report "${LIFERAY_HOME}/reports/upgrade_report.txt"
}

main "${@}"