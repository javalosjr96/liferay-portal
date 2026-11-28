/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.dao.db.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.string.CharPool;
import com.liferay.portal.kernel.dao.db.DBInspector;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.db.DBType;
import com.liferay.portal.kernel.dao.db.IndexMetadata;
import com.liferay.portal.kernel.util.StringUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Luis Ortiz
 */
@RunWith(Arquillian.class)
public class PostgreSQLDBTest extends DBTest {

	public static void assume() {
		db = DBManagerUtil.getDB();

		dbInspector = new DBInspector(connection);

		Assume.assumeTrue(db.getDBType() == DBType.POSTGRESQL);
	}

	@Test
	public void testAddIndexesWithLimit() throws Exception {
		db.alterTableAddColumn(
			connection, TABLE_NAME_1, "typeVarcharLarge", "VARCHAR(1000) null");

		addIndex(new String[] {"typeVarcharLarge"});

		String indexDefinition = _getIndexDefinition(INDEX_NAME);

		Assert.assertTrue(indexDefinition.contains("left("));

		List<IndexMetadata> indexMetadatas = db.getIndexMetadatas(
			connection, TABLE_NAME_1, null, false);

		Assert.assertEquals(
			indexMetadatas.toString(), 1, indexMetadatas.size());

		IndexMetadata indexMetadata = indexMetadatas.get(0);

		Assert.assertArrayEquals(
			new String[] {dbInspector.normalizeName("typeVarcharLarge")},
			indexMetadata.getColumnNames());
	}

	@Test
	public void testDropIndexes() throws Exception {
		addIndex(new String[] {_INDEX_COLUMN_NAME_LEFT_CLAUSE});

		_testDropIndexes("typeText");

		db.alterTableAddColumn(
			connection, TABLE_NAME_1, "typeVarcharLarge", "VARCHAR(1000) null");

		addIndex(new String[] {"typeVarcharLarge"});

		_testDropIndexes("typeVarcharLarge");
	}

	@Test
	public void testGetAndAddIndexWithLeftClause() throws Exception {
		addIndex(new String[] {_INDEX_COLUMN_NAME_LEFT_CLAUSE});

		List<IndexMetadata> indexMetadatas = db.getIndexMetadatas(
			connection, TABLE_NAME_1, null, false);

		_assertIndexMetadatas(indexMetadatas);

		db.dropIndexes(connection, TABLE_NAME_1, null);

		db.addIndexes(connection, indexMetadatas);

		_assertIndexMetadatas(
			db.getIndexMetadatas(connection, TABLE_NAME_1, null, false));

		db.dropIndexes(connection, TABLE_NAME_1, null);

		db.runSQL(
			connection, db.getIndexesCreateSQL(connection, indexMetadatas));

		_assertIndexMetadatas(
			db.getIndexMetadatas(connection, TABLE_NAME_1, null, false));
	}

	private void _assertIndexMetadatas(List<IndexMetadata> indexMetadatas)
		throws Exception {

		Assert.assertEquals(
			indexMetadatas.toString(), 1, indexMetadatas.size());

		IndexMetadata indexMetadata = indexMetadatas.get(0);

		Assert.assertEquals(
			dbInspector.normalizeName(INDEX_NAME),
			indexMetadata.getIndexName());

		String[] columnNames = indexMetadata.getColumnNames();

		Assert.assertEquals(
			Arrays.toString(columnNames), 1, columnNames.length);
		Assert.assertEquals(
			dbInspector.normalizeName("typeText"), columnNames[0]);
	}

	private String _getIndexDefinition(String indexName) throws Exception {
		try (PreparedStatement preparedStatement = connection.prepareStatement(
				"select indexdef from pg_indexes where indexname = ?")) {

			preparedStatement.setString(
				1, dbInspector.normalizeName(indexName));

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				Assert.assertTrue(resultSet.next());

				return StringUtil.removeChar(
					resultSet.getString("indexdef"), CharPool.QUOTE);
			}
		}
	}

	private void _testDropIndexes(String columnName) throws Exception {
		String indexDefinition = _getIndexDefinition(INDEX_NAME);

		Assert.assertTrue(indexDefinition.contains("left("));

		List<IndexMetadata> indexMetadatas = db.dropIndexes(
			connection, TABLE_NAME_1, columnName);

		Assert.assertEquals(
			indexMetadatas.toString(), 1, indexMetadatas.size());

		Assert.assertFalse(dbInspector.hasIndex(TABLE_NAME_1, INDEX_NAME));
	}

	private static final String _INDEX_COLUMN_NAME_LEFT_CLAUSE =
		"left(typeText, 15)";

}