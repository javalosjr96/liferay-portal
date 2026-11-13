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

	rm -fr "${LIFERAY_HOME}/data"

	start_default_app_server

	trap stop_default_app_server EXIT

	cd "${_PLAYWRIGHT_BASE_DIR}"

	node_modules/.bin/ts-node --files scripts/upgrade-data-archive/run.ts --liferay-home "${LIFERAY_HOME}" "${@}"
}

main "${@}"