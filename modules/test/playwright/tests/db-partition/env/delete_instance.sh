#!/bin/bash

# Deletes the virtual instance via the Liferay headless API.
# Must be called while the server is running (before shutdown).
#
# Required env vars:
# LIFERAY_HOME — path to the Liferay bundle
# STATE_FILE — path to the JSON state file written by Phase 1
# LIFERAY_PORTAL_URL — base URL of the portal (default: http://localhost:8080)
# ADMIN_PASSWORD — admin user password (default: test)

source "$(dirname "${BASH_SOURCE[0]}")/db_partition_common.sh"

LIFERAY_PORTAL_URL=${LIFERAY_PORTAL_URL:-"http://localhost:8080"}
ADMIN_PASSWORD=${ADMIN_PASSWORD:-"test"}

function main {
	local company_id
	company_id=$(read_partition_company_id) || exit 1

	echo "Deleting virtual instance for company ${company_id}."

	local http_code
	http_code=$(curl \
		--output /dev/null \
		--request DELETE \
		--silent \
		--url "${LIFERAY_PORTAL_URL}/headless-portal-instances/v1.0/portal-instances/${company_id}" \
		--user "test@liferay.com:${ADMIN_PASSWORD}" \
		--write-out "%{http_code}")

	if [[ "${http_code}" != "204" && "${http_code}" != "200" ]]
	then
		echo "Unable to delete virtual instance ${company_id}: HTTP ${http_code}."

		exit 1
	fi

	echo "Virtual instance ${company_id} deleted successfully."
}

main "${@}"