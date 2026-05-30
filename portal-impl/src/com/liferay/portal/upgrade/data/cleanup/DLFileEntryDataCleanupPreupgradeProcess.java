/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.upgrade.data.cleanup;

import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.document.library.kernel.model.DLFileEntryMetadata;
import com.liferay.document.library.kernel.model.DLFileShortcut;
import com.liferay.document.library.kernel.processor.RawMetadataProcessor;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.db.DBInspector;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.upgrade.data.cleanup.DataCleanupPreupgradeProcess;
import com.liferay.portal.kernel.upgrade.data.cleanup.FilterableAllTablesOrphanReferencesDataCleanupPreupgradeProcess;
import com.liferay.portal.kernel.upgrade.data.cleanup.TableOrphanReferencesDataCleanupPreupgradeProcess;
import com.liferay.portal.kernel.upgrade.data.cleanup.util.DataCleanupLoggingUtil;
import com.liferay.portal.kernel.util.LinkedHashMapBuilder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author István András Dézsi
 */
public class DLFileEntryDataCleanupPreupgradeProcess
	extends DataCleanupPreupgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		DataCleanupPreupgradeProcess dlFileEntryDataCleanupPreupgradeProcess =
			_getDLFileEntryDataCleanupPreupgradeProcess();
		DataCleanupPreupgradeProcess
			dlFileEntryEmptyNameDataCleanupPreupgradeProcess =
				_getDLFileEntryEmptyNameDataCleanupPreupgradeProcess();
		DataCleanupPreupgradeProcess
			dlFileEntryPointingDLFileVersionDataCleanupPreupgradeProcess =
				_getDLFileEntryPointingDLFileVersionDataCleanupPreupgradeProcess();

		Map<DataCleanupPreupgradeProcess, List<DataCleanupPreupgradeProcess>>
			dataCleanupPreupgradeProcessesMap =
				LinkedHashMapBuilder.
					<DataCleanupPreupgradeProcess,
					 List<DataCleanupPreupgradeProcess>>put(
						dlFileEntryDataCleanupPreupgradeProcess,
						dependsOn(
							dlFileEntryEmptyNameDataCleanupPreupgradeProcess,
							dlFileEntryPointingDLFileVersionDataCleanupPreupgradeProcess)
					).put(
						dlFileEntryEmptyNameDataCleanupPreupgradeProcess,
						dependsOn()
					).put(
						_getDLFileEntryMetadataDataCleanupPreupgradeProcess(),
						dependsOn(dlFileEntryDataCleanupPreupgradeProcess)
					).put(
						dlFileEntryPointingDLFileVersionDataCleanupPreupgradeProcess,
						dependsOn(
							dlFileEntryEmptyNameDataCleanupPreupgradeProcess)
					).put(
						_getDLFileShortcutDataCleanupPreupgradeProcess(),
						dependsOn(dlFileEntryDataCleanupPreupgradeProcess)
					).put(
						_getDLFileVersionDataCleanupPreupgradeProcess(),
						dependsOn(dlFileEntryDataCleanupPreupgradeProcess)
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
		_getDLFileEntryDataCleanupPreupgradeProcess() {

		return new DataCleanupPreupgradeProcess() {

			@Override
			protected void doUpgrade() throws Exception {
				_cleanUpOrphans(
					"fileEntryId", "DLFileEntryMetadata", "fileEntryId",
					"DLFileEntry");
				_cleanUpOrphans(
					"fileEntryId", "DLFileVersion", "fileEntryId",
					"DLFileEntry");
				_cleanUpOrphans(
					"fileEntryId", "DLFileVersionPreview", "fileEntryId",
					"DLFileEntry");
				_cleanUpOrphans(
					"toFileEntryId", "DLFileShortcut", "fileEntryId",
					"DLFileEntry");

				upgrade(
					new TableOrphanReferencesDataCleanupPreupgradeProcess(
						null,
						StringBundler.concat(
							"[$SOURCE_TABLE_ALIAS$].name = '",
							DLFileEntry.class.getName(), "'"),
						"primKeyId", "ResourcePermission", "fileEntryId",
						"DLFileEntry"));
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
									sourceColumnName, " was not found in ",
									targetColumnName, " from table ",
									normalizedTargetTableName));
						}
					}
				}
			}

		};
	}

	private DataCleanupPreupgradeProcess
		_getDLFileEntryEmptyNameDataCleanupPreupgradeProcess() {

		return new DataCleanupPreupgradeProcess() {

			@Override
			protected void doUpgrade() throws Exception {
				try (PreparedStatement preparedStatement1 =
						connection.prepareStatement(
							"select fileEntryId, name from DLFileEntry where " +
								"name is null or name = ''");
					PreparedStatement preparedStatement2 =
						connection.prepareStatement(
							"delete from DLFileEntry where name is null or " +
								"name = ''");
					ResultSet resultSet = preparedStatement1.executeQuery()) {

					preparedStatement2.execute();

					if (!_log.isInfoEnabled()) {
						return;
					}

					DBInspector dbInspector = new DBInspector(connection);

					while (resultSet.next()) {
						long fileEntryId = resultSet.getLong("fileEntryId");
						String name = resultSet.getString("name");

						DataCleanupLoggingUtil.logDelete(
							_log, 1, dbInspector.normalizeName("DLFileEntry"),
							StringBundler.concat(
								dbInspector.normalizeName("fileEntryId"),
								StringPool.SPACE, fileEntryId, StringPool.SPACE,
								dbInspector.normalizeName("name"), " was ",
								(name == null) ? "null" : "empty"));
					}
				}
			}

		};
	}

	private DataCleanupPreupgradeProcess
			_getDLFileEntryMetadataDataCleanupPreupgradeProcess()
		throws Exception {

		List<String> structureIds = new ArrayList<>();

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select structureId from DDMStructure where ",
					"DDMStructure.classNameId in (select classNameId from ",
					"ClassName_ where value in ('",
					DLFileEntryMetadata.class.getName(), "', '",
					RawMetadataProcessor.class.getName(), "'))"));

			ResultSet resultSet = preparedStatement.executeQuery()) {

			while (resultSet.next()) {
				structureIds.add(resultSet.getString("structureId"));
			}
		}

		if (structureIds.isEmpty()) {
			return new DataCleanupPreupgradeProcess();
		}

		return new DataCleanupPreupgradeProcess(
			new TableOrphanReferencesDataCleanupPreupgradeProcess(
				null,
				StringBundler.concat(
					"[$SOURCE_TABLE_ALIAS$].structureId in (",
					String.join(", ", structureIds), ")"),
				"classPK", "DDMStorageLink", "DDMStorageId",
				"DLFileEntryMetadata"));
	}

	private DataCleanupPreupgradeProcess
		_getDLFileEntryPointingDLFileVersionDataCleanupPreupgradeProcess() {

		return new DataCleanupPreupgradeProcess(
			new TableOrphanReferencesDataCleanupPreupgradeProcess(
				null, null, "fileEntryId", "DLFileEntry", "fileEntryId",
				"DLFileVersion"));
	}

	private DataCleanupPreupgradeProcess
		_getDLFileShortcutDataCleanupPreupgradeProcess() {

		return new DataCleanupPreupgradeProcess(
			new FilterableAllTablesOrphanReferencesDataCleanupPreupgradeProcess(
				StringBundler.concat(
					"[$SOURCE_TABLE_ALIAS$].classNameId = (select classNameId ",
					"from ClassName_ where value = '",
					DLFileShortcut.class.getName(), "')"),
				new String[] {"classNameId"}, "classPK",
				new String[] {"fileShortcutId"}, "DLFileShortcut"),
			new TableOrphanReferencesDataCleanupPreupgradeProcess(
				null,
				StringBundler.concat(
					"[$SOURCE_TABLE_ALIAS$].name = '",
					DLFileShortcut.class.getName(), "'"),
				"primKeyId", "ResourcePermission", "fileShortcutId",
				"DLFileShortcut"));
	}

	private DataCleanupPreupgradeProcess
		_getDLFileVersionDataCleanupPreupgradeProcess() {

		return new DataCleanupPreupgradeProcess(
			new FilterableAllTablesOrphanReferencesDataCleanupPreupgradeProcess(
				StringBundler.concat(
					"[$SOURCE_TABLE_ALIAS$].classNameId in (select ",
					"classNameId from ClassName_ where value in ('",
					FileEntry.class.getName(), "', '",
					DLFileEntry.class.getName(), "'))"),
				new String[] {"classNameId"}, "classPK",
				new String[] {"fileEntryId", "fileVersionId"},
				"DLFileVersion"));
	}

	private static final int _BATCH_SIZE = 1000;

	private static final Log _log = LogFactoryUtil.getLog(
		DLFileEntryDataCleanupPreupgradeProcess.class);

}