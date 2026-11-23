#!/bin/bash

PLAYWRIGHT_PROJECT_NAME=${PLAYWRIGHT_PROJECT_NAME:-upgrade-data-archive}

source "$(dirname "${BASH_SOURCE[0]}")/../../env/common.sh"

function get_listener_pid {
	ss --listening --no-header --numeric --processes --tcp "sport = :${1}" | grep --max-count=1 --only-matching "pid=[0-9]*" | cut --characters=5-
}

function get_portal_port {
	local host_port=${LIFERAY_PORTAL_URL#*://}

	host_port=${host_port%%/*}

	if [[ ${host_port} == *:* ]]
	then
		echo "${host_port##*:}"
	elif [[ ${LIFERAY_PORTAL_URL} == https://* ]]
	then
		echo 443
	else
		echo 80
	fi
}

function main {
	set -e

	if [[ ${APP_SERVER_TYPE} != "tomcat" ]]
	then
		echo "Set APP_SERVER_TYPE to tomcat."

		exit 1
	fi

	local portal_port

	portal_port=$(get_portal_port)

	if [[ -n $(get_listener_pid "${portal_port}") ]] || curl --fail --output /dev/null --silent "${LIFERAY_PORTAL_URL}"
	then
		echo "Stop the portal on port ${portal_port} before generating a data archive."

		exit 1
	fi

	local liferay_home

	liferay_home=$(cd "${LIFERAY_HOME}" 2>/dev/null && pwd) || true

	if [[ -z ${liferay_home} ]] || [[ ! -d ${liferay_home}/osgi ]]
	then
		echo "Set LIFERAY_HOME to an existing bundle."

		exit 1
	fi

	LIFERAY_HOME=${liferay_home}

	_APP_SERVER_DIR=$(get_app_server_dir "${liferay_home}")

	if [[ -z ${_APP_SERVER_DIR} ]] || [[ ! -f ${_APP_SERVER_DIR}/bin/catalina.sh ]]
	then
		echo "Unable to find a Tomcat in ${liferay_home}."

		exit 1
	fi

	local database_container
	local database_name=lportal
	local database_type=postgresql
	local out_dir=.
	local type

	while [[ ${#} -gt 0 ]]
	do
		case ${1} in
			--database-container)
				database_container=${2}
				;;
			--database-name)
				database_name=${2}
				;;
			--database-type)
				database_type=${2}
				;;
			--out)
				out_dir=${2}
				;;
			--type)
				type=${2}
				;;
			*)
				echo "Usage: generate.sh --type <type> --database-container <container> [--database-name lportal] [--database-type postgresql] [--out <dir>]"

				exit 1
				;;
		esac

		shift 2
	done

	if [[ -z ${database_container} ]] || [[ -z ${type} ]]
	then
		echo "Usage: generate.sh --type <type> --database-container <container> [--database-name lportal] [--database-type postgresql] [--out <dir>]"

		exit 1
	fi

	mkdir -p "${out_dir}"

	out_dir=$(cd "${out_dir}" && pwd)

	rm -fr "${liferay_home}/data"

	touch "${liferay_home}/portal-ext.properties"

	combine_properties_files "${liferay_home}/portal-ext.properties" "${_PLAYWRIGHT_BASE_DIR}/env/portal-ext.properties"

	trap stop_started_app_server EXIT

	start_default_app_server

	local app_server_pid=${!}

	if [[ -z ${app_server_pid} ]] || [[ $(get_listener_pid "${portal_port}") != "${app_server_pid}" ]]
	then
		echo "The portal on port ${portal_port} is not the one this run started."

		exit 1
	fi

	cd "${_PLAYWRIGHT_BASE_DIR}"

	PORTAL_URL="${LIFERAY_PORTAL_URL}" node_modules/.bin/ts-node --files scripts/upgrade-data-archive/run.ts --liferay-home "${liferay_home}" --type "${type}"

	stop_started_app_server

	trap - EXIT

	DOCKER_ENABLED=true ant -f "${_PLAYWRIGHT_BASE_DIR}/scripts/upgrade-data-archive/export-database.xml" \
		-Ddata.archive.type="data-archive-${type}" \
		-Ddatabase."${database_type}".host="${database_container}" \
		-Ddatabase."${database_type}".schema="${database_name}" \
		-Ddatabase.type="${database_type}" \
		-Dliferay.home="${liferay_home}" \
		-Dout.dir="${out_dir}" \
		export-upgrade-data-archive
}

function stop_started_app_server {
	local app_server_pid=${!}

	if [[ -z ${app_server_pid} ]] || ! tr '\0' '\n' 2>/dev/null < "/proc/${app_server_pid}/cmdline" | grep --fixed-strings --line-regexp --quiet --regexp="-Dcatalina.base=${_APP_SERVER_DIR}"
	then
		echo "Skipping the app server stop because this run did not start one."

		return
	fi

	kill "${app_server_pid}"

	while kill -0 "${app_server_pid}" 2>/dev/null
	do
		sleep 5
	done

	echo "Stopped the app server this run started."
}

main "${@}"