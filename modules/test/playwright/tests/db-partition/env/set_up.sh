#!/bin/bash

source "$(dirname "${BASH_SOURCE[0]}")/../../../env/common.sh"

function main {
	if [[ "${PLAYWRIGHT_PROJECT_NAME}" == "db-partition.phase2" ]]
	then
		# Server is already running after partition import — handled by the
		# offline bridge that runs between the two Playwright phases.
		return 0
	fi

	default_set_up
}

main "${@}"