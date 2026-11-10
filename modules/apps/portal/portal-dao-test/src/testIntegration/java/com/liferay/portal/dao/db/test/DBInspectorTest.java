/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.dao.db.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBInspector;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Alberto Chaparro
 */
@RunWith(Arquillian.class)
public class DBInspectorTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@BeforeClass
	public static void setUpClass() throws Exception {
		_connection = DataAccess.getConnection();

		_dbInspector = new DBInspector(_connection);

		_db = DBManagerUtil.getDB();

		_db.runSQL(
			StringBundler.concat(
				"create table ", _TABLE_NAME, " (id LONG not null primary ",
				"key, notNilColumn VARCHAR(75) not null, nilColumn ",
				"VARCHAR(75) null, typeBigDecimal BIGDECIMAL, typeBlob BLOB, ",
				"typeBoolean BOOLEAN, typeDate DATE null, typeDouble DOUBLE, ",
				"typeInteger INTEGER, typeLong LONG null, typeLongDefault ",
				"LONG default 10 not null, typeNumeric NUMERIC(13,4) null, ",
				"typeSBlob SBLOB, typeString STRING null, typeText TEXT null, ",
				"typeVarchar VARCHAR(75) null, typeVarcharDefault VARCHAR(10) ",
				"default 'testValue' not null)"));

		_db.runSQL(
			StringBundler.concat(
				"create view ", _VIEW_NAME, " as select id from ",
				_TABLE_NAME));
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		_db.runSQL("drop view " + _VIEW_NAME);
		_db.runSQL("drop table " + _TABLE_NAME);

		DataAccess.cleanUp(_connection);
	}

	@Test
	public void testGetColumnsResultSet() throws Exception {
		try (ResultSet resultSet = _dbInspector.getColumnsResultSet(
				"DBInspecto_Test")) {

			Assert.assertFalse(resultSet.next());
		}
	}

	@Test
	public void testHasColumn() throws Exception {
		Assert.assertFalse(
			_dbInspector.hasColumn(_TABLE_NAME, _COLUMN_NAME_NONEXISTING));
		Assert.assertFalse(
			_dbInspector.hasColumn(_TABLE_NAME, _COLUMN_NAME_WILDCARD));
		Assert.assertFalse(_dbInspector.hasColumn(_TABLE_NAME, "typeLong%"));
		Assert.assertFalse(_dbInspector.hasColumn(_TABLE_NAME, null));
		Assert.assertFalse(_dbInspector.hasColumn(null, _COLUMN_NAME));
		Assert.assertTrue(_dbInspector.hasColumn(_TABLE_NAME, _COLUMN_NAME));

		DatabaseMetaData databaseMetaData = _connection.getMetaData();

		if (databaseMetaData.storesLowerCaseIdentifiers()) {
			Assert.assertTrue(
				_dbInspector.hasColumn(
					_TABLE_NAME, StringUtil.toLowerCase(_COLUMN_NAME)));
		}

		if (databaseMetaData.storesUpperCaseIdentifiers()) {
			Assert.assertTrue(
				_dbInspector.hasColumn(
					_TABLE_NAME, StringUtil.toUpperCase(_COLUMN_NAME)));
		}
	}

	@Test
	public void testHasColumnType() throws Exception {
		Assert.assertFalse(
			_dbInspector.hasColumnType(
				_TABLE_NAME, "nilColumn", "VARCHAR(75) not null"));
		Assert.assertFalse(
			_dbInspector.hasColumnType(
				_TABLE_NAME, "typ_Varchar", "VARCHAR(75) null"));
		Assert.assertFalse(
			_dbInspector.hasColumnType(
				_TABLE_NAME, "notNilColumn", "VARCHAR(75) null"));
		Assert.assertFalse(
			_dbInspector.hasColumnType(
				_TABLE_NAME, "typeLongDefault", "LONG default 15 not null"));
		Assert.assertFalse(
			_dbInspector.hasColumnType(
				_TABLE_NAME, "typeNumeric", "NUMERIC(10,4) null"));
		Assert.assertFalse(
			_dbInspector.hasColumnType(
				_TABLE_NAME, "typeNumeric", "NUMERIC(13,2) null"));
		Assert.assertFalse(
			_dbInspector.hasColumnType(
				_TABLE_NAME, "typeNumeric", "NUMERIC(13,4) not null"));
		Assert.assertFalse(
			_dbInspector.hasColumnType(
				_TABLE_NAME, "typeVarchar", "STRING null"));
		Assert.assertFalse(
			_dbInspector.hasColumnType(
				_TABLE_NAME, "typeVarchar", "TEXT null"));
		Assert.assertFalse(
			_dbInspector.hasColumnType(
				_TABLE_NAME, "typeVarcharDefault",
				"VARCHAR(10) default 'notTestValue' not null"));
		Assert.assertFalse(
			_dbInspector.hasColumnType(_TABLE_NAME, _COLUMN_NAME, null));
		Assert.assertFalse(
			_dbInspector.hasColumnType(_TABLE_NAME, null, "STRING null"));
		Assert.assertFalse(
			_dbInspector.hasColumnType(null, _COLUMN_NAME, "STRING null"));
		Assert.assertTrue(
			_dbInspector.hasColumnType(
				_TABLE_NAME, "nilColumn", "VARCHAR(75) null"));
		Assert.assertTrue(
			_dbInspector.hasColumnType(
				_TABLE_NAME, "notNilColumn", "VARCHAR(75) not null"));
		Assert.assertTrue(
			_dbInspector.hasColumnType(
				_TABLE_NAME, "typeBigDecimal", "BIGDECIMAL"));
		Assert.assertTrue(
			_dbInspector.hasColumnType(_TABLE_NAME, "typeBlob", "BLOB null"));
		Assert.assertTrue(
			_dbInspector.hasColumnType(
				_TABLE_NAME, "typeBoolean", "BOOLEAN null"));
		Assert.assertTrue(
			_dbInspector.hasColumnType(_TABLE_NAME, "typeDate", "DATE null"));
		Assert.assertTrue(
			_dbInspector.hasColumnType(
				_TABLE_NAME, "typeDouble", "DOUBLE null"));
		Assert.assertTrue(
			_dbInspector.hasColumnType(
				_TABLE_NAME, "typeInteger", "INTEGER null"));
		Assert.assertTrue(
			_dbInspector.hasColumnType(_TABLE_NAME, "typeLong", "LONG null"));
		Assert.assertTrue(
			_dbInspector.hasColumnType(
				_TABLE_NAME, "typeLongDefault", "LONG default 10 not null"));
		Assert.assertTrue(
			_dbInspector.hasColumnType(
				_TABLE_NAME, "typeNumeric", "NUMERIC(13,4) null"));
		Assert.assertTrue(
			_dbInspector.hasColumnType(_TABLE_NAME, "typeSBlob", "SBLOB null"));
		Assert.assertTrue(
			_dbInspector.hasColumnType(
				_TABLE_NAME, "typeString", "STRING null"));
		Assert.assertTrue(
			_dbInspector.hasColumnType(_TABLE_NAME, "typeText", "TEXT null"));
		Assert.assertTrue(
			_dbInspector.hasColumnType(
				_TABLE_NAME, "typeVarchar", "VARCHAR(75) null"));
		Assert.assertTrue(
			_dbInspector.hasColumnType(
				_TABLE_NAME, "typeVarcharDefault",
				"VARCHAR(10) default 'testValue' not null"));
	}

	@Test
	public void testHasColumnWithCacheEnabled() throws Exception {
		DBInspector.enableCache();

		try {
			Assert.assertFalse(
				_dbInspector.hasColumn(_TABLE_NAME, _COLUMN_NAME_WILDCARD));
			Assert.assertTrue(
				_dbInspector.hasColumn(_TABLE_NAME, _COLUMN_NAME));
		}
		finally {
			DBInspector.disableCache();
		}
	}

	@Test
	public void testHasIndex() throws Exception {
		Assert.assertFalse(_dbInspector.hasIndex(_TABLE_NAME, null));
		Assert.assertFalse(_dbInspector.hasIndex(null, "IX_40A51197"));
	}

	@Test
	public void testHasRows() {
		Assert.assertFalse(_dbInspector.hasRows(null));
	}

	@Test
	public void testHasTable() throws Exception {
		Assert.assertFalse(_dbInspector.hasTable("DBInspecto_Test"));
		Assert.assertFalse(_dbInspector.hasTable("DBInspector%"));
		Assert.assertFalse(_dbInspector.hasTable(_TABLE_NAME_NONEXISTING));
		Assert.assertFalse(_dbInspector.hasTable(null));
		Assert.assertTrue(_dbInspector.hasTable(_TABLE_NAME));

		DatabaseMetaData databaseMetaData = _connection.getMetaData();

		if (databaseMetaData.storesLowerCaseIdentifiers()) {
			Assert.assertTrue(
				_dbInspector.hasTable(StringUtil.toLowerCase(_TABLE_NAME)));
		}

		if (databaseMetaData.storesUpperCaseIdentifiers()) {
			Assert.assertTrue(
				_dbInspector.hasTable(StringUtil.toUpperCase(_TABLE_NAME)));
		}
	}

	@Test
	public void testHasView() throws Exception {
		Assert.assertFalse(_dbInspector.hasView("DBInspectorTes_View"));
		Assert.assertFalse(_dbInspector.hasView(null));
		Assert.assertTrue(_dbInspector.hasView(_VIEW_NAME));
	}

	@Test
	public void testIsNullable() throws Exception {
		Assert.assertThrows(
			SQLException.class,
			() -> _dbInspector.isNullable(_TABLE_NAME, "ni_Column"));
		Assert.assertFalse(
			_dbInspector.isNullable(_TABLE_NAME, "notNilColumn"));
		Assert.assertFalse(_dbInspector.isNullable(_TABLE_NAME, null));
		Assert.assertFalse(_dbInspector.isNullable(null, _COLUMN_NAME));
		Assert.assertTrue(_dbInspector.isNullable(_TABLE_NAME, "nilColumn"));
	}

	@Test
	public void testIsNumeric() throws Exception {
		Assert.assertFalse(
			_dbInspector.isNumeric(_TABLE_NAME, _COLUMN_NAME_WILDCARD));
		Assert.assertFalse(_dbInspector.isNumeric(_TABLE_NAME, null));
		Assert.assertFalse(_dbInspector.isNumeric(null, _COLUMN_NAME));
	}

	@Test
	public void testIsObjectTable() {
		Assert.assertFalse(_dbInspector.isObjectTable(null));
	}

	private static final String _COLUMN_NAME = "id";

	private static final String _COLUMN_NAME_NONEXISTING = "nonexistentColumn";

	private static final String _COLUMN_NAME_WILDCARD = "typ_Long";

	private static final String _TABLE_NAME = "DBInspectorTest";

	private static final String _TABLE_NAME_NONEXISTING = "NonexistentTable";

	private static final String _VIEW_NAME = "DBInspectorTestView";

	private static Connection _connection;
	private static DB _db;
	private static DBInspector _dbInspector;

}