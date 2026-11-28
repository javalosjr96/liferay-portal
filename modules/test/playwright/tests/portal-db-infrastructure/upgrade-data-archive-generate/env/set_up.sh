#!/bin/bash

CURRENT_DIR_NAME=$(dirname "${BASH_SOURCE[0]}")

echo "CURRENT_DIR_NAME=${CURRENT_DIR_NAME}"

source "${CURRENT_DIR_NAME}/../../../../env/common.sh"

function main {
	set -ex

	if [[ -z "${UPGRADE_DATA_ARCHIVE_PORTAL_VERSION}" ]]
	then
		echo "Unable to generate the upgrade data archive because UPGRADE_DATA_ARCHIVE_PORTAL_VERSION is not set."

		exit 1
	fi

	if [[ "${DATABASE_TYPE}" != "postgresql" ]]
	then
		echo "Unable to generate the upgrade data archive because only PostgreSQL is supported."

		exit 1
	fi

	local jdbc_url

	jdbc_url=$(grep "^jdbc\.default\.url=" "${_PORTAL_PROJECT_DIR}/portal-impl/src/portal-ext.properties" | tail --lines=1)

	if [[ -z "${jdbc_url}" ]]
	then
		echo "Unable to generate the upgrade data archive because \"jdbc.default.url\" is not set in \"portal-impl/src/portal-ext.properties\"."

		exit 1
	fi

	local database_host
	local database_name

	database_host=$(echo "${jdbc_url}" | sed --regexp-extended "s#.*://([^:/]+).*#\1#")
	database_name=$(echo "${jdbc_url}" | sed --regexp-extended "s#.*://[^/]+/([^?]+).*#\1#")

	if ! docker inspect "${database_host}" > /dev/null 2>&1
	then
		echo "Unable to generate the upgrade data archive because the database \"${database_host}\" is not a Docker container."

		exit 1
	fi

	local archive_dir="${_PORTAL_PROJECT_DIR}/build/upgrade-data-archive/${UPGRADE_DATA_ARCHIVE_PORTAL_VERSION}"
	local archive_type="${UPGRADE_DATA_ARCHIVE_TYPE:-cms}"

	rm -f "${archive_dir}/data-archive-${archive_type}-postgresql.zip"

	"${_PLAYWRIGHT_BASE_DIR}/scripts/upgrade-data-archive/generate.sh" \
		--database-container "${database_host}" \
		--database-name "${database_name}" \
		--database-type postgresql \
		--out "${archive_dir}" \
		--type "${archive_type}"
}

main "${@}"