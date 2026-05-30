/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.upgrade.data.cleanup;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.db.DBInspector;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.upgrade.data.cleanup.util.DataCleanupLoggingUtil;
import com.liferay.portal.kernel.upgrade.data.cleanup.util.OrphanReferencesDataCleanupUtil;
import com.liferay.portal.kernel.util.StringUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Luis Ortiz
 */
public class FilterableAllTablesOrphanReferencesDataCleanupPreupgradeProcess
	extends BaseAllTablesOrphanReferencesDataCleanupPreupgradeProcess {

	public FilterableAllTablesOrphanReferencesDataCleanupPreupgradeProcess(
		String sourceAdditionalWhereClause,
		String[] sourceAdditionalColumnNamesCheck, String sourceColumnName,
		String[] targetColumnNames, String targetTableName) {

		super(sourceColumnName, targetColumnNames, targetTableName);

		_sourceAdditionalWhereClause = sourceAdditionalWhereClause;
		_sourceAdditionalColumnNamesCheck = sourceAdditionalColumnNamesCheck;
	}

	@Override
	protected void cleanUp(
			String sourceColumnName, String sourceTableName,
			String[] targetColumnNames, String targetTableName)
		throws Exception {

		String alias = OrphanReferencesDataCleanupUtil.getSourceTableAlias();

		String whereClause = OrphanReferencesDataCleanupUtil.getWhereClause(
			connection, null, _sourceAdditionalWhereClause, sourceColumnName,
			sourceTableName, targetColumnNames, targetTableName);

		// Phase 1: find distinct orphan classPKs via LEFT JOIN anti-join

		List<Long> orphanIds = new ArrayList<>();

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select distinct ", alias, ".", sourceColumnName, " from ",
					sourceTableName, " ", alias, whereClause));
			ResultSet resultSet = preparedStatement.executeQuery()) {

			while (resultSet.next()) {
				orphanIds.add(resultSet.getLong(1));
			}
		}

		if (orphanIds.isEmpty()) {
			return;
		}

		// Phase 2: delete orphans in batches

		String resolvedAdditionalWhereClause = StringUtil.replace(
			_sourceAdditionalWhereClause, "[$SOURCE_TABLE_ALIAS$]",
			sourceTableName);

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
							") and ", resolvedAdditionalWhereClause))) {

				int deleted = preparedStatement.executeUpdate();

				if (deleted > 0) {
					DataCleanupLoggingUtil.logDelete(
						_log, deleted, sourceTableName,
						StringBundler.concat(
							sourceColumnName, " was not found in column",
							(targetColumnNames.length > 1) ? "s " : " ",
							String.join(", ", targetColumnNames),
							" from table ", targetTableName));
				}
			}
		}
	}

	@Override
	protected boolean shouldSkipSourceTable(
			DBInspector dbInspector, String sourceTableName)
		throws Exception {

		for (String sourceAdditionalColumnName :
				_sourceAdditionalColumnNamesCheck) {

			if (!dbInspector.hasColumn(
					sourceTableName, sourceAdditionalColumnName)) {

				return true;
			}
		}

		return false;
	}

	private static final int _BATCH_SIZE = 1000;

	private static final Log _log = LogFactoryUtil.getLog(
		FilterableAllTablesOrphanReferencesDataCleanupPreupgradeProcess.class);

	private final String[] _sourceAdditionalColumnNamesCheck;
	private final String _sourceAdditionalWhereClause;

}