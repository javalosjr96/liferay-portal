/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.upgrade.data.cleanup;

import com.liferay.petra.lang.SafeCloseable;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.dao.db.DBInspector;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.db.DBResourceUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.upgrade.data.cleanup.util.OrphanReferencesDataCleanupUtil;
import com.liferay.portal.kernel.util.PropsValues;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Jorge Avalos
 */
public class
	MultiFilterableAllTablesOrphanReferencesDataCleanupPreupgradeProcess
		extends DataCleanupPreupgradeProcess {

	public MultiFilterableAllTablesOrphanReferencesDataCleanupPreupgradeProcess(
		FilterConfig... filterConfigs) {

		_filterConfigs = filterConfigs;
	}

	public static class FilterConfig {

		public FilterConfig(
			String sourceAdditionalWhereClause,
			String[] sourceAdditionalColumnNamesCheck, String sourceColumnName,
			String[] targetColumnNames, String targetTableName) {

			_sourceAdditionalWhereClause = sourceAdditionalWhereClause;
			_sourceAdditionalColumnNamesCheck =
				sourceAdditionalColumnNamesCheck;
			_sourceColumnName = sourceColumnName;
			_targetColumnNames = targetColumnNames;
			_targetTableName = targetTableName;
		}

		private final String[] _sourceAdditionalColumnNamesCheck;
		private final String _sourceAdditionalWhereClause;
		private final String _sourceColumnName;
		private final String[] _targetColumnNames;
		private final String _targetTableName;

	}

	@Override
	protected void doUpgrade() throws Exception {
		DBInspector dbInspector = new DBInspector(connection);

		List<_PreparedConfig> preparedConfigs = new ArrayList<>();
		List<SafeCloseable> allSafeCloseables = new ArrayList<>();

		for (FilterConfig filterConfig : _filterConfigs) {
			String targetTableName = dbInspector.normalizeName(
				filterConfig._targetTableName);

			if (!dbInspector.hasTable(targetTableName) &&
				!(PropsValues.DATABASE_PARTITION_ENABLED &&
				  dbInspector.isControlTable(targetTableName) &&
				  dbInspector.hasView(targetTableName))) {

				if (_log.isDebugEnabled()) {
					_log.debug(
						"Table \"" + targetTableName + "\" does not exist");
				}

				continue;
			}

			String[] targetColumnNames =
				new String[filterConfig._targetColumnNames.length];

			boolean valid = true;

			for (int i = 0; i < filterConfig._targetColumnNames.length; i++) {
				targetColumnNames[i] = dbInspector.normalizeName(
					filterConfig._targetColumnNames[i]);

				if (!dbInspector.hasColumn(
						targetTableName, targetColumnNames[i])) {

					if (_log.isDebugEnabled()) {
						_log.debug(
							StringBundler.concat(
								"Table ", targetTableName,
								" does not have column ",
								targetColumnNames[i]));
					}

					valid = false;

					break;
				}
			}

			if (!valid) {
				continue;
			}

			boolean[] numericTargetColumns =
				new boolean[targetColumnNames.length];

			for (int i = 0; i < targetColumnNames.length; i++) {
				numericTargetColumns[i] = dbInspector.isNumeric(
					targetTableName, targetColumnNames[i]);
			}

			List<SafeCloseable> safeCloseables =
				OrphanReferencesDataCleanupUtil.addTemporaryIndexes(
					targetColumnNames, connection, DBManagerUtil.getDB(),
					targetTableName);

			allSafeCloseables.addAll(safeCloseables);

			String sourceColumnName = dbInspector.normalizeName(
				filterConfig._sourceColumnName);

			preparedConfigs.add(
				new _PreparedConfig(
					filterConfig._sourceAdditionalColumnNamesCheck,
					filterConfig._sourceAdditionalWhereClause, sourceColumnName,
					targetColumnNames, targetTableName, numericTargetColumns));
		}

		if (preparedConfigs.isEmpty()) {
			return;
		}

		List<String> excludedTableNames =
			OrphanReferencesDataCleanupUtil.getNormalizedExcludedTableNames(
				connection);

		Set<String> liferayTableNames = DBResourceUtil.getLiferayTableNames(
			connection);

		List<String> tableNames = dbInspector.getTableNames(null);

		Collections.sort(tableNames);

		try {
			processConcurrently(
				tableNames.toArray(new String[0]),
				sourceTableName -> {
					if (excludedTableNames.contains(sourceTableName)) {
						return;
					}

					for (_PreparedConfig preparedConfig : preparedConfigs) {
						if (sourceTableName.equals(
								preparedConfig._targetTableName) ||
							!dbInspector.hasColumn(
								sourceTableName,
								preparedConfig._sourceColumnName)) {

							continue;
						}

						boolean skip = false;

						for (String sourceAdditionalColumnName :
								preparedConfig.
									_sourceAdditionalColumnNamesCheck) {

							if (!dbInspector.hasColumn(
									sourceTableName,
									sourceAdditionalColumnName)) {

								skip = true;

								break;
							}
						}

						if (skip) {
							continue;
						}

						boolean numericSourceColumn = dbInspector.isNumeric(
							sourceTableName, preparedConfig._sourceColumnName);

						boolean compatibleTypes = true;

						for (int i = 0;
							 i < preparedConfig._targetColumnNames.length;
							 i++) {

							if (numericSourceColumn !=
									preparedConfig._numericTargetColumns[i]) {

								String message = StringBundler.concat(
									"Table ", sourceTableName, " and column ",
									preparedConfig._sourceColumnName,
									" has an incompatible type with table ",
									preparedConfig._targetTableName,
									" and column ",
									preparedConfig._targetColumnNames[i]);

								compatibleTypes = false;

								if (!dbInspector.isObjectTable(
										sourceTableName) &&
									!liferayTableNames.contains(
										sourceTableName)) {

									if (_log.isDebugEnabled()) {
										_log.debug(message);
									}
								}
								else if (_log.isWarnEnabled()) {
									_log.warn(message);
								}

								break;
							}
						}

						if (!compatibleTypes) {
							continue;
						}

						OrphanReferencesDataCleanupUtil.cleanUpTable(
							connection, null, false,
							preparedConfig._sourceAdditionalWhereClause,
							preparedConfig._sourceColumnName, sourceTableName,
							preparedConfig._targetColumnNames,
							preparedConfig._targetTableName, true);
					}
				},
				null);
		}
		finally {
			for (SafeCloseable safeCloseable : allSafeCloseables) {
				safeCloseable.close();
			}
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		MultiFilterableAllTablesOrphanReferencesDataCleanupPreupgradeProcess.
			class);

	private final FilterConfig[] _filterConfigs;

	private static class _PreparedConfig {

		private _PreparedConfig(
			String[] sourceAdditionalColumnNamesCheck,
			String sourceAdditionalWhereClause, String sourceColumnName,
			String[] targetColumnNames, String targetTableName,
			boolean[] numericTargetColumns) {

			_sourceAdditionalColumnNamesCheck =
				sourceAdditionalColumnNamesCheck;
			_sourceAdditionalWhereClause = sourceAdditionalWhereClause;
			_sourceColumnName = sourceColumnName;
			_targetColumnNames = targetColumnNames;
			_targetTableName = targetTableName;
			_numericTargetColumns = numericTargetColumns;
		}

		private final boolean[] _numericTargetColumns;
		private final String[] _sourceAdditionalColumnNamesCheck;
		private final String _sourceAdditionalWhereClause;
		private final String _sourceColumnName;
		private final String[] _targetColumnNames;
		private final String _targetTableName;

	}

}