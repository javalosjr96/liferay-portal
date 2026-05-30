/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.upgrade.data.cleanup;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.db.DBInspector;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.db.DBType;
import com.liferay.portal.kernel.dao.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.upgrade.data.cleanup.DataCleanupPreupgradeProcess;
import com.liferay.portal.kernel.upgrade.data.cleanup.FilterableAllTablesOrphanReferencesDataCleanupPreupgradeProcess;
import com.liferay.portal.kernel.upgrade.data.cleanup.TableOrphanReferencesDataCleanupPreupgradeProcess;
import com.liferay.portal.kernel.upgrade.data.cleanup.util.DataCleanupLoggingUtil;
import com.liferay.portal.kernel.util.LinkedHashMapBuilder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Luis Ortiz
 */
public class DDMDataCleanupPreupgradeProcess
	extends DataCleanupPreupgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		DataCleanupPreupgradeProcess ddmStructureDataCleanupPreupgradeProcess =
			_getDDMStructureDataCleanupPreupgradeProcess();
		DataCleanupPreupgradeProcess
			ddmStructureVersionDataCleanupPreupgradeProcess =
				_getDDMStructureVersionDataCleanupPreupgradeProcess();
		DataCleanupPreupgradeProcess
			journalPointingOrphanDDMStructureCleanupPreupgradeProcess70to73 =
				_getJournalPointingOrphanDDMStructureCleanupPreupgradeProcess70to73();

		Map<DataCleanupPreupgradeProcess, List<DataCleanupPreupgradeProcess>>
			dataCleanupPreupgradeProcessesMap =
				LinkedHashMapBuilder.
					<DataCleanupPreupgradeProcess,
					 List<DataCleanupPreupgradeProcess>>put(
						_getDDMFieldDataCleanupPreupgradeProcess(),
						dependsOn(
							ddmStructureVersionDataCleanupPreupgradeProcess)
					).put(
						_getDDMFormInstanceDataCleanupPreupgradeProcess(),
						dependsOn(ddmStructureDataCleanupPreupgradeProcess)
					).put(
						ddmStructureDataCleanupPreupgradeProcess, dependsOn()
					).put(
						ddmStructureVersionDataCleanupPreupgradeProcess,
						dependsOn(ddmStructureDataCleanupPreupgradeProcess)
					).put(
						_getDDMTemplateDataCleanupPreupgradeProcess(),
						dependsOn(ddmStructureDataCleanupPreupgradeProcess)
					).put(
						_getJournalPointingOrphanDDMStructureCleanupPreupgradeProcess62(),
						dependsOn(ddmStructureDataCleanupPreupgradeProcess)
					).put(
						journalPointingOrphanDDMStructureCleanupPreupgradeProcess70to73,
						dependsOn(ddmStructureDataCleanupPreupgradeProcess)
					).put(
						_getJournalPointingOrphanDDMStructureCleanupPreupgradeProcess74(),
						dependsOn(ddmStructureDataCleanupPreupgradeProcess)
					).put(
						_getJournalPointingOrphanNonancestorDDMStructureCleanupPreupgradeProcess70to73(),
						dependsOn(
							journalPointingOrphanDDMStructureCleanupPreupgradeProcess70to73)
					).build();

		List<DataCleanupPreupgradeProcess> dataCleanupPreupgradeProcesses =
			getSortedDataCleanupPreupgradeProcesses(
				dataCleanupPreupgradeProcessesMap);

		for (DataCleanupPreupgradeProcess dataCleanupPreupgradeProcess :
				dataCleanupPreupgradeProcesses) {

			dataCleanupPreupgradeProcess.upgrade();
		}
	}

	private DataCleanupPreupgradeProcess
		_getDDMFieldDataCleanupPreupgradeProcess() {

		return new DataCleanupPreupgradeProcess() {

			@Override
			protected void doUpgrade() throws Exception {
				DBInspector dbInspector = new DBInspector(connection);

				String ddmFieldAttributeTableName = dbInspector.normalizeName(
					"DDMFieldAttribute");
				String ddmFieldTableName = dbInspector.normalizeName("DDMField");

				if (!dbInspector.hasTable(ddmFieldAttributeTableName) ||
					!dbInspector.hasTable(ddmFieldTableName)) {

					return;
				}

				List<Long> orphanIds = new ArrayList<>();

				try (PreparedStatement preparedStatement =
						connection.prepareStatement(
							StringBundler.concat(
								"select distinct fieldId from ",
								ddmFieldAttributeTableName,
								" where fieldId != 0 and fieldId is not null",
								" and fieldId not in (select fieldId from ",
								ddmFieldTableName, ")"));
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
									"delete from ", ddmFieldAttributeTableName,
									" where fieldId in (",
									String.join(
										StringPool.COMMA_AND_SPACE, batchStrings),
									")"))) {

						int deleted = preparedStatement.executeUpdate();

						if (deleted > 0) {
							DataCleanupLoggingUtil.logDelete(
								_log, deleted, ddmFieldAttributeTableName,
								StringBundler.concat(
									"fieldId was not found in fieldId from table ",
									ddmFieldTableName));
						}
					}
				}
			}

		};
	}

	private DataCleanupPreupgradeProcess
		_getDDMFormInstanceDataCleanupPreupgradeProcess() {

		return new DataCleanupPreupgradeProcess(
			new TableOrphanReferencesDataCleanupPreupgradeProcess(
				null, null, "formInstanceId", "DDMFormInstanceRecord",
				"formInstanceId", "DDMFormInstance"),
			new TableOrphanReferencesDataCleanupPreupgradeProcess(
				null, null, "formInstanceId", "DDMFormInstanceRecordVersion",
				"formInstanceId", "DDMFormInstance"),
			new TableOrphanReferencesDataCleanupPreupgradeProcess(
				null, null, "formInstanceId", "DDMFormInstanceReport",
				"formInstanceId", "DDMFormInstance"),
			new TableOrphanReferencesDataCleanupPreupgradeProcess(
				null, null, "formInstanceId", "DDMFormInstanceReportVersion",
				"formInstanceId", "DDMFormInstance"));
	}

	private DataCleanupPreupgradeProcess
		_getDDMStructureDataCleanupPreupgradeProcess() {

		return new DataCleanupPreupgradeProcess(
			new FilterableAllTablesOrphanReferencesDataCleanupPreupgradeProcess(
				StringBundler.concat(
					"[$SOURCE_TABLE_ALIAS$].classNameId = (select classNameId ",
					"from ClassName_ where value = 'com.liferay.dynamic.data.",
					"mapping.model.DDMStructure')"),
				new String[] {"classNameId"}, "classPK",
				new String[] {"structureId"}, "DDMStructure"),
			new TableOrphanReferencesDataCleanupPreupgradeProcess(
				null, null, "structureId", "DDMFormInstance", "structureId",
				"DDMStructure"),
			new TableOrphanReferencesDataCleanupPreupgradeProcess(
				null, null, "structureId", "DDMStorageLink", "structureId",
				"DDMStructure"),
			new TableOrphanReferencesDataCleanupPreupgradeProcess(
				null, null, "structureId", "DDMStructureLink", "structureId",
				"DDMStructure"),
			new TableOrphanReferencesDataCleanupPreupgradeProcess(
				null, null, "structureId", "DDMStructureVersion", "structureId",
				"DDMStructure"));
	}

	private DataCleanupPreupgradeProcess
		_getDDMStructureVersionDataCleanupPreupgradeProcess() {

		return new DataCleanupPreupgradeProcess() {

			@Override
			protected void doUpgrade() throws Exception {
				_cleanUpOrphans(
					"structureVersionId", "DDMField",
					"structureVersionId", "DDMStructureVersion");
				_cleanUpOrphans(
					"structureVersionId", "DDMStructureLayout",
					"structureVersionId", "DDMStructureVersion");
			}

			private void _cleanUpOrphans(
					String sourceColumnName, String sourceTableName,
					String targetColumnName, String targetTableName)
				throws Exception {

				DBInspector dbInspector = new DBInspector(connection);

				String normalizedSourceTableName = dbInspector.normalizeName(
					sourceTableName);
				String normalizedTargetTableName = dbInspector.normalizeName(
					targetTableName);

				if (!dbInspector.hasTable(normalizedSourceTableName) ||
					!dbInspector.hasTable(normalizedTargetTableName)) {

					return;
				}

				List<Long> orphanIds = new ArrayList<>();

				try (PreparedStatement preparedStatement =
						connection.prepareStatement(
							StringBundler.concat(
								"select distinct ", sourceColumnName, " from ",
								normalizedSourceTableName, " where ",
								sourceColumnName, " != 0 and ", sourceColumnName,
								" is not null and ", sourceColumnName,
								" not in (select ", targetColumnName, " from ",
								normalizedTargetTableName, ")"));
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
									"delete from ", normalizedSourceTableName,
									" where ", sourceColumnName, " in (",
									String.join(
										StringPool.COMMA_AND_SPACE, batchStrings),
									")"))) {

						int deleted = preparedStatement.executeUpdate();

						if (deleted > 0) {
							DataCleanupLoggingUtil.logDelete(
								_log, deleted, normalizedSourceTableName,
								StringBundler.concat(
									sourceColumnName,
									" was not found in ", targetColumnName,
									" from table ", normalizedTargetTableName));
						}
					}
				}
			}

		};
	}

	private DataCleanupPreupgradeProcess
		_getDDMTemplateDataCleanupPreupgradeProcess() {

		return new DataCleanupPreupgradeProcess(
			new FilterableAllTablesOrphanReferencesDataCleanupPreupgradeProcess(
				StringBundler.concat(
					"[$SOURCE_TABLE_ALIAS$].classNameId = (select classNameId ",
					"from ClassName_ where value = 'com.liferay.dynamic.data.",
					"mapping.model.DDMTemplate')"),
				new String[] {"classNameId"}, "classPK",
				new String[] {"templateId"}, "DDMTemplate"),
			new TableOrphanReferencesDataCleanupPreupgradeProcess(
				null, null, "templateId", "DDMTemplateLink", "templateId",
				"DDMTemplate"),
			new TableOrphanReferencesDataCleanupPreupgradeProcess(
				null, null, "templateId", "DDMTemplateVersion", "templateId",
				"DDMTemplate"));
	}

	private DataCleanupPreupgradeProcess
		_getJournalPointingOrphanDDMStructureCleanupPreupgradeProcess62() {

		return new DataCleanupPreupgradeProcess(
			new TableOrphanReferencesDataCleanupPreupgradeProcess(
				null, null, "structureId", "JournalArticle", "structureKey",
				"DDMStructure"),
			new TableOrphanReferencesDataCleanupPreupgradeProcess(
				null, null, "structureId", "JournalFeed", "structureKey",
				"DDMStructure"));
	}

	private DataCleanupPreupgradeProcess
		_getJournalPointingOrphanDDMStructureCleanupPreupgradeProcess70to73() {

		return new DataCleanupPreupgradeProcess(
			new TableOrphanReferencesDataCleanupPreupgradeProcess(
				null, null, "DDMStructureKey", "JournalArticle", "structureKey",
				"DDMStructure"),
			new TableOrphanReferencesDataCleanupPreupgradeProcess(
				null, null, "DDMStructureKey", "JournalFeed", "structureKey",
				"DDMStructure"));
	}

	private DataCleanupPreupgradeProcess
		_getJournalPointingOrphanDDMStructureCleanupPreupgradeProcess74() {

		return new DataCleanupPreupgradeProcess(
			new TableOrphanReferencesDataCleanupPreupgradeProcess(
				null, null, "DDMStructureId", "JournalArticle", "structureId",
				"DDMStructure"),
			new TableOrphanReferencesDataCleanupPreupgradeProcess(
				null, null, "DDMStructureId", "JournalFeed", "structureId",
				"DDMStructure"));
	}

	private DataCleanupPreupgradeProcess
		_getJournalPointingOrphanNonancestorDDMStructureCleanupPreupgradeProcess70to73() {

		return new DataCleanupPreupgradeProcess() {

			@Override
			protected void doUpgrade() throws Exception {
				DBInspector dbInspector = new DBInspector(connection);

				if (!dbInspector.hasColumn(
						"JournalArticle", "DDMStructureKey")) {

					return;
				}

				Map<Long, Long> parentGroupIds = new HashMap<>();

				try (PreparedStatement preparedStatement =
						connection.prepareStatement(
							"select groupId, parentGroupId from Group_ where " +
								"parentGroupId != 0");

					ResultSet resultSet = preparedStatement.executeQuery()) {

					while (resultSet.next()) {
						parentGroupIds.put(
							resultSet.getLong("groupId"),
							resultSet.getLong("parentGroupId"));
					}
				}

				Map<Long, Set<String>> structureKeysMap = new HashMap<>();

				try (PreparedStatement preparedStatement =
						connection.prepareStatement(
							"select groupId, structureKey from DDMStructure");

					ResultSet resultSet = preparedStatement.executeQuery()) {

					while (resultSet.next()) {
						Set<String> structureKeys =
							structureKeysMap.computeIfAbsent(
								resultSet.getLong("groupId"),
								key -> new HashSet<>());

						structureKeys.add(
							resultSet.getString("structureKey"));
					}
				}

				String sql = null;

				if ((DBManagerUtil.getDBType() == DBType.MARIADB) ||
					(DBManagerUtil.getDBType() == DBType.MYSQL)) {

					sql = StringBundler.concat(
						"select JournalArticle.groupId, ",
						"JournalArticle.DDMStructureKey, count(1) from ",
						"JournalArticle left join DDMStructure DDMStructure1 ",
						"on DDMStructure1.structureKey = ",
						"JournalArticle.DDMStructureKey and ",
						"DDMStructure1.groupId = JournalArticle.groupId left ",
						"join Group_ on Group_.friendlyURL = ? and ",
						"Group_.companyId = JournalArticle.companyId left ",
						"join DDMStructure DDMStructure2 on ",
						"DDMStructure2.structureKey = ",
						"JournalArticle.DDMStructureKey and ",
						"DDMStructure2.groupId = Group_.groupId where ",
						"JournalArticle.DDMStructureKey is not null and ",
						"DDMStructure1.structureKey is null and ",
						"DDMStructure2.structureKey is null group by ",
						"JournalArticle.groupId, ",
						"JournalArticle.DDMStructureKey");
				}
				else {
					sql = StringBundler.concat(
						"select groupId, DDMStructureKey, count(1) from ",
						"JournalArticle where DDMStructureKey is not null and ",
						"not exists (select 1 from DDMStructure where ",
						"DDMStructure.structureKey = ",
						"JournalArticle.DDMStructureKey and ",
						"(DDMStructure.groupId = JournalArticle.groupId or ",
						"DDMStructure.groupId in (select groupId from Group_ ",
						"where friendlyURL = ? and companyId = ",
						"JournalArticle.companyId))) group by groupId, ",
						"DDMStructureKey");
				}

				try (PreparedStatement preparedStatement1 =
						connection.prepareStatement(sql);
					PreparedStatement preparedStatement2 =
						AutoBatchPreparedStatementUtil.autoBatch(
							connection,
							"delete from JournalArticle where groupId = ? " +
								"and DDMStructureKey = ?")) {

					preparedStatement1.setString(
						1, GroupConstants.GLOBAL_FRIENDLY_URL);

					try (ResultSet resultSet =
							preparedStatement1.executeQuery()) {

						while (resultSet.next()) {
							long groupId = resultSet.getLong("groupId");

							String structureKey = resultSet.getString(
								"DDMStructureKey");

							if (_hasStructure(
									groupId, parentGroupIds, structureKey,
									structureKeysMap)) {

								continue;
							}

							if (_log.isInfoEnabled()) {
								DataCleanupLoggingUtil.logDelete(
									_log, resultSet.getLong(3),
									dbInspector.normalizeName("JournalArticle"),
									StringBundler.concat(
										dbInspector.normalizeName(
											"DDMStructureKey"),
										StringPool.SPACE, structureKey,
										" was not found in ",
										dbInspector.normalizeName("groupId"),
										StringPool.SPACE, groupId,
										" or its ancestors"));
							}

							preparedStatement2.setLong(1, groupId);
							preparedStatement2.setString(2, structureKey);

							preparedStatement2.addBatch();
						}

						preparedStatement2.executeBatch();
					}
				}
			}

			private boolean _hasStructure(
				long groupId, Map<Long, Long> parentGroupIds,
				String structureKey, Map<Long, Set<String>> structureKeysMap) {

				while (true) {
					Set<String> groupStructureKeys = structureKeysMap.get(
						groupId);

					if ((groupStructureKeys != null) &&
						groupStructureKeys.contains(structureKey)) {

						return true;
					}

					Long parentGroupId = parentGroupIds.get(groupId);

					if (parentGroupId == null) {
						break;
					}

					groupId = parentGroupId;
				}

				return false;
			}

		};
	}

	private static final int _BATCH_SIZE = 1000;

	private static final Log _log = LogFactoryUtil.getLog(
		DDMDataCleanupPreupgradeProcess.class);

}