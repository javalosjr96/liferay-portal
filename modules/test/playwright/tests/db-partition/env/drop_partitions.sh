#!/bin/bash

# Drops all partition schemas listed in the partitionCompanyIds array in the
# state file. Called offline (server must be stopped) between rebuild steps.
#
# Required env vars:
# DATABASE_HOST — Docker container name for the database
# DATABASE_PASSWORD — database password
# DATABASE_TYPE — "mysql" or "postgresql"
# DATABASE_USERNAME — database username
# LIFERAY_HOME — path to the Liferay bundle
# STATE_FILE — path to the JSON state file written by the Phase 1 spec

source "$(dirname "${BASH_SOURCE[0]}")/db_partition_common.sh"

function main {
	local company_ids
	company_ids=$(read_partition_company_ids) || exit 1

	while IFS= read -r company_id
	do
		echo "Dropping schema lpartition_${company_id}."

		if [[ "${DATABASE_TYPE}" == "mysql" ]]
		then
			docker exec "${DATABASE_HOST}" mysql \
				--execute="DROP SCHEMA IF EXISTS lpartition_${company_id};" \
				--host=127.0.0.1 \
				--password="${DATABASE_PASSWORD}" \
				--user="${DATABASE_USERNAME}"
		elif [[ "${DATABASE_TYPE}" == "postgresql" ]]
		then
			docker exec "${DATABASE_HOST}" /bin/bash -c \
				"PGPASSWORD=${DATABASE_PASSWORD} psql --command='DROP SCHEMA IF EXISTS lpartition_${company_id} CASCADE;' --username=${DATABASE_USERNAME}"
		fi
	done <<< "${company_ids}"
}

main "${@}"