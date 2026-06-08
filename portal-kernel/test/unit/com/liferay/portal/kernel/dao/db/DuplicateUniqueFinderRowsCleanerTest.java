/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.dao.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;

import org.junit.Assert;
import org.junit.Test;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Jorge Avalos
 */
public class DuplicateUniqueFinderRowsCleanerTest {

	@Test
	public void testDeleteDuplicates() throws Exception {
		try (MockedStatic<DBManagerUtil> dbManagerUtilMockedStatic =
				Mockito.mockStatic(DBManagerUtil.class)) {

			DB db = Mockito.mock(DB.class);

			dbManagerUtilMockedStatic.when(
				DBManagerUtil::getDB
			).thenReturn(
				db
			);

			Mockito.when(
				db.getPrimaryKeyColumnNames(_connection, _TABLE_NAME)
			).thenReturn(
				new String[] {_PRIMARY_KEY_COLUMN}
			);

			PreparedStatement groupByPreparedStatement = Mockito.mock(
				PreparedStatement.class);
			ResultSet groupByResultSet = Mockito.mock(ResultSet.class);

			Mockito.when(
				groupByPreparedStatement.executeQuery()
			).thenReturn(
				groupByResultSet
			);

			Mockito.when(
				groupByResultSet.next()
			).thenReturn(
				true, false
			);

			Mockito.when(
				groupByResultSet.getString(1)
			).thenReturn(
				"value1"
			);

			PreparedStatement selectPreparedStatement = Mockito.mock(
				PreparedStatement.class);
			ResultSet selectResultSet = Mockito.mock(ResultSet.class);
			ResultSetMetaData selectResultSetMetaData = Mockito.mock(
				ResultSetMetaData.class);

			Mockito.when(
				selectPreparedStatement.executeQuery()
			).thenReturn(
				selectResultSet
			);

			Mockito.when(
				selectResultSet.getMetaData()
			).thenReturn(
				selectResultSetMetaData
			);

			Mockito.when(
				selectResultSetMetaData.getColumnCount()
			).thenReturn(
				2
			);

			Mockito.when(
				selectResultSetMetaData.getColumnName(1)
			).thenReturn(
				_PRIMARY_KEY_COLUMN
			);

			Mockito.when(
				selectResultSetMetaData.getColumnName(2)
			).thenReturn(
				_COLUMN_NAME
			);

			Mockito.when(
				selectResultSet.next()
			).thenReturn(
				true, true, false
			);

			Mockito.when(
				selectResultSet.getString(_PRIMARY_KEY_COLUMN)
			).thenReturn(
				"1", "2"
			);

			Mockito.when(
				selectResultSet.getString(_COLUMN_NAME)
			).thenReturn(
				"value1"
			);

			PreparedStatement deletePreparedStatement = Mockito.mock(
				PreparedStatement.class);

			Mockito.when(
				_connection.prepareStatement(Mockito.anyString())
			).thenReturn(
				groupByPreparedStatement, selectPreparedStatement,
				deletePreparedStatement
			);

			Mockito.when(
				_connection.getMetaData()
			).thenReturn(
				_databaseMetaData
			);

			Mockito.when(
				_databaseMetaData.storesLowerCaseIdentifiers()
			).thenReturn(
				false
			);

			Mockito.when(
				_databaseMetaData.storesUpperCaseIdentifiers()
			).thenReturn(
				false
			);

			ResultSet columnsResultSet = Mockito.mock(ResultSet.class);

			Mockito.when(
				_databaseMetaData.getColumns(
					Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())
			).thenReturn(
				columnsResultSet
			);

			Mockito.when(
				columnsResultSet.getInt("DATA_TYPE")
			).thenReturn(
				Types.VARCHAR
			);

			DuplicateUniqueFinderRowsCleaner cleaner =
				new DuplicateUniqueFinderRowsCleaner(
					_connection, _TABLE_NAME, new String[] {_COLUMN_NAME},
					_PRIMARY_KEY_COLUMN + " asc");

			Assert.assertTrue(cleaner.deleteDuplicates());

			Mockito.verify(
				deletePreparedStatement
			).execute();
		}
	}

	@Test
	public void testDeleteDuplicatesNoDuplicates() throws Exception {
		try (MockedStatic<DBManagerUtil> dbManagerUtilMockedStatic =
				Mockito.mockStatic(DBManagerUtil.class)) {

			dbManagerUtilMockedStatic.when(
				DBManagerUtil::getDB
			).thenReturn(
				Mockito.mock(DB.class)
			);

			PreparedStatement groupByPreparedStatement = Mockito.mock(
				PreparedStatement.class);
			ResultSet groupByResultSet = Mockito.mock(ResultSet.class);

			Mockito.when(
				groupByPreparedStatement.executeQuery()
			).thenReturn(
				groupByResultSet
			);

			Mockito.when(
				groupByResultSet.next()
			).thenReturn(
				false
			);

			Mockito.when(
				_connection.prepareStatement(Mockito.anyString())
			).thenReturn(
				groupByPreparedStatement
			);

			DuplicateUniqueFinderRowsCleaner cleaner =
				new DuplicateUniqueFinderRowsCleaner(
					_connection, _TABLE_NAME, new String[] {_COLUMN_NAME},
					_PRIMARY_KEY_COLUMN + " asc");

			Assert.assertFalse(cleaner.deleteDuplicates());
		}
	}

	@Test
	public void testDeleteDuplicatesNullColumn() throws Exception {
		try (MockedStatic<DBManagerUtil> dbManagerUtilMockedStatic =
				Mockito.mockStatic(DBManagerUtil.class)) {

			DB db = Mockito.mock(DB.class);

			dbManagerUtilMockedStatic.when(
				DBManagerUtil::getDB
			).thenReturn(
				db
			);

			Mockito.when(
				db.getPrimaryKeyColumnNames(_connection, _TABLE_NAME)
			).thenReturn(
				new String[] {_PRIMARY_KEY_COLUMN}
			);

			PreparedStatement groupByPreparedStatement = Mockito.mock(
				PreparedStatement.class);
			ResultSet groupByResultSet = Mockito.mock(ResultSet.class);

			Mockito.when(
				groupByPreparedStatement.executeQuery()
			).thenReturn(
				groupByResultSet
			);

			Mockito.when(
				groupByResultSet.next()
			).thenReturn(
				true, false
			);

			Mockito.when(
				groupByResultSet.getString(1)
			).thenReturn(
				(String)null
			);

			PreparedStatement selectPreparedStatement = Mockito.mock(
				PreparedStatement.class);
			ResultSet selectResultSet = Mockito.mock(ResultSet.class);
			ResultSetMetaData selectResultSetMetaData = Mockito.mock(
				ResultSetMetaData.class);

			Mockito.when(
				selectPreparedStatement.executeQuery()
			).thenReturn(
				selectResultSet
			);

			Mockito.when(
				selectResultSet.getMetaData()
			).thenReturn(
				selectResultSetMetaData
			);

			Mockito.when(
				selectResultSetMetaData.getColumnCount()
			).thenReturn(
				2
			);

			Mockito.when(
				selectResultSetMetaData.getColumnName(1)
			).thenReturn(
				_PRIMARY_KEY_COLUMN
			);

			Mockito.when(
				selectResultSetMetaData.getColumnName(2)
			).thenReturn(
				_COLUMN_NAME
			);

			Mockito.when(
				selectResultSet.next()
			).thenReturn(
				true, true, false
			);

			Mockito.when(
				selectResultSet.getString(_PRIMARY_KEY_COLUMN)
			).thenReturn(
				"1", "2"
			);

			Mockito.when(
				selectResultSet.getString(_COLUMN_NAME)
			).thenReturn(
				(String)null
			);

			PreparedStatement deletePreparedStatement = Mockito.mock(
				PreparedStatement.class);

			Mockito.when(
				_connection.prepareStatement(Mockito.anyString())
			).thenReturn(
				groupByPreparedStatement, selectPreparedStatement,
				deletePreparedStatement
			);

			Mockito.when(
				_connection.getMetaData()
			).thenReturn(
				_databaseMetaData
			);

			Mockito.when(
				_databaseMetaData.storesLowerCaseIdentifiers()
			).thenReturn(
				false
			);

			Mockito.when(
				_databaseMetaData.storesUpperCaseIdentifiers()
			).thenReturn(
				false
			);

			DuplicateUniqueFinderRowsCleaner cleaner =
				new DuplicateUniqueFinderRowsCleaner(
					_connection, _TABLE_NAME, new String[] {_COLUMN_NAME},
					_PRIMARY_KEY_COLUMN + " asc");

			Assert.assertTrue(cleaner.deleteDuplicates());

			Mockito.verify(
				deletePreparedStatement
			).execute();
		}
	}

	private static final String _COLUMN_NAME = "col";

	private static final String _PRIMARY_KEY_COLUMN = "id";

	private static final String _TABLE_NAME = "TestTable";

	private final Connection _connection = Mockito.mock(Connection.class);
	private final DatabaseMetaData _databaseMetaData = Mockito.mock(
		DatabaseMetaData.class);

}