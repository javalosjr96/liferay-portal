#!/bin/bash

CURRENT_DIR_NAME=$(dirname "${BASH_SOURCE[0]}")

echo "CURRENT_DIR_NAME=${CURRENT_DIR_NAME}"

source "${CURRENT_DIR_NAME}/../../../../env/common.sh"

DATA_ARCHIVE_TYPE="data-archive-content-pages"
PORTAL_VERSION="${UPGRADE_DATA_ARCHIVE_PORTAL_VERSION:-master}"

function main {
	set -ex

	upgrade_legacy_database_set_up "${DATA_ARCHIVE_TYPE}" "${PORTAL_VERSION}"
}

main "${@}"