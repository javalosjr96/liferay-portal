/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.upgrade.data.cleanup;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.dao.db.DBInspector;
import com.liferay.portal.kernel.db.DBResourceUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.upgrade.data.cleanup.DataCleanupPreupgradeProcess;
import com.liferay.portal.kernel.upgrade.data.cleanup.TableOrphanReferencesDataCleanupPreupgradeProcess;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.security.permission.ResourceActionsImpl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Luis Ortiz
 */
public class ResourcePermissionDataCleanupPreupgradeProcess
	extends DataCleanupPreupgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		DBInspector dbInspector = new DBInspector(connection);

		if (!dbInspector.hasTable("ResourcePermission") ||
			!dbInspector.hasColumn("ResourcePermission", "primKeyId")) {

			return;
		}

		Set<String> liferayTableNames = DBResourceUtil.getLiferayTableNames(
			connection);

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				"select distinct name from ResourcePermission where name " +
					"like 'com.liferay.%' and primKeyId != 0 and primKeyId " +
						"is not null and scope = ?")) {

			preparedStatement.setLong(1, ResourceConstants.SCOPE_INDIVIDUAL);

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				ResourceActionsImpl resourceActionsImpl =
					new ResourceActionsImpl();

				String compositeModelNameSeparatorString =
					resourceActionsImpl.getCompositeModelNameSeparator();

				Map<String, List<String>> tableNames = new HashMap<>();

				while (resultSet.next()) {
					String name = resultSet.getString("name");

					String[] classNames = StringUtil.split(
						name, compositeModelNameSeparatorString);

					String tableName = null;

					if (classNames.length == 1) {
						tableName =
							DataCleanupPreupgradeProcessUtil.getTableName(
								connection, dbInspector, classNames[0]);
					}
					else {
						for (String className : classNames) {
							tableName =
								DataCleanupPreupgradeProcessUtil.getTableName(
									connection, dbInspector, className);

							if (StringUtil.startsWith(tableName, "DDM")) {
								break;
							}

							tableName = null;
						}
					}

					if ((tableName == null) ||
						(!dbInspector.isObjectTable(tableName) &&
						 !liferayTableNames.contains(tableName))) {

						if (_log.isDebugEnabled()) {
							_log.debug(
								StringBundler.concat(
									"Skipping class name ", name,
									" because its associated table was not ",
									"found or it does not belong to Liferay"));
						}

						continue;
					}

					if (!dbInspector.hasTable(tableName)) {
						if (_log.isWarnEnabled()) {
							_log.warn(
								"Table \"" + tableName + "\" does not exist");
						}

						continue;
					}

					List<String> names = tableNames.computeIfAbsent(
						tableName, key -> new ArrayList<>());

					names.add(name);
				}

				List<DataCleanupPreupgradeProcess> tableCleanupProcesses =
					new ArrayList<>();

				for (Map.Entry<String, List<String>> entry :
						tableNames.entrySet()) {

					String tableName = entry.getKey();

					String primaryKeyColumnName = "resourcePrimKey";

					if (!dbInspector.hasColumn(
							tableName, primaryKeyColumnName)) {

						primaryKeyColumnName =
							DataCleanupPreupgradeProcessUtil.
								getPrimaryKeyColumnName(
									connection, dbInspector, tableName);
					}

					if (primaryKeyColumnName == null) {
						if (_log.isWarnEnabled()) {
							_log.warn(
								"Table \"" + tableName +
									"\" has no primary key column");
						}

						continue;
					}

					List<String> names = entry.getValue();

					String additionalWhereClause = null;

					if (names.size() == 1) {
						additionalWhereClause = StringBundler.concat(
							"[$SOURCE_TABLE_ALIAS$].scope = ",
							ResourceConstants.SCOPE_INDIVIDUAL,
							" and [$SOURCE_TABLE_ALIAS$].name = '",
							names.get(0), "'");
					}
					else {
						StringBundler joinedNamesSB = new StringBundler(
							(names.size() * 2) - 1);

						for (int i = 0; i < names.size(); i++) {
							if (i > 0) {
								joinedNamesSB.append(", ");
							}

							joinedNamesSB.append("'");
							joinedNamesSB.append(names.get(i));
							joinedNamesSB.append("'");
						}

						additionalWhereClause = StringBundler.concat(
							"[$SOURCE_TABLE_ALIAS$].scope = ",
							ResourceConstants.SCOPE_INDIVIDUAL,
							" and [$SOURCE_TABLE_ALIAS$].name in (",
							joinedNamesSB, ")");
					}

					tableCleanupProcesses.add(
						new TableOrphanReferencesDataCleanupPreupgradeProcess(
							null, additionalWhereClause, "primKeyId",
							"ResourcePermission", primaryKeyColumnName,
							tableName));
				}

				for (DataCleanupPreupgradeProcess tableCleanupProcess :
						tableCleanupProcesses) {

					upgrade(tableCleanupProcess);
				}
			}
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ResourcePermissionDataCleanupPreupgradeProcess.class);

}