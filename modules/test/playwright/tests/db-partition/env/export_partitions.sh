#!/bin/bash

# Exports all partitions listed in the partitionCompanyIds array in the state
# file. Each partition is exported sequentially and waits for completion before
# starting the next.
#
# Required env vars:
# LIFERAY_HOME — path to the Liferay bundle
# STATE_FILE — path to the JSON state file written by the Phase 1 spec

source "$(dirname "${BASH_SOURCE[0]}")/db_partition_common.sh"

function main {
	local company_ids
	company_ids=$(read_partition_company_ids) || exit 1

	local config_dir="${LIFERAY_HOME}/osgi/configs"

	mkdir -p "${config_dir}"

	while IFS= read -r company_id
	do
		echo "Exporting partition for company ${company_id}."

		local config_file="${config_dir}/com.liferay.portal.instances.internal.configuration.ExportPortalInstanceConfiguration.config"

		echo "exportCompanyId=L\"${company_id}\"" > "${config_file}"

		poll_config_deletion "${config_file}" "export"
	done <<< "${company_ids}"
}

main "${@}"