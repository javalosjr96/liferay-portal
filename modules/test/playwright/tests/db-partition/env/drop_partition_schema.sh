#!/bin/bash

# Drops the standalone lpartition_{id} schema after the partition has been
# exported. Must run while the app server is DOWN.
#
# Required env vars:
#   DATABASE_TYPE      — mysql or postgresql
#   DATABASE_HOST      — DB host (docker container host in CI)
#   DATABASE_PASSWORD  — DB password
#   DATABASE_USER      — DB username
#   STATE_FILE         — path to the JSON file written by Phase 1
#                        (default: modules/test/playwright/test-results/db-partition-state.json)

STATE_FILE=${STATE_FILE:-modules/test/playwright/test-results/db-partition-state.json}

function main {
	if [[ -z "${DATABASE_TYPE}" ]] || [[ -z "${DATABASE_HOST}" ]] || [[ -z "${DATABASE_USER}" ]]
	then
		echo "DATABASE_TYPE, DATABASE_HOST, and DATABASE_USER must be set."

		exit 1
	fi

	if [[ ! -f "${STATE_FILE}" ]]
	then
		echo "State file not found: ${STATE_FILE}"

		exit 1
	fi

	local partition_company_id

	partition_company_id=$(grep -oP '"partitionCompanyId":\s*\K[0-9]+' "${STATE_FILE}")

	if [[ -z "${partition_company_id}" ]]
	then
		echo "Unable to read partitionCompanyId from ${STATE_FILE}."

		exit 1
	fi

	local schema_name="lpartition_${partition_company_id}"

	echo "Dropping ${schema_name} (${DATABASE_TYPE})."

	if [[ "${DATABASE_TYPE}" == "mysql" ]]
	then
		mysql \
			--host="${DATABASE_HOST}" \
			--password="${DATABASE_PASSWORD}" \
			--user="${DATABASE_USER}" \
			--execute="DROP SCHEMA IF EXISTS ${schema_name};"
	elif [[ "${DATABASE_TYPE}" == "postgresql" ]]
	then
		PGPASSWORD="${DATABASE_PASSWORD}" psql \
			--command="DROP SCHEMA IF EXISTS ${schema_name} CASCADE;" \
			--host="${DATABASE_HOST}" \
			--username="${DATABASE_USER}"
	else
		echo "Unsupported DATABASE_TYPE: ${DATABASE_TYPE}"

		exit 1
	fi

	echo "Dropped ${schema_name}."
}

main "${@}"
