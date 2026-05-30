/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.upgrade.data.cleanup;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.upgrade.data.cleanup.util.DataCleanupLoggingUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Luis Ortiz
 */
public class DefaultAllTablesOrphanReferencesDataCleanupPreupgradeProcess
	extends BaseAllTablesOrphanReferencesDataCleanupPreupgradeProcess {

	public DefaultAllTablesOrphanReferencesDataCleanupPreupgradeProcess(
		String targetColumnName, String targetTableName) {

		super(targetColumnName, targetTableName);
	}

	@Override
	protected void cleanUp(
			String sourceColumnName, String sourceTableName,
			String[] targetColumnNames, String targetTableName)
		throws Exception {

		List<Long> orphanIds = new ArrayList<>();

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select distinct ", sourceColumnName, " from ",
					sourceTableName, " where ", sourceColumnName,
					" != 0 and ", sourceColumnName, " is not null and ",
					sourceColumnName, " not in (select ", targetColumnNames[0],
					" from ", targetTableName, ")"));
			ResultSet resultSet = preparedStatement.executeQuery()) {

			while (resultSet.next()) {
				orphanIds.add(resultSet.getLong(1));
			}
		}

		if (orphanIds.isEmpty()) {
			return;
		}

		for (int i = 0; i < orphanIds.size(); i += _BATCH_SIZE) {
			int end = Math.min(i + _BATCH_SIZE, orphanIds.size());

			List<String> batchStrings = new ArrayList<>(end - i);

			for (int j = i; j < end; j++) {
				batchStrings.add(String.valueOf(orphanIds.get(j)));
			}

			try (PreparedStatement preparedStatement =
					connection.prepareStatement(
						StringBundler.concat(
							"delete from ", sourceTableName, " where ",
							sourceColumnName, " in (",
							String.join(
								StringPool.COMMA_AND_SPACE, batchStrings),
							")"))) {

				int deleted = preparedStatement.executeUpdate();

				if (deleted > 0) {
					DataCleanupLoggingUtil.logDelete(
						_log, deleted, sourceTableName,
						StringBundler.concat(
							sourceColumnName, " was not found in column",
							(targetColumnNames.length > 1) ? "s " : " ",
							String.join(", ", targetColumnNames), " from table ",
							targetTableName));
				}
			}
		}
	}

	private static final int _BATCH_SIZE = 1000;

	private static final Log _log = LogFactoryUtil.getLog(
		DefaultAllTablesOrphanReferencesDataCleanupPreupgradeProcess.class);

}
