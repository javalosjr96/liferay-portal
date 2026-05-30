/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.upgrade.data.cleanup;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBInspector;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.db.IndexMetadata;
import com.liferay.portal.kernel.dao.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.UserConstants;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.upgrade.data.cleanup.util.DataCleanupLoggingUtil;
import com.liferay.portal.kernel.util.ArrayUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Luis Ortiz
 */
public class UserAllTablesOrphanReferencesDataCleanupPreupgradeProcess
	extends BaseAllTablesOrphanReferencesDataCleanupPreupgradeProcess {

	public UserAllTablesOrphanReferencesDataCleanupPreupgradeProcess() {
		super("userId", "User_");
	}

	@Override
	protected void cleanUp(
			String sourceColumnName, String sourceTableName,
			String[] targetColumnNames, String targetTableName)
		throws Exception {

		// Phase 1: collect distinct source userIds

		List<Long> tableUserIds = new ArrayList<>();

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select distinct ", sourceColumnName, " from ",
					sourceTableName, " where ", sourceColumnName,
					" is not null and ", sourceColumnName, " != 0"));
			ResultSet resultSet = preparedStatement.executeQuery()) {

			while (resultSet.next()) {
				tableUserIds.add(resultSet.getLong(1));
			}
		}

		if (tableUserIds.isEmpty()) {
			return;
		}

		// Phase 2: find orphans via Java set difference (User_ loaded once)

		Set<Long> validUserIds = _getValidUserIds(
			connection, targetColumnNames[0], targetTableName);

		List<Long> orphanIds = new ArrayList<>();

		for (long userId : tableUserIds) {
			if (!validUserIds.contains(userId)) {
				orphanIds.add(userId);
			}
		}

		if (orphanIds.isEmpty()) {
			return;
		}

		// Phase 3: process orphans in batches

		boolean partOfUniqueIndex = _isPartOfUniqueIndex(
			connection, sourceColumnName, sourceTableName);

		for (int i = 0; i < orphanIds.size(); i += _BATCH_SIZE) {
			int end = Math.min(i + _BATCH_SIZE, orphanIds.size());

			List<String> batchStrings = new ArrayList<>(end - i);

			for (int j = i; j < end; j++) {
				batchStrings.add(String.valueOf(orphanIds.get(j)));
			}

			try (PreparedStatement preparedStatement1 =
					connection.prepareStatement(
						StringBundler.concat(
							"select distinct ", sourceColumnName,
							", companyId from ", sourceTableName, " where ",
							sourceColumnName, " in (",
							String.join(
								StringPool.COMMA_AND_SPACE, batchStrings),
							")"));
				PreparedStatement preparedStatement2 =
					AutoBatchPreparedStatementUtil.concurrentAutoBatch(
						connection,
						StringBundler.concat(
							"delete from ", sourceTableName, " where ",
							sourceColumnName, " = ? and companyId = ?"));
				PreparedStatement preparedStatement3 =
					AutoBatchPreparedStatementUtil.concurrentAutoBatch(
						connection,
						StringBundler.concat(
							"update ", sourceTableName, " set ",
							sourceColumnName, " = ? where ", sourceColumnName,
							" = ? and companyId = ?"));
				ResultSet resultSet = preparedStatement1.executeQuery()) {

				while (resultSet.next()) {
					long companyId = resultSet.getLong("companyId");
					long userId = resultSet.getLong(sourceColumnName);

					if (_deleteTableNames.contains(sourceTableName) ||
						partOfUniqueIndex) {

						preparedStatement2.setLong(1, userId);
						preparedStatement2.setLong(2, companyId);

						preparedStatement2.addBatch();

						DataCleanupLoggingUtil.logDelete(
							_log, 1, sourceTableName,
							StringBundler.concat(
								sourceColumnName, StringPool.SPACE, userId,
								" was not found in column",
								(targetColumnNames.length > 1) ? "s " : " ",
								String.join(", ", targetColumnNames),
								" from table ", targetTableName));

						continue;
					}

					long newUserId = _getAdminUserId(connection, companyId);

					if (newUserId == 0) {
						continue;
					}

					preparedStatement3.setLong(1, newUserId);
					preparedStatement3.setLong(2, userId);
					preparedStatement3.setLong(3, companyId);

					preparedStatement3.addBatch();

					DataCleanupLoggingUtil.logUpdate(
						_log, 1, sourceTableName, sourceColumnName, newUserId,
						StringBundler.concat(
							sourceColumnName, StringPool.SPACE, userId,
							" was not found in column",
							(targetColumnNames.length > 1) ? "s " : " ",
							String.join(", ", targetColumnNames),
							" from table ", targetTableName));
				}

				preparedStatement2.executeBatch();

				preparedStatement3.executeBatch();
			}
		}
	}

	@Override
	protected boolean shouldSkipSourceTable(
			DBInspector dbInspector, String sourceTableName)
		throws Exception {

		return !dbInspector.hasColumn(sourceTableName, "companyId");
	}

	private long _getAdminUserId(Connection connection, long companyId)
		throws Exception {

		Long cachedUserId = _adminUserIds.get(companyId);

		if (cachedUserId != null) {
			return cachedUserId;
		}

		Boolean hasUserTypeColumn = _hasUserTypeColumn;

		if (hasUserTypeColumn == null) {
			synchronized (this) {
				if (_hasUserTypeColumn == null) {
					DBInspector dbInspector = new DBInspector(connection);

					_hasUserTypeColumn = dbInspector.hasColumn(
						"User_", "type_");
				}

				hasUserTypeColumn = _hasUserTypeColumn;
			}
		}

		boolean hasColumn = hasUserTypeColumn;

		StringBundler sb = new StringBundler(6);

		sb.append("select User_.userId from User_ inner join Users_Roles on ");
		sb.append("User_.userId = Users_Roles.userId inner join Role_ on ");
		sb.append("Users_Roles.roleId = Role_.roleId where Role_.name = ? ");
		sb.append("and User_.companyId = ? and Role_.companyId = ?");

		if (hasColumn) {
			sb.append(" and User_.type_ = ?");
		}

		sb.append(" order by User_.userId asc");

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				sb.toString())) {

			preparedStatement.setString(1, RoleConstants.ADMINISTRATOR);
			preparedStatement.setLong(2, companyId);
			preparedStatement.setLong(3, companyId);

			if (hasColumn) {
				preparedStatement.setInt(4, UserConstants.TYPE_REGULAR);
			}

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				long userId = 0;

				if (resultSet.next()) {
					userId = resultSet.getLong(1);
				}
				else {
					if (_log.isWarnEnabled()) {
						_log.warn(
							"Unable to find admin user for company " +
								companyId);
					}
				}

				_adminUserIds.putIfAbsent(companyId, userId);

				return userId;
			}
		}
	}

	private Set<Long> _getValidUserIds(
			Connection connection, String targetColumnName,
			String targetTableName)
		throws Exception {

		Set<Long> validUserIds = _validUserIdsByConnection.get(connection);

		if (validUserIds != null) {
			return validUserIds;
		}

		validUserIds = new HashSet<>();

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select ", targetColumnName, " from ", targetTableName));
			ResultSet resultSet = preparedStatement.executeQuery()) {

			while (resultSet.next()) {
				validUserIds.add(resultSet.getLong(1));
			}
		}

		_validUserIdsByConnection.putIfAbsent(connection, validUserIds);

		return _validUserIdsByConnection.get(connection);
	}

	private boolean _isPartOfUniqueIndex(
			Connection connection, String sourceColumnName,
			String sourceTableName)
		throws Exception {

		Boolean partOfUniqueIndex = _isPartOfUniqueIndexCache.get(
			sourceTableName);

		if (partOfUniqueIndex != null) {
			return partOfUniqueIndex;
		}

		DB db = DBManagerUtil.getDB();

		List<IndexMetadata> indexes = db.getIndexMetadatas(
			connection, sourceTableName, sourceColumnName, true);

		if (!indexes.isEmpty()) {
			_isPartOfUniqueIndexCache.put(sourceTableName, true);

			return true;
		}

		String[] columnNames = db.getPrimaryKeyColumnNames(
			connection, sourceTableName);

		boolean result = ArrayUtil.contains(columnNames, sourceColumnName);

		_isPartOfUniqueIndexCache.put(sourceTableName, result);

		return result;
	}

	private static final int _BATCH_SIZE = 1000;

	private static final Log _log = LogFactoryUtil.getLog(
		UserAllTablesOrphanReferencesDataCleanupPreupgradeProcess.class);

	private static final Set<String> _deleteTableNames = new TreeSet<>(
		String.CASE_INSENSITIVE_ORDER) {

		{
			addAll(
				Set.of(
					"MFAEmailOTPEntry", "MFAFIDO2CredentialEntry",
					"MFATimeBasedOTPEntry", "OAuth2Authorization",
					"OpenIdConnectSession", "OpenIdConnectUser",
					"SamlIdpSpSession", "SamlIdpSsoSession", "SamlPeerBinding",
					"SamlSpSession"));
		}
	};

	private final Map<Long, Long> _adminUserIds = new ConcurrentHashMap<>();
	private volatile Boolean _hasUserTypeColumn;
	private final Map<String, Boolean> _isPartOfUniqueIndexCache =
		new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Connection, Set<Long>>
		_validUserIdsByConnection = new ConcurrentHashMap<>();

}