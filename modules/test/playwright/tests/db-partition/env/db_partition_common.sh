#!/bin/bash

# Shared helpers for export_partition.sh and import_partition.sh.
#
# Required env vars:
# LIFERAY_HOME — path to the Liferay bundle (e.g. /opt/liferay)
# STATE_FILE — path to the JSON file written by the Phase 1 Playwright spec
# (default: modules/test/playwright/test-results/db-partition-state.json)

STATE_FILE=${STATE_FILE:-modules/test/playwright/test-results/db-partition-state.json}

function poll_config_deletion {
	local config_file=${1}
	local operation=${2}

	echo "Waiting for Liferay to process the ${operation}..."

	local attempts=0
	local max_attempts=75

	while [[ -f "${config_file}" ]]
	do
		if [[ ${attempts} -ge ${max_attempts} ]]
		then
			echo "Unable to ${operation} partition: timed out waiting for ${config_file} to be deleted."

			exit 1
		fi

		sleep 8

		((attempts++))
	done

	echo "Partition ${operation} is complete."
}

function read_partition_company_id {
	if [[ -z "${LIFERAY_HOME}" ]]
	then
		echo "LIFERAY_HOME is not set."

		exit 1
	fi

	if [[ ! -f "${STATE_FILE}" ]]
	then
		echo "State file was not found: ${STATE_FILE}."

		exit 1
	fi

	local company_id

	company_id=$(grep -oP '"partitionCompanyId":\s*\K[0-9]+' "${STATE_FILE}")

	if [[ -z "${company_id}" ]]
	then
		echo "Unable to read partitionCompanyId from ${STATE_FILE}."

		exit 1
	fi

	echo "${company_id}"
}
