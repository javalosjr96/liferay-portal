/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.upgrade;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.db.DBType;
import com.liferay.portal.kernel.dao.db.DBTypeToSQLMap;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.ListUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Luis Ortiz Fuentes
 */
public class UpgradeProcessFactory {

	public static UpgradeProcess addColumns(
		String tableName, String... columnDefinitions) {

		return new DescribableUpgradeProcess(
			HashMapBuilder.<String, Object>put(
				"columnDefinitions", ListUtil.fromArray(columnDefinitions)
			).put(
				"tableName", tableName
			).build(),
			"addColumns",
			_getUpgradeInfo(
				tableName,
				"add the columns " + Arrays.toString(columnDefinitions))) {

			@Override
			protected void doUpgrade() throws Exception {
				for (String columnDefinition : columnDefinitions) {
					alterTableAddColumn(
						tableName,
						columnDefinition.substring(
							0, columnDefinition.indexOf(StringPool.SPACE)),
						columnDefinition.substring(
							columnDefinition.indexOf(StringPool.SPACE) + 1));
				}
			}

		};
	}

	public static UpgradeProcess alterColumnName(
		String tableName, String oldColumnName, String newColumnName) {

		return new DescribableUpgradeProcess(
			HashMapBuilder.<String, Object>put(
				"newColumnName", newColumnName
			).put(
				"oldColumnName", oldColumnName
			).put(
				"tableName", tableName
			).build(),
			"alterColumnName",
			_getUpgradeInfo(
				tableName,
				StringBundler.concat(
					"alter the name of the column ", oldColumnName, " to ",
					newColumnName))) {

			@Override
			protected void doUpgrade() throws Exception {
				alterColumnName(tableName, oldColumnName, newColumnName);
			}

		};
	}

	public static UpgradeProcess alterColumnType(
		String tableName, String columnName, String newColumnType) {

		return new DescribableUpgradeProcess(
			HashMapBuilder.<String, Object>put(
				"columnName", columnName
			).put(
				"newColumnType", newColumnType
			).put(
				"tableName", tableName
			).build(),
			"alterColumnType",
			_getUpgradeInfo(
				tableName,
				StringBundler.concat(
					"alter the type of the column ", columnName, " to ",
					newColumnType))) {

			@Override
			protected void doUpgrade() throws Exception {
				alterColumnType(tableName, columnName, newColumnType);
			}

		};
	}

	public static UpgradeProcess dropColumns(
		String tableName, String... columnNames) {

		return new DescribableUpgradeProcess(
			HashMapBuilder.<String, Object>put(
				"columnNames", ListUtil.fromArray(columnNames)
			).put(
				"tableName", tableName
			).build(),
			"dropColumns",
			_getUpgradeInfo(
				tableName,
				"drop the columns " + Arrays.toString(columnNames))) {

			@Override
			protected void doUpgrade() throws Exception {
				for (String columnName : columnNames) {
					alterTableDropColumn(tableName, columnName);
				}
			}

		};
	}

	public static UpgradeProcess dropTables(String... tableNames) {
		return new DescribableUpgradeProcess(
			HashMapBuilder.<String, Object>put(
				"tableNames", ListUtil.fromArray(tableNames)
			).build(),
			"dropTables",
			_getUpgradeInfo("drop tables " + Arrays.toString(tableNames))) {

			@Override
			protected void doUpgrade() throws Exception {
				for (String tableName : tableNames) {
					dropTable(tableName);
				}
			}

		};
	}

	public static UpgradeProcess runSQL(DBTypeToSQLMap... dbTypeToSQLMaps) {
		return new DescribableUpgradeProcess(
			HashMapBuilder.<String, Object>put(
				"dbTypeToSQLMaps", _getSQLs(dbTypeToSQLMaps)
			).build(),
			"runSQL", _getUpgradeInfo("run SQL updates")) {

			@Override
			protected void doUpgrade() throws Exception {
				for (DBTypeToSQLMap dbTypeToSQLMap : dbTypeToSQLMaps) {
					runSQL(dbTypeToSQLMap);
				}
			}

		};
	}

	public static UpgradeProcess runSQL(String... sqls) {
		return new DescribableUpgradeProcess(
			HashMapBuilder.<String, Object>put(
				"sqls", ListUtil.fromArray(sqls)
			).build(),
			"runSQL", _getUpgradeInfo("run SQL updates")) {

			@Override
			protected void doUpgrade() throws Exception {
				for (String sql : sqls) {
					runSQL(sql);
				}
			}

		};
	}

	private static String _getCaller() {
		Thread thread = Thread.currentThread();

		for (StackTraceElement stackTraceElement : thread.getStackTrace()) {
			String className = stackTraceElement.getClassName();

			if (!className.equals(Thread.class.getName()) &&
				!className.equals(UpgradeProcessFactory.class.getName())) {

				return className + ":" + stackTraceElement.getLineNumber();
			}
		}

		return UpgradeProcessFactory.class.getName();
	}

	private static List<Map<String, String>> _getSQLs(
		DBTypeToSQLMap[] dbTypeToSQLMaps) {

		List<Map<String, String>> sqls = new ArrayList<>();

		for (DBTypeToSQLMap dbTypeToSQLMap : dbTypeToSQLMaps) {
			Map<String, String> dbTypeSQLs = HashMapBuilder.put(
				"default", dbTypeToSQLMap.getDefaultSQL()
			).build();

			Map<DBType, String> explicitSQLs = dbTypeToSQLMap.getSQLs();

			for (Map.Entry<DBType, String> entry : explicitSQLs.entrySet()) {
				DBType dbType = entry.getKey();

				dbTypeSQLs.put(dbType.getName(), entry.getValue());
			}

			sqls.add(dbTypeSQLs);
		}

		return sqls;
	}

	private static String _getUpgradeInfo(String message) {
		return _getUpgradeInfo(null, "Modifying schema to " + message);
	}

	private static String _getUpgradeInfo(String tableName, String message) {
		String caller = _getCaller();

		if (tableName == null) {
			return caller + " - " + message;
		}

		return StringBundler.concat(
			caller, " - Modifying table ", tableName, " to ", message);
	}

	private abstract static class DescribableUpgradeProcess
		extends UpgradeProcess implements DescribableUpgradeStep {

		public DescribableUpgradeProcess(
			Map<String, Object> arguments, String operation,
			String upgradeInfo) {

			super(upgradeInfo);

			_arguments = Collections.unmodifiableMap(arguments);
			_operation = operation;
		}

		@Override
		public Map<String, Object> getArguments() {
			return _arguments;
		}

		@Override
		public String getOperation() {
			return _operation;
		}

		private final Map<String, Object> _arguments;
		private final String _operation;

	}

}