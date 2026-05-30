/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.upgrade.data.cleanup;

import com.liferay.petra.lang.SafeCloseable;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.db.DBInspector;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.upgrade.data.cleanup.DataCleanupPreupgradeProcess;
import com.liferay.portal.kernel.upgrade.data.cleanup.util.DataCleanupLoggingUtil;
import com.liferay.portal.kernel.upgrade.data.cleanup.util.OrphanReferencesDataCleanupUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Luis Ortiz
 */
public class DDMStorageLinkDataCleanupPreupgradeProcess
	extends DataCleanupPreupgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		DBInspector dbInspector = new DBInspector(connection);

		if (!dbInspector.hasTable("DDMStorageLink") ||
			!dbInspector.hasColumn("DDMStorageLink", "classPK")) {

			return;
		}

		List<SafeCloseable> safeCloseables =
			OrphanReferencesDataCleanupUtil.addTemporaryIndexes(
				new String[] {"classPK"}, connection, DBManagerUtil.getDB(),
				"DDMStorageLink");

		try {
			if (dbInspector.hasTable("DDMContent")) {
				OrphanReferencesDataCleanupUtil.cleanUpTable(
					connection, null, false, null, "contentId", "DDMContent",
					new String[] {"classPK"}, "DDMStorageLink", true);
			}

			if (dbInspector.hasTable("DDMField")) {
				_cleanUpOrphans("DDMField", "storageId");
			}

			if (dbInspector.hasTable("DDMFieldAttribute")) {
				_cleanUpOrphans("DDMFieldAttribute", "storageId");
			}
		}
		finally {
			for (SafeCloseable safeCloseable : safeCloseables) {
				safeCloseable.close();
			}
		}
	}

	private void _cleanUpOrphans(String tableName, String columnName)
		throws Exception {

		List<Long> orphanIds = new ArrayList<>();

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select distinct ", columnName, " from ", tableName,
					" where ", columnName, " is not null and ", columnName,
					" != 0 and ", columnName,
					" not in (select classPK from DDMStorageLink)"));
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
							"delete from ", tableName, " where ", columnName,
							" in (",
							String.join(
								StringPool.COMMA_AND_SPACE, batchStrings),
							")"))) {

				int count = preparedStatement.executeUpdate();

				DataCleanupLoggingUtil.logDelete(
					_log, count, tableName,
					StringBundler.concat(
						columnName, " was not found in DDMStorageLink"));
			}
		}
	}

	private static final int _BATCH_SIZE = 1000;

	private static final Log _log = LogFactoryUtil.getLog(
		DDMStorageLinkDataCleanupPreupgradeProcess.class);

}