#!/bin/bash

# Imports all partitions listed in the partitionCompanyIds array in the state
# file. Each partition is imported sequentially and waits for completion before
# starting the next.
#
# Required env vars:
# LIFERAY_HOME — path to the Liferay bundle
# STATE_FILE — path to the JSON state file written by the Phase 1 spec

CURRENT_DIR_NAME=$(dirname ${BASH_SOURCE[0]})

source ${CURRENT_DIR_NAME}/db_partition_common.sh

function main {
	local company_ids
	company_ids=$(read_partition_company_ids) || exit 1

	local config_dir="${LIFERAY_HOME}/osgi/configs"

	mkdir -p "${config_dir}"

	while IFS= read -r company_id
	do
		echo "Importing partition for company ${company_id}."

		local config_file="${config_dir}/com.liferay.portal.instances.internal.configuration.ImportPortalInstanceConfiguration.config"

		echo "importCompanyId=L\"${company_id}\"" > "${config_file}"

		poll_config_deletion "${config_file}" "import"
	done <<< "${company_ids}"
}

main "${@}"