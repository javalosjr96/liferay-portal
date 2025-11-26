/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.upgrade.data.cleanup;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.db.DBResourceUtil;
import com.liferay.portal.kernel.dao.db.DBInspector;
import com.liferay.portal.kernel.upgrade.UpgradeException;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.upgrade.UpgradeProcessFactory;
import com.liferay.portal.kernel.upgrade.data.cleanup.DataCleanupPreupgradeProcess;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;

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

		Map<String, String> actualTableNames = new TreeMap<>(
			String.CASE_INSENSITIVE_ORDER);

		for (String tableName : dbInspector.getTableNames(null)) {
			actualTableNames.put(tableName, tableName);
		}

		_validateTableNamesCasing(expectedTableNames, actualTableNames);

		Map<String, List<String>> expectedColumnDefinitionsMap =
			DBResourceUtil.getServiceComponentPortalColumnDefinitionsMap(
				connection);

		if (expectedColumnDefinitionsMap.isEmpty()) {
			return;
		}

		expectedColumnDefinitionsMap.putAll(
			DBResourceUtil.getServiceComponentModuleColumnDefinitionsMap(
				connection));

		DatabaseMetaData metaData = connection.getMetaData();

		Map<String, Map<String, String>> actualColumnsMap = new TreeMap<>();

		try (ResultSet resultSet = metaData.getColumns(
				dbInspector.getCatalog(), dbInspector.getSchema(), "%", "%")) {

			while (resultSet.next()) {
				String tableName = resultSet.getString("TABLE_NAME");
				String columnName = resultSet.getString("COLUMN_NAME");

				Map<String, String> columns = actualColumnsMap.computeIfAbsent(
					tableName,
					k -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));

				columns.put(columnName, columnName);
			}
		}

		_validateColumnNamesCasing(
			expectedColumnDefinitionsMap, actualColumnsMap);
	}

	private void _validateColumnNamesCasing(
		Map<String, List<String>> expectedColumnDefinitionsMap,
		Map<String, Map<String, String>> actualColumnsMap) {

		expectedColumnDefinitionsMap.forEach(
			(tableName, values) -> {
				Map<String, String> columnNames = actualColumnsMap.get(
					tableName);

				Map<String, String> invalidColumnCasingMap =
					new ConcurrentSkipListMap<>();

				for (String columnDefinition : values) {
					if (Validator.isNull(columnDefinition)) {
						continue;
					}

					String expectedColumnName =
						StringUtil.split(columnDefinition, StringPool.SPACE)[0];

					String badColumnName = columnNames.get(expectedColumnName);

					if (badColumnName == null) {
						continue;
					}

					if (!badColumnName.equals(expectedColumnName)) {
						invalidColumnCasingMap.put(
							badColumnName, columnDefinition);

						break;
					}
				}

				invalidColumnCasingMap.forEach(
					(actualColumnName, expectedColumnDefinition) -> {
						UpgradeProcess alterColumnUpgradeProcess =
							UpgradeProcessFactory.alterColumnName(
								tableName, actualColumnName,
								expectedColumnDefinition);

						try {
							alterColumnUpgradeProcess.upgrade();
						}
						catch (UpgradeException upgradeException) {
							throw new RuntimeException(upgradeException);
						}
					});
			});
	}

	private void _validateTableNamesCasing(
			Set<String> expectedTableNames,
			Map<String, String> existingDatabaseTableNames)
		throws Exception {

		DBInspector dbInspector = new DBInspector(connection);

		for (String expectedTableName : expectedTableNames) {
			expectedTableName = dbInspector.normalizeName(expectedTableName);

			String actualTableName = existingDatabaseTableNames.get(
				expectedTableName);

			if (actualTableName == null) {
				continue;
			}

			if (!actualTableName.equals(expectedTableName)) {
				System.out.println(
					StringBundler.concat(
						"Incorrect Casing Found. Actual: ", actualTableName,
						" Expected: ", expectedTableName,
						" Table Name will be altered"));

				alterTableName(actualTableName, expectedTableName);
			}
		}
	}

}