/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.background.task.internal.upgrade.v2_0_1;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.liferay.portal.kernel.upgrade.UpgradeProcess;

import java.io.Serializable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Jorge Avalos
 */
public class BackgroundTaskCompanyIdUpgradeProcess extends UpgradeProcess {

	public static void removeCompanyId(Map<String, Serializable> map) {
		Map<String, Serializable> taskContextMap =
			(Map<String, Serializable>)map.get("map");

		taskContextMap.remove("companyId");

		((Map<String, Serializable>)
			((Map<String, Serializable>)taskContextMap.get(
				"threadLocalValues")).get("map")).remove("companyId");
	}

	@Override
	protected void doUpgrade() throws Exception {
		try (PreparedStatement preparedStatement = connection.prepareStatement(
				"Select backgroundTaskId,taskContextMap from BackgroundTask " +
					"where taskContextMap LIKE \"%companyId%\"")) {

			ResultSet resultSet = preparedStatement.executeQuery();

			while (resultSet.next()) {
				String taskContextMapValue = resultSet.getString(
					"taskContextMap");

				if (taskContextMapValue != null) {
					long backgroundTaskId = resultSet.getLong(
						"backgroundTaskId");

					ObjectMapper mapper = new ObjectMapper();

					LinkedHashMap taskContextMap = mapper.readValue(
						taskContextMapValue, LinkedHashMap.class);

					removeCompanyId(taskContextMap);

					taskContextMapValue = mapper.writeValueAsString(
						taskContextMap);

					try (PreparedStatement updateSQL =
							connection.prepareStatement(
								"Update BackgroundTask set taskContextMap = ? where " +
									"backgroundTaskId = ?")) {

						updateSQL.setString(1, taskContextMapValue);

						updateSQL.setLong(2, backgroundTaskId);

						updateSQL.executeUpdate();
					}
				}
			}
		}
	}

}