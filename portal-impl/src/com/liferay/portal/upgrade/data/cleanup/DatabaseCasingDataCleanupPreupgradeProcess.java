/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.upgrade.data.cleanup;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.db.DBResourceUtil;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBInspector;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.upgrade.data.cleanup.DataCleanupPreupgradeProcess;
import com.liferay.portal.kernel.upgrade.data.cleanup.util.DataCleanupLoggingUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author Jorge Avalos
 */
public class DatabaseCasingDataCleanupPreupgradeProcess
	extends DataCleanupPreupgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		Set<String> expectedTableNames = new TreeSet<>();

		expectedTableNames.addAll(
			DBResourceUtil.getServiceComponentModuleTableNames(connection));
		expectedTableNames.addAll(
			DBResourceUtil.getServiceComponentPortalTableNames(connection));
		expectedTableNames.addAll(
			DBResourceUtil.getModuleTableNames(connection));
		expectedTableNames.addAll(
			DBResourceUtil.getPortalTableNames(connection));

		DBInspector dbInspector = new DBInspector(connection);

		Map<String, String> tableNames = new TreeMap<>(
			String.CASE_INSENSITIVE_ORDER);

		for (String tableName : dbInspector.getTableNames(null)) {
			tableNames.put(tableName, tableName);
		}

		for (String expectedTableName : expectedTableNames) {
			expectedTableName = dbInspector.normalizeName(expectedTableName);

			String tableName = tableNames.get(expectedTableName);

			if ((tableName == null) || !tableName.equals(expectedTableName)) {
				continue;
			}

			DataCleanupLoggingUtil.logAlter(
				_log, expectedTableName, "incorrect table name casing");

			alterTableName(tableName, expectedTableName + "_temp");

			alterTableName(expectedTableName + "_temp", expectedTableName);
		}

		Map<String, List<String>> columnDefinitionsMap =
			DBResourceUtil.getServiceComponentPortalColumnDefinitionsMap(
				connection);

		if (columnDefinitionsMap.isEmpty()) {
			return;
		}

		columnDefinitionsMap.putAll(
			DBResourceUtil.getServiceComponentModuleColumnDefinitionsMap(
				connection));

		DatabaseMetaData databaseMetaData = connection.getMetaData();

		Map<String, Map<String, String>> columnsMap = new TreeMap<>();

		try (ResultSet resultSet = databaseMetaData.getColumns(
				dbInspector.getCatalog(), dbInspector.getSchema(), "%", "%")) {

			while (resultSet.next()) {
				String tableName = resultSet.getString("TABLE_NAME");
				String columnName = resultSet.getString("COLUMN_NAME");

				Map<String, String> columns = columnsMap.computeIfAbsent(
					tableName,
					k -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));

				columns.put(columnName, columnName);
			}
		}

		for (Map.Entry<String, List<String>> entry :
				columnDefinitionsMap.entrySet()) {

			String tableName = entry.getKey();
			List<String> columnDefinitions = entry.getValue();

			_validateColumnNamesCasing(
				tableName, columnDefinitions, columnsMap);
		}
	}

	private void _validateColumnNamesCasing(
			String tableName, List<String> columnDefinitions,
			Map<String, Map<String, String>> columnsMap)
		throws Exception {

		Map<String, String> columnNames = columnsMap.get(tableName);

		DB db = DBManagerUtil.getDB();

		for (String columnDefinition : columnDefinitions) {
			if (Validator.isNull(columnDefinition)) {
				continue;
			}

			String expectedColumnName =
				StringUtil.split(columnDefinition, StringPool.SPACE)[0];

			String columnName = columnNames.get(expectedColumnName);

			if ((columnName == null) || columnName.equals(expectedColumnName)) {
				continue;
			}

			DataCleanupLoggingUtil.logAlter(
				_log, tableName,
				"incorrect column name casing, column: " + columnName);

			int index = columnDefinition.indexOf(StringPool.SPACE);

			String columnDataType =
				(index != -1) ? columnDefinition.substring(index + 1) : "";

			String tempColumnDefinition =
				expectedColumnName + "_temp " + columnDataType;

			db.alterColumnName(
				connection, tableName, columnName, tempColumnDefinition);

			db.alterColumnName(
				connection, tableName, expectedColumnName + "_temp",
				columnDefinition);
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DatabaseCasingDataCleanupPreupgradeProcess.class);

}