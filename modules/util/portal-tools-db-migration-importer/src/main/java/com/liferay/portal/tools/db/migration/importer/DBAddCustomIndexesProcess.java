/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.tools.db.migration.importer;

import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.db.IndexMetadataFactoryUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.StringUtil;

import java.nio.charset.StandardCharsets;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

/**
 * @author Jorge Avalos
 */
public class DBAddCustomIndexesProcess {

	public DBAddCustomIndexesProcess(
		DataSource sourceDataSource, DataSource targetDataSource) {

		this(null, sourceDataSource, targetDataSource);
	}

	public DBAddCustomIndexesProcess(
		String partitionName, DataSource sourceDataSource,
		DataSource targetDataSource) {

		_sourceDataSource = sourceDataSource;
		_targetDataSource = targetDataSource;

		if (partitionName == null) {
			_partitionMessage = " in the default schema";
		}
		else {
			_partitionMessage = " in partition " + partitionName;
		}
	}

	public List<String> run() {
		List<String> messages = new ArrayList<>();

		try {
			_run(messages);

			System.out.println(
				"Finished adding custom indexes" + _partitionMessage);
		}
		catch (Exception exception) {
			_addMessage(
				messages,
				StringBundler.concat(
					"Unable to add custom indexes", _partitionMessage, ": ",
					exception.getMessage()));
		}

		return messages;
	}

	private void _addIndex(
		String indexName, List<String> messages, Set<String> relationNames,
		SourceIndex sourceIndex, Statement statement, String tableName,
		List<String> targetColumnNames,
		Map<String, SourceIndex> targetIndexes) {

		SourceIndex targetIndex = targetIndexes.get(indexName);

		if ((targetIndex != null) &&
			!targetColumnNames.containsAll(targetIndex.getColumnNames())) {

			return;
		}

		List<String> columnNames = sourceIndex.getColumnNames();

		for (SourceIndex curTargetIndex : targetIndexes.values()) {
			if (columnNames.equals(curTargetIndex.getColumnNames()) &&
				(curTargetIndex.isUnique() || !sourceIndex.isUnique())) {

				return;
			}
		}

		if (!sourceIndex.isExpression() &&
			ListUtil.exists(columnNames, this::_isLongName)) {

			_addMessage(
				messages,
				StringBundler.concat(
					"Unable to create index ", indexName, " on table ",
					tableName, _partitionMessage,
					" because the name of one of its columns is longer than ",
					_POSTGRESQL_NAME_MAX_LENGTH, " bytes"));

			return;
		}

		if (sourceIndex.isExpression() ||
			!targetColumnNames.containsAll(columnNames)) {

			_addMessage(
				messages,
				StringBundler.concat(
					"Unable to create index ", indexName, " on table ",
					tableName, _partitionMessage,
					" because it is not based on whole table columns"));

			return;
		}

		String targetIndexName = _getIndexName(
			columnNames, indexName, relationNames, tableName);

		if (targetIndexName == null) {
			_addMessage(
				messages,
				StringBundler.concat(
					"Unable to create index ", indexName, " on table ",
					tableName, _partitionMessage,
					" because its name is already used"));

			return;
		}

		try {
			String unique = StringPool.BLANK;

			if (sourceIndex.isUnique()) {
				unique = "unique ";
			}

			statement.executeUpdate(
				StringBundler.concat(
					"create ", unique, "index ",
					_getQuotedName(targetIndexName), " on ",
					_getQuotedName(tableName), " (",
					_getQuotedNames(columnNames), ")"));
		}
		catch (SQLException sqlException) {
			_addMessage(
				messages,
				StringBundler.concat(
					"Unable to create index ", indexName, " on table ",
					tableName, _partitionMessage, ": ",
					sqlException.getMessage()));

			return;
		}

		relationNames.add(targetIndexName);
		targetIndexes.put(targetIndexName, sourceIndex);

		if (!targetIndexName.equals(StringUtil.toLowerCase(indexName))) {
			_addMessage(
				messages,
				StringBundler.concat(
					"Created index ", targetIndexName, " on table ", tableName,
					" instead of ", indexName, _partitionMessage));
		}
	}

	private void _addIndexes(
			List<String> messages, Set<String> relationNames,
			Map<String, SourceIndex> sourceIndexes,
			List<String> sourcePrimaryKeyColumnNames, Statement statement,
			String tableName, List<String> targetColumnNames,
			Connection targetConnection)
		throws SQLException {

		Map<String, SourceIndex> targetIndexes = _getIndexes(
			targetConnection, tableName);

		_addPrimaryKey(
			messages, relationNames, sourcePrimaryKeyColumnNames, statement,
			tableName, targetIndexes,
			_getPrimaryKeyColumnNames(targetConnection, tableName));

		for (Map.Entry<String, SourceIndex> entry : sourceIndexes.entrySet()) {
			_addIndex(
				entry.getKey(), messages, relationNames, entry.getValue(),
				statement, tableName, targetColumnNames, targetIndexes);
		}
	}

	private void _addMessage(List<String> messages, String message) {
		messages.add(message);

		System.out.println(message);
	}

	private void _addPrimaryKey(
		List<String> messages, Set<String> relationNames,
		List<String> sourcePrimaryKeyColumnNames, Statement statement,
		String tableName, Map<String, SourceIndex> targetIndexes,
		List<String> targetPrimaryKeyColumnNames) {

		if (ListUtil.isEmpty(sourcePrimaryKeyColumnNames) ||
			!targetPrimaryKeyColumnNames.isEmpty()) {

			return;
		}

		if (ListUtil.exists(sourcePrimaryKeyColumnNames, this::_isLongName)) {
			_addMessage(
				messages,
				StringBundler.concat(
					"Unable to add the primary key of table ", tableName,
					_partitionMessage,
					" because the name of one of its columns is longer than ",
					_POSTGRESQL_NAME_MAX_LENGTH, " bytes"));

			return;
		}

		try {
			statement.executeUpdate(
				StringBundler.concat(
					"alter table ", _getQuotedName(tableName),
					" add primary key (",
					_getQuotedNames(sourcePrimaryKeyColumnNames), ")"));
		}
		catch (SQLException sqlException) {
			_addMessage(
				messages,
				StringBundler.concat(
					"Unable to add the primary key of table ", tableName,
					_partitionMessage, ": ", sqlException.getMessage()));

			return;
		}

		String primaryKeyIndexName = tableName + "_pkey";

		relationNames.add(primaryKeyIndexName);
		targetIndexes.put(
			primaryKeyIndexName,
			new SourceIndex(sourcePrimaryKeyColumnNames, true));
	}

	private Map<String, List<String>> _getColumnNamesMap(Connection connection)
		throws SQLException {

		DatabaseMetaData databaseMetaData = connection.getMetaData();

		Set<String> tableNames = new HashSet<>();

		try (ResultSet resultSet = databaseMetaData.getTables(
				connection.getCatalog(), connection.getSchema(), null,
				new String[] {"TABLE"})) {

			while (resultSet.next()) {
				tableNames.add(resultSet.getString("TABLE_NAME"));
			}
		}

		Map<String, List<String>> columnNamesMap = new TreeMap<>(
			String.CASE_INSENSITIVE_ORDER);

		try (ResultSet resultSet = databaseMetaData.getColumns(
				connection.getCatalog(), connection.getSchema(), null, null)) {

			while (resultSet.next()) {
				String tableName = resultSet.getString("TABLE_NAME");

				if (!tableNames.contains(tableName)) {
					continue;
				}

				List<String> columnNames = columnNamesMap.computeIfAbsent(
					tableName, key -> new ArrayList<>());

				columnNames.add(
					StringUtil.toLowerCase(resultSet.getString("COLUMN_NAME")));
			}
		}

		return columnNamesMap;
	}

	private String _getIndexName(
		List<String> columnNames, String indexName, Set<String> relationNames,
		String tableName) {

		String targetIndexName = StringUtil.toLowerCase(indexName);

		Matcher matcher = _identifierPattern.matcher(targetIndexName);

		if (matcher.matches() && !_isLongName(targetIndexName) &&
			!targetIndexName.equals("primary") &&
			!relationNames.contains(targetIndexName)) {

			return targetIndexName;
		}

		String createdIndexName = IndexMetadataFactoryUtil.createIndexName(
			tableName, columnNames.toArray(new String[0]));

		targetIndexName = StringUtil.toLowerCase("custom_" + createdIndexName);

		if (relationNames.contains(targetIndexName)) {
			return null;
		}

		return targetIndexName;
	}

	private Map<String, SourceIndex> _getIndexes(
			Connection connection, String tableName)
		throws SQLException {

		Map<String, SourceIndex> indexes = new TreeMap<>(
			String.CASE_INSENSITIVE_ORDER);

		DatabaseMetaData databaseMetaData = connection.getMetaData();

		try (ResultSet resultSet = databaseMetaData.getIndexInfo(
				connection.getCatalog(), connection.getSchema(), tableName,
				false, true)) {

			while (resultSet.next()) {
				String indexName = resultSet.getString("INDEX_NAME");

				if (indexName == null) {
					continue;
				}

				boolean unique = !resultSet.getBoolean("NON_UNIQUE");

				SourceIndex sourceIndex = indexes.computeIfAbsent(
					indexName, key -> new SourceIndex(unique));

				sourceIndex.addColumnName(
					resultSet.getString("COLUMN_NAME"),
					resultSet.getInt("ORDINAL_POSITION"));

				if (resultSet.getString("FILTER_CONDITION") == null) {
					continue;
				}

				sourceIndex.setExpression(true);
			}
		}

		Set<String> partialIndexNames = _getPartialIndexNames(
			connection, databaseMetaData.getDatabaseProductName(), tableName);

		for (String partialIndexName : partialIndexNames) {
			SourceIndex sourceIndex = indexes.get(partialIndexName);

			if (sourceIndex == null) {
				continue;
			}

			sourceIndex.setExpression(true);
		}

		return indexes;
	}

	private Set<String> _getPartialIndexNames(
			Connection connection, String databaseProductName, String tableName)
		throws SQLException {

		Set<String> partialIndexNames = new HashSet<>();

		if (StringUtil.equalsIgnoreCase(databaseProductName, "MariaDB") ||
			StringUtil.equalsIgnoreCase(databaseProductName, "MySQL")) {

			try (Statement statement = connection.createStatement();

				ResultSet resultSet = statement.executeQuery(
					"show index from `" +
						StringUtil.replace(tableName, CharPool.PRIME, "``") +
							"`")) {

				while (resultSet.next()) {
					String indexType = resultSet.getString("Index_type");

					if ((resultSet.getObject("Sub_part") == null) &&
						!StringUtil.equalsIgnoreCase(indexType, "FULLTEXT") &&
						!StringUtil.equalsIgnoreCase(indexType, "SPATIAL")) {

						continue;
					}

					partialIndexNames.add(resultSet.getString("Key_name"));
				}
			}
		}
		else if (StringUtil.equalsIgnoreCase(
					databaseProductName, "Microsoft SQL Server")) {

			try (PreparedStatement preparedStatement =
					connection.prepareStatement(
						"select name from sys.indexes where object_id = " +
							"object_id(?) and has_filter = 1")) {

				String schemaName = connection.getSchema();

				if (schemaName == null) {
					preparedStatement.setString(1, tableName);
				}
				else {
					preparedStatement.setString(
						1, schemaName + "." + tableName);
				}

				try (ResultSet resultSet = preparedStatement.executeQuery()) {
					while (resultSet.next()) {
						partialIndexNames.add(resultSet.getString("name"));
					}
				}
			}
		}

		return partialIndexNames;
	}

	private List<String> _getPrimaryKeyColumnNames(
			Connection connection, String tableName)
		throws SQLException {

		DatabaseMetaData databaseMetaData = connection.getMetaData();

		Map<Integer, String> columnNames = new TreeMap<>();

		try (ResultSet resultSet = databaseMetaData.getPrimaryKeys(
				connection.getCatalog(), connection.getSchema(), tableName)) {

			while (resultSet.next()) {
				columnNames.put(
					resultSet.getInt("KEY_SEQ"),
					StringUtil.toLowerCase(resultSet.getString("COLUMN_NAME")));
			}
		}

		return new ArrayList<>(columnNames.values());
	}

	private String _getQuotedName(String name) {
		return StringBundler.concat(
			StringPool.QUOTE,
			StringUtil.replace(name, CharPool.QUOTE, StringPool.DOUBLE_QUOTE),
			StringPool.QUOTE);
	}

	private String _getQuotedNames(List<String> names) {
		List<String> quotedNames = new ArrayList<>();

		for (String name : names) {
			quotedNames.add(_getQuotedName(name));
		}

		return StringUtil.merge(quotedNames);
	}

	private Set<String> _getRelationNames(Connection connection)
		throws SQLException {

		Set<String> relationNames = new HashSet<>();

		try (Statement statement = connection.createStatement();

			ResultSet resultSet = statement.executeQuery(
				"select relname from pg_class inner join pg_namespace on " +
					"pg_namespace.oid = pg_class.relnamespace where nspname " +
						"= current_schema()")) {

			while (resultSet.next()) {
				relationNames.add(resultSet.getString("relname"));
			}
		}

		return relationNames;
	}

	private boolean _isLongName(String name) {
		byte[] bytes = name.getBytes(StandardCharsets.UTF_8);

		if (bytes.length > _POSTGRESQL_NAME_MAX_LENGTH) {
			return true;
		}

		return false;
	}

	private void _run(List<String> messages) throws SQLException {
		try (Connection targetConnection = _targetDataSource.getConnection();

			Statement statement = targetConnection.createStatement()) {

			Map<String, List<String>> targetColumnNamesMap = _getColumnNamesMap(
				targetConnection);

			Map<String, Map<String, SourceIndex>> sourceIndexesMap =
				new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			Map<String, List<String>> sourcePrimaryKeyColumnNamesMap =
				new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

			try (Connection sourceConnection =
					_sourceDataSource.getConnection()) {

				Map<String, List<String>> sourceColumnNamesMap =
					_getColumnNamesMap(sourceConnection);

				for (String tableName : sourceColumnNamesMap.keySet()) {
					if (!targetColumnNamesMap.containsKey(tableName)) {
						continue;
					}

					try {
						Map<String, SourceIndex> sourceIndexes = _getIndexes(
							sourceConnection, tableName);
						List<String> sourcePrimaryKeyColumnNames =
							_getPrimaryKeyColumnNames(
								sourceConnection, tableName);

						sourceIndexesMap.put(tableName, sourceIndexes);
						sourcePrimaryKeyColumnNamesMap.put(
							tableName, sourcePrimaryKeyColumnNames);
					}
					catch (SQLException sqlException) {
						_addMessage(
							messages,
							StringBundler.concat(
								"Unable to read the indexes of table ",
								tableName, _partitionMessage, ": ",
								sqlException.getMessage()));
					}
				}
			}

			Set<String> relationNames = _getRelationNames(targetConnection);

			for (Map.Entry<String, Map<String, SourceIndex>> entry :
					sourceIndexesMap.entrySet()) {

				String tableName = StringUtil.toLowerCase(entry.getKey());

				try {
					_addIndexes(
						messages, relationNames, entry.getValue(),
						sourcePrimaryKeyColumnNamesMap.get(tableName),
						statement, tableName,
						targetColumnNamesMap.get(tableName), targetConnection);
				}
				catch (SQLException sqlException) {
					_addMessage(
						messages,
						StringBundler.concat(
							"Unable to add the indexes of table ", tableName,
							_partitionMessage, ": ",
							sqlException.getMessage()));
				}
			}
		}
	}

	private static final int _POSTGRESQL_NAME_MAX_LENGTH = 63;

	private static final Pattern _identifierPattern = Pattern.compile(
		"[a-z_][a-z0-9_$]*");

	private final String _partitionMessage;
	private final DataSource _sourceDataSource;
	private final DataSource _targetDataSource;

	private static class SourceIndex {

		public SourceIndex(boolean unique) {
			_unique = unique;
		}

		public SourceIndex(List<String> columnNames, boolean unique) {
			_unique = unique;

			for (int i = 0; i < columnNames.size(); i++) {
				_columnNames.put(i + 1, columnNames.get(i));
			}
		}

		public void addColumnName(String columnName, int position) {
			if (columnName == null) {
				_expression = true;

				return;
			}

			_columnNames.put(position, StringUtil.toLowerCase(columnName));
		}

		public List<String> getColumnNames() {
			return new ArrayList<>(_columnNames.values());
		}

		public boolean isExpression() {
			return _expression;
		}

		public boolean isUnique() {
			return _unique;
		}

		public void setExpression(boolean expression) {
			_expression = expression;
		}

		private final Map<Integer, String> _columnNames = new TreeMap<>();
		private boolean _expression;
		private final boolean _unique;

	}

}