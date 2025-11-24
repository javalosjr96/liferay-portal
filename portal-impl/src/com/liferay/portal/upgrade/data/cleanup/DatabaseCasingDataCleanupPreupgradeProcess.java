/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.upgrade.data.cleanup;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.db.DBResourceUtil;
import com.liferay.portal.kernel.dao.db.DBInspector;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.db.DBType;
import com.liferay.portal.kernel.upgrade.UpgradeException;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.upgrade.UpgradeProcessFactory;
import com.liferay.portal.kernel.upgrade.data.cleanup.DataCleanupPreupgradeProcess;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.UnaryOperator;

/**
 * @author Jorge Avalos
 */
public class DatabaseCasingDataCleanupPreupgradeProcess
	extends DataCleanupPreupgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		Set<String> expectedTableNames = new HashSet<>();

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

		Map<String, Set<String>> actualColumnsMap = new TreeMap<>();

		try (ResultSet resultSet = metaData.getColumns(
				dbInspector.getCatalog(), dbInspector.getSchema(), "%", "%")) {

			while (resultSet.next()) {
				String tableName = resultSet.getString("TABLE_NAME");
				String columnName = resultSet.getString("COLUMN_NAME");

				Set<String> columns = actualColumnsMap.computeIfAbsent(
					tableName, k -> new TreeSet<>());

				columns.add(columnName);
			}
		}

		_validateColumnNamesCasing(
			expectedColumnDefinitionsMap, actualColumnsMap);
	}

	private TreeSet<String> _caseColumnNameList(List<String> columnsNamesList)
		throws Exception {
		if (ListUtil.isEmpty(columnsNamesList)) {
			return new TreeSet<>();
		}

		TreeSet<String> casedColumnNames = new TreeSet<>();

		UnaryOperator<String> casingStrategy = _getCasingStrategy(connection);

		for (String line : columnsNamesList) {
			if (Validator.isNotNull(line)) {
				casedColumnNames.add(casingStrategy.apply(line));
			}
		}

		return casedColumnNames;
	}

	private UnaryOperator<String> _getCasingStrategy(Connection connection)
		throws Exception {
		DBType dbType = DBManagerUtil.getDBType();

		if (dbType == DBType.POSTGRESQL) {
			return String::toLowerCase;
		}
		else if ((dbType == DBType.ORACLE) || (dbType == DBType.DB2)) {
			return String::toUpperCase;
		}
		else if ((dbType == DBType.MYSQL) || (dbType == DBType.MARIADB)) {
			return _getMySQLCasingVariable(connection);
		}

		return UnaryOperator.identity();
	}

	private UnaryOperator<String> _getMySQLCasingVariable(Connection connection)
		throws Exception {

		String sql = "SHOW VARIABLES LIKE 'lower_case_table_names'";

		try (Statement stmt = connection.createStatement();
			ResultSet resultSet = stmt.executeQuery(sql)) {

			if (resultSet.next() && (resultSet.getInt(2) == 1)) {
				return String::toLowerCase;
			}
		}

		return UnaryOperator.identity();
	}

	private void _validateColumnNamesCasing(
		Map<String, List<String>> expectedColumnDefinitionsMap,
		Map<String, Set<String>> actualColumnsMap) {

		expectedColumnDefinitionsMap.forEach(
			(tableName, values) -> {
				TreeSet<String> expectedColumnDefinitions = null;

				try {
					expectedColumnDefinitions = _caseColumnNameList(
						values);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}

				Set<String> columnNames = actualColumnsMap.get(tableName);

				Map<String, String> invalidColumnCasingMap =
					new LinkedHashMap<>();

				for (String columnDefinition : expectedColumnDefinitions) {
					if (Validator.isNull(columnDefinition)) {
						continue;
					}

					int spaceIndex = columnDefinition.indexOf(StringPool.SPACE);

					String expectedColumnName =
						(spaceIndex == -1) ? columnDefinition :
							columnDefinition.substring(0, spaceIndex);

					for (String columnName : columnNames) {
						if (StringUtil.equalsIgnoreCase(
								columnName, expectedColumnName) &&
							!columnName.equals(expectedColumnName)) {

							invalidColumnCasingMap.put(
								columnName, columnDefinition);

							break;
						}
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
						catch (UpgradeException e) {
							throw new RuntimeException(e);
						}
					});
			});
	}

	private void _validateTableNamesCasing(
			Set<String> expectedTableNames,
			Map<String, String> existingDatabaseTableNames)
		throws Exception {

		UnaryOperator<String> casingStrategy = _getCasingStrategy(connection);

		for (String expectedTableName : expectedTableNames) {
			expectedTableName = casingStrategy.apply(expectedTableName);

			String actualTableName = existingDatabaseTableNames.get(
				expectedTableName);

			if (actualTableName == null) {
				continue;
			}

			if (StringUtil.equalsIgnoreCase(
					actualTableName, expectedTableName) &&
				!actualTableName.equals(expectedTableName)) {

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