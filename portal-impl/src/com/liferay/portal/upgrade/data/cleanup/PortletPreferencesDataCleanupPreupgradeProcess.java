/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.upgrade.data.cleanup;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.upgrade.data.cleanup.DataCleanupPreupgradeProcess;
import com.liferay.portal.kernel.upgrade.data.cleanup.util.DataCleanupLoggingUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Luis Ortiz
 */
public class PortletPreferencesDataCleanupPreupgradeProcess
	extends DataCleanupPreupgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		List<Long> orphanPortletPreferencesIds = new ArrayList<>();

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				"select distinct portletPreferencesId " +
					"from PortletPreferenceValue " +
						"where portletPreferencesId is not null " +
							"and portletPreferencesId != 0 " +
								"and portletPreferencesId not in (" +
									"select portletPreferencesId from PortletPreferences)");
			ResultSet resultSet = preparedStatement.executeQuery()) {

			while (resultSet.next()) {
				orphanPortletPreferencesIds.add(resultSet.getLong(1));
			}
		}

		if (orphanPortletPreferencesIds.isEmpty()) {
			return;
		}

		for (int i = 0; i < orphanPortletPreferencesIds.size();
			 i += _BATCH_SIZE) {

			int end = Math.min(
				i + _BATCH_SIZE, orphanPortletPreferencesIds.size());

			List<String> batchStrings = new ArrayList<>(end - i);

			for (int j = i; j < end; j++) {
				batchStrings.add(
					String.valueOf(orphanPortletPreferencesIds.get(j)));
			}

			try (PreparedStatement preparedStatement =
					connection.prepareStatement(
						StringBundler.concat(
							"delete from PortletPreferenceValue ",
							"where portletPreferencesId in (",
							String.join(
								StringPool.COMMA_AND_SPACE, batchStrings),
							")"))) {

				int count = preparedStatement.executeUpdate();

				DataCleanupLoggingUtil.logDelete(
					_log, count, "PortletPreferenceValue",
					"portletPreferencesId was not found in PortletPreferences");
			}
		}
	}

	private static final int _BATCH_SIZE = 1000;

	private static final Log _log = LogFactoryUtil.getLog(
		PortletPreferencesDataCleanupPreupgradeProcess.class);

}