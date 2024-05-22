/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.background.task.internal.upgrade.v2_0_1;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.LoggingTimer;

import java.io.Serializable;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Jorge Avalos
 */
public class BackgroundTaskCompanyIdUpgradeProcess extends UpgradeProcess {

	public static void removeCompanyId(Map<String, Serializable> map) {
		Iterator<Map.Entry<String, Serializable>> iterator = map.entrySet(
		).iterator();

		while (iterator.hasNext()) {
			Map.Entry<String, Serializable> entry = iterator.next();

			String key = entry.getKey();

			if (key.equals("companyId")) {
				iterator.remove();
			}
			else {
				Object value = entry.getValue();

				if (value instanceof LinkedHashMap) {
					removeCompanyId((Map<String, Serializable>)value);
				}
			}
		}
	}

	@Override
	protected void doUpgrade() throws Exception {
		try (LoggingTimer loggingTimer = new LoggingTimer()) {
			processConcurrently(
				"Select backgroundTaskId,taskContextMap from BackgroundTask " +
					"where taskContextMap LIKE \"%companyId:%\"",
				"Update BackgroundTask set taskContextMap = ? where " +
					"backgroundTaskId = ?",
				resultSet -> new Object[] {
					resultSet.getLong("backgroundTaskId"),
					GetterUtil.getString(resultSet.getString("taskContextMap"))
				},
				(values, preparedStatement) -> {
					String taskContextMapValue = (String)values[1];

					System.out.println(taskContextMapValue);

					if (taskContextMapValue != null) {
						long backgroundTaskId = (Long)values[0];

						ObjectMapper mapper = new ObjectMapper();

						Map<String, Serializable> taskContextMap =
							mapper.readValue(
								taskContextMapValue, LinkedHashMap.class);

						removeCompanyId(taskContextMap);

						taskContextMapValue = mapper.writeValueAsString(
							taskContextMap);

						System.out.println(taskContextMapValue);

						preparedStatement.setString(1, taskContextMapValue);

						preparedStatement.setLong(2, backgroundTaskId);

						preparedStatement.addBatch();
					}
				},
				"Unable to remove companyId");
		}
	}

}