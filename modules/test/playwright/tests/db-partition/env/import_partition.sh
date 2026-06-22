#!/bin/bash

# Triggers a partition import and waits for Liferay to process it.
#
# Required env vars:
# LIFERAY_HOME — path to the Liferay bundle (e.g. /opt/liferay)
# STATE_FILE — path to the JSON file written by the Phase 1 Playwright spec
# (default: modules/test/playwright/test-results/db-partition-state.json)

STATE_FILE=${STATE_FILE:-modules/test/playwright/test-results/db-partition-state.json}

function main {
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

	local partition_company_id

	partition_company_id=$(grep -oP '"partitionCompanyId":\s*\K[0-9]+' "${STATE_FILE}")

	if [[ -z "${partition_company_id}" ]]
	then
		echo "Unable to read partitionCompanyId from ${STATE_FILE}."

		exit 1
	fi

	echo "Importing partition for company ${partition_company_id}."

	local config_dir="${LIFERAY_HOME}/osgi/configs"
	local config_file="${config_dir}/com.liferay.portal.instances.internal.configuration.ImportPortalInstanceConfiguration.config"

	mkdir -p "${config_dir}"

	echo "importCompanyId=L\"${partition_company_id}\"" > "${config_file}"

	echo "Waiting for Liferay to process the import..."

	local attempts=0
	local max_attempts=75

	while [[ -f "${config_file}" ]]
	do
		if [[ ${attempts} -ge ${max_attempts} ]]
		then
			echo "Unable to import partition: timed out waiting for ${config_file} to be deleted."

			exit 1
		fi

		sleep 8

		((attempts++))
	done

	echo "Partition import is complete."
}

main "${@}"