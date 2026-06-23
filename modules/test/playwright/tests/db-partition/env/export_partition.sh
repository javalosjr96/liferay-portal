#!/bin/bash

# Triggers a partition export and waits for Liferay to process it.

source "$(dirname "${BASH_SOURCE[0]}")/db_partition_common.sh"

function main {
	local partition_company_id

	partition_company_id=$(read_partition_company_id) || exit 1

	echo "Exporting partition for company ${partition_company_id}."

	local config_dir="${LIFERAY_HOME}/osgi/configs"
	local config_file="${config_dir}/com.liferay.portal.instances.internal.configuration.ExportPortalInstanceConfiguration.config"

	mkdir -p "${config_dir}"

	echo "exportCompanyId=L\"${partition_company_id}\"" > "${config_file}"

	poll_config_deletion "${config_file}" "export"
}

main "${@}"