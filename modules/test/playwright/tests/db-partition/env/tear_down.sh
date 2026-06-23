#!/bin/bash

source "$(dirname "${BASH_SOURCE[0]}")/../../../env/common.sh"

function main {
	if [[ "${PLAYWRIGHT_PROJECT_NAME}" == "db-partition.phase1" ]]
	then
		# Server is kept running after phase 1 so the offline bridge can
		# trigger the partition export before stopping it.
		return 0
	fi

	default_tear_down
}

main "${@}"