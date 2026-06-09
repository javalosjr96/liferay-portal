/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.upgrade.data.cleanup;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBInspector;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.db.DBType;
import com.liferay.portal.kernel.db.DBResourceUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.upgrade.data.cleanup.DataCleanupPreupgradeProcess;
import com.liferay.portal.kernel.upgrade.data.cleanup.util.DataCleanupLoggingUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.security.permission.ResourceActionsImpl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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

		Map<String, List<String>> classNamesByTableName = new TreeMap<>();

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				"select distinct name from ResourcePermission where name " +
					"like 'com.liferay.%' and primKeyId != 0 and primKeyId " +
						"is not null and scope = ?")) {

			preparedStatement.setLong(1, ResourceConstants.SCOPE_INDIVIDUAL);

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				ResourceActionsImpl resourceActionsImpl =
					new ResourceActionsImpl();

				String compositeModelNameSeparator =
					resourceActionsImpl.getCompositeModelNameSeparator();

				while (resultSet.next()) {
					String name = resultSet.getString("name");

					String[] classNames = StringUtil.split(
						name, compositeModelNameSeparator);

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
							_log.warn("Table " + tableName + " does not exist");
						}

						continue;
					}

					List<String> names = classNamesByTableName.computeIfAbsent(
						tableName, key -> new ArrayList<>());

					names.add(name);
				}
			}
		}

		DB db = DBManagerUtil.getDB();

		DBType dbType = db.getDBType();

		for (Map.Entry<String, List<String>> entry :
				classNamesByTableName.entrySet()) {

			String tableName = entry.getKey();

			String primaryKeyColumnName = "resourcePrimKey";

			if (!dbInspector.hasColumn(tableName, primaryKeyColumnName)) {
				primaryKeyColumnName =
					DataCleanupPreupgradeProcessUtil.getPrimaryKeyColumnName(
						connection, dbInspector, tableName);
			}

			if (primaryKeyColumnName == null) {
				if (_log.isWarnEnabled()) {
					_log.warn(
						"Skipping table " + tableName +
							" because it does not have a primary key");
				}

				continue;
			}

			List<String> names = entry.getValue();

			String namesClause;

			if (names.size() == 1) {
				namesClause = StringBundler.concat(
					"scope = ", ResourceConstants.SCOPE_INDIVIDUAL,
					" and name = '", names.get(0), "'");
			}
			else {
				List<String> quotedNames = new ArrayList<>(names.size());

				for (String name : names) {
					quotedNames.add(StringBundler.concat("'", name, "'"));
				}

				namesClause = StringBundler.concat(
					"scope = ", ResourceConstants.SCOPE_INDIVIDUAL,
					" and name in (",
					String.join(StringPool.COMMA_AND_SPACE, quotedNames), ")");
			}

			String sql;

			if ((dbType == DBType.MARIADB) || (dbType == DBType.MYSQL)) {
				String aliasTableName = StringBundler.concat(
					tableName, StringPool.UNDERLINE, primaryKeyColumnName);

				sql = StringBundler.concat(
					"select distinct ResourcePermission.primKeyId from ",
					"ResourcePermission left join ", tableName,
					StringPool.SPACE, aliasTableName, " on ", aliasTableName,
					StringPool.PERIOD, primaryKeyColumnName,
					" = ResourcePermission.primKeyId where ",
					"ResourcePermission.primKeyId is not null and ",
					"ResourcePermission.primKeyId != 0 and ", namesClause,
					" and ", aliasTableName, StringPool.PERIOD,
					primaryKeyColumnName, " is null");
			}
			else {
				sql = StringBundler.concat(
					"select distinct ResourcePermission.primKeyId from ",
					"ResourcePermission where ResourcePermission.primKeyId is ",
					"not null and ResourcePermission.primKeyId != 0 and ",
					namesClause, " and not exists (select 1 from ", tableName,
					" where ", tableName, StringPool.PERIOD,
					primaryKeyColumnName, " = ResourcePermission.primKeyId)");
			}

			List<Long> orphanIds = new ArrayList<>();

			try (PreparedStatement preparedStatement =
					connection.prepareStatement(sql)) {

				try (ResultSet resultSet = preparedStatement.executeQuery()) {
					while (resultSet.next()) {
						orphanIds.add(resultSet.getLong("primKeyId"));
					}
				}
			}

			if (orphanIds.isEmpty()) {
				continue;
			}

			int totalDeleted = 0;

			for (int i = 0; i < orphanIds.size(); i += _BATCH_SIZE) {
				int end = Math.min(i + _BATCH_SIZE, orphanIds.size());

				List<String> batchIds = new ArrayList<>(end - i);

				for (int j = i; j < end; j++) {
					batchIds.add(String.valueOf(orphanIds.get(j)));
				}

				try (PreparedStatement preparedStatement =
						connection.prepareStatement(
							StringBundler.concat(
								"delete from ResourcePermission where ",
								"primKeyId in (",
								String.join(
									StringPool.COMMA_AND_SPACE, batchIds),
								") and ", namesClause))) {

					totalDeleted += preparedStatement.executeUpdate();
				}
			}

			DataCleanupLoggingUtil.logDelete(
				_log, totalDeleted, "ResourcePermission",
				StringBundler.concat(
					"primKeyId was not found in column ", primaryKeyColumnName,
					" from table \"", tableName, "\""));
		}
	}

	private static final int _BATCH_SIZE = 1000;

	private static final Log _log = LogFactoryUtil.getLog(
		ResourcePermissionDataCleanupPreupgradeProcess.class);

}