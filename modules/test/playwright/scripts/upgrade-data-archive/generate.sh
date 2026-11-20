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

	local args=()

	while [[ ${#} -gt 0 ]]
	do
		if [[ ${1} == "--out" ]]
		then
			mkdir -p "${2}"

			args+=("${1}" "$(cd "${2}" && pwd)")
		else
			args+=("${1}" "${2}")
		fi

		shift 2
	done

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

	PORTAL_URL="${LIFERAY_PORTAL_URL}" node_modules/.bin/ts-node --files scripts/upgrade-data-archive/run.ts --liferay-home "${liferay_home}" "${args[@]}"
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