#!/bin/bash

PLAYWRIGHT_PROJECT_NAME=${PLAYWRIGHT_PROJECT_NAME:-upgrade-data-archive}

source "$(dirname "${BASH_SOURCE[0]}")/../../env/common.sh"

function main {
	set -e

	if curl --fail --output /dev/null --silent "${LIFERAY_PORTAL_URL}"
	then
		echo "Stop the portal at ${LIFERAY_PORTAL_URL} before generating a data archive."

		exit 1
	fi

	local liferay_home=$(cd "${LIFERAY_HOME}" && pwd)

	if [[ -z ${liferay_home} ]] || [[ ! -d ${liferay_home}/osgi ]]
	then
		echo "Set LIFERAY_HOME to an existing bundle."

		exit 1
	fi

	local args=()

	while [[ ${#} -gt 0 ]]
	do
		if [[ ${1} == "--out" ]]
		then
			mkdir -p "${2}"

			args+=("${1}" "$(cd "${2}" && pwd)")
		else
			args+=("${1}" "${2}")
		fi

		shift 2
	done

	rm -fr "${liferay_home}/data"

	trap stop_default_app_server EXIT

	start_default_app_server

	cd "${_PLAYWRIGHT_BASE_DIR}"

	PORTAL_URL="${LIFERAY_PORTAL_URL}" node_modules/.bin/ts-node --files scripts/upgrade-data-archive/run.ts --liferay-home "${liferay_home}" "${args[@]}"
}

main "${@}"