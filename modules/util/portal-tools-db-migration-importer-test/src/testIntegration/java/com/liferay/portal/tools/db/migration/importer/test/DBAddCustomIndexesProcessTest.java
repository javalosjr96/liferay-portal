/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.tools.db.migration.importer.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.function.UnsafeRunnable;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.db.DBType;
import com.liferay.portal.kernel.dao.db.IndexMetadataFactoryUtil;
import com.liferay.portal.kernel.dao.jdbc.DataSourceFactoryUtil;
import com.liferay.portal.kernel.test.TestInfo;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.InfrastructureUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.tools.db.migration.importer.DBAddCustomIndexesProcess;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jorge Avalos
 */
@RunWith(Arquillian.class)
public class DBAddCustomIndexesProcessTest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		Assume.assumeTrue(
			System.getProperty("database.postgresql.driver") != null);

		_sourceDataSource = InfrastructureUtil.getDataSource();
		_targetDataSource = _initTargetDataSource(StringPool.BLANK);
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		if (System.getProperty("database.postgresql.driver") == null) {
			return;
		}

		DataSourceFactoryUtil.destroyDataSource(_targetDataSource);
	}

	@After
	public void tearDown() throws Exception {
		_sourceDB.runSQL("DROP_TABLE_IF_EXISTS(TestTable)");
		_sourceDB.runSQL("DROP_TABLE_IF_EXISTS(TestTable2)");

		_runTargetSQL("drop table if exists testtable");
		_runTargetSQL("drop table if exists testtable2");
	}

	@Test
	@TestInfo("LPD-110216")
	public void testRun() throws Exception {
		_testRun();
	}

	@Test
	@TestInfo("LPD-110216")
	public void testRunInPartition() throws Exception {
		_testInPartition(this::_testRun);
	}

	@Test
	@TestInfo("LPD-110216")
	public void testRunWithDuplicateIndexes() throws Exception {
		_createTable("TestTable");

		String indexName = _randomIndexName();

		_sourceDB.runSQL("create index " + indexName + " on TestTable (c)");
		_sourceDB.runSQL(
			"create index " + indexName + "_duplicate on TestTable (c)");

		List<String> messages = _run();

		Assert.assertEquals(messages.toString(), 0, messages.size());

		Assert.assertEquals(
			HashMapBuilder.put(
				indexName,
				_getIndexDefinition(indexName, "testtable", false, "c")
			).put(
				"testtable_pkey",
				_getIndexDefinition("testtable_pkey", "testtable", true, "id")
			).build(),
			_getIndexDefinitions());
	}

	@Test
	@TestInfo("LPD-110216")
	public void testRunWithDuplicateValues() throws Exception {
		_createTable("TestTable");

		String indexName = _randomIndexName();
		String uniqueIndexName = _randomIndexName();

		_sourceDB.runSQL("create index " + indexName + " on TestTable (c)");
		_sourceDB.runSQL(
			"create unique index " + uniqueIndexName + " on TestTable (a)");

		long id = RandomTestUtil.randomInt();
		String value = RandomTestUtil.randomString();

		_runTargetSQL(
			StringBundler.concat(
				"insert into testtable (id, a) values (", id, ", '", value,
				"'), (", id + 1, ", '", value, "')"));

		List<String> messages = _run();

		Assert.assertEquals(messages.toString(), 1, messages.size());

		String message = messages.get(0);

		Assert.assertTrue(
			message.startsWith(
				StringBundler.concat(
					"Unable to create index ", uniqueIndexName,
					" on table testtable in the default schema: ")));

		Assert.assertEquals(
			HashMapBuilder.put(
				indexName,
				_getIndexDefinition(indexName, "testtable", false, "c")
			).put(
				"testtable_pkey",
				_getIndexDefinition("testtable_pkey", "testtable", true, "id")
			).build(),
			_getIndexDefinitions());
	}

	@Test
	@TestInfo("LPD-110216")
	public void testRunWithExpression() throws Exception {
		DBType dbType = _sourceDB.getDBType();

		Assume.assumeTrue(
			(dbType == DBType.MARIADB) || (dbType == DBType.MYSQL));

		_createTable("TestTable");

		String expressionIndexName = _randomIndexName();
		String indexName = _randomIndexName();

		_sourceDB.runSQL(
			"create index " + expressionIndexName + " on TestTable (a(10))");
		_sourceDB.runSQL("create index " + indexName + " on TestTable (c)");

		List<String> messages = _run();

		Assert.assertEquals(messages.toString(), 1, messages.size());
		Assert.assertEquals(
			StringBundler.concat(
				"Unable to create index ", expressionIndexName,
				" on table testtable in the default schema because it is not ",
				"based on whole table columns"),
			messages.get(0));

		Assert.assertEquals(
			HashMapBuilder.put(
				indexName,
				_getIndexDefinition(indexName, "testtable", false, "c")
			).put(
				"testtable_pkey",
				_getIndexDefinition("testtable_pkey", "testtable", true, "id")
			).build(),
			_getIndexDefinitions());
	}

	@Test
	@TestInfo("LPD-110216")
	public void testRunWithLongName() throws Exception {
		_createTable("TestTable");

		String indexName =
			"ix_" + StringUtil.toLowerCase(RandomTestUtil.randomString(61));

		_sourceDB.runSQL("create index " + indexName + " on TestTable (c)");

		_testRunWithRename(indexName);
	}

	@Test
	@TestInfo("LPD-110216")
	public void testRunWithMissingColumn() throws Exception {
		_createTable("TestTable");

		_sourceDB.runSQL("alter table TestTable add d VARCHAR(75) null");

		String indexName = _randomIndexName();

		_sourceDB.runSQL("create index " + indexName + " on TestTable (d)");

		List<String> messages = _run();

		Assert.assertEquals(messages.toString(), 1, messages.size());
		Assert.assertEquals(
			StringBundler.concat(
				"Unable to create index ", indexName,
				" on table testtable in the default schema because it is not ",
				"based on whole table columns"),
			messages.get(0));

		Assert.assertEquals(
			HashMapBuilder.put(
				"testtable_pkey",
				_getIndexDefinition("testtable_pkey", "testtable", true, "id")
			).build(),
			_getIndexDefinitions());
	}

	@Test
	@TestInfo("LPD-110216")
	public void testRunWithUniqueIndex() throws Exception {
		_createTable("TestTable");

		String indexName = _randomIndexName();

		_runTargetSQL("create index " + indexName + " on testtable (a)");

		String uniqueIndexName = _randomIndexName();

		_sourceDB.runSQL(
			"create unique index " + uniqueIndexName + " on TestTable (a)");

		List<String> messages = _run();

		Assert.assertEquals(messages.toString(), 0, messages.size());

		Assert.assertEquals(
			HashMapBuilder.put(
				indexName,
				_getIndexDefinition(indexName, "testtable", false, "a")
			).put(
				uniqueIndexName,
				_getIndexDefinition(uniqueIndexName, "testtable", true, "a")
			).put(
				"testtable_pkey",
				_getIndexDefinition("testtable_pkey", "testtable", true, "id")
			).build(),
			_getIndexDefinitions());
	}

	@Test
	@TestInfo("LPD-110216")
	public void testRunWithUniqueTargetIndex() throws Exception {
		_createTable("TestTable");

		String targetIndexName = _randomIndexName();

		_runTargetSQL(
			"create unique index " + targetIndexName + " on testtable (c)");

		_sourceDB.runSQL(
			"create unique index " + _randomIndexName() + " on TestTable (c)");

		List<String> messages = _run();

		Assert.assertEquals(messages.toString(), 0, messages.size());

		Assert.assertEquals(
			HashMapBuilder.put(
				targetIndexName,
				_getIndexDefinition(targetIndexName, "testtable", true, "c")
			).put(
				"testtable_pkey",
				_getIndexDefinition("testtable_pkey", "testtable", true, "id")
			).build(),
			_getIndexDefinitions());
	}

	@Test
	@TestInfo("LPD-110216")
	public void testRunWithUsedName() throws Exception {
		_createTable("TestTable");

		_sourceDB.runSQL("create index TestTable on TestTable (c)");

		_testRunWithRename("TestTable");
	}

	@Test
	@TestInfo("LPD-110216")
	public void testRunWithUsedNameAndExpression() throws Exception {
		_createTable("TestTable");

		String indexName = _randomIndexName();

		_runTargetSQL(
			"create index " + indexName + " on testtable (left(a, 10))");

		_sourceDB.runSQL("create index " + indexName + " on TestTable (a)");

		Map<String, String> indexDefinitions = _getIndexDefinitions();

		List<String> messages = _run();

		Assert.assertEquals(messages.toString(), 0, messages.size());

		indexDefinitions.put(
			"testtable_pkey",
			_getIndexDefinition("testtable_pkey", "testtable", true, "id"));

		Assert.assertEquals(indexDefinitions, _getIndexDefinitions());
	}

	@Test
	@TestInfo("LPD-110216")
	public void testRunWithUsedNameAndOtherColumns() throws Exception {
		_createTable("TestTable");

		String indexName = _randomIndexName();

		_runTargetSQL("create index " + indexName + " on testtable (b)");

		_sourceDB.runSQL("create index " + indexName + " on TestTable (c)");

		_testRunWithRename(indexName);
	}

	@Test
	@TestInfo("LPD-110216")
	public void testRunWithUsedNameInPartition() throws Exception {
		_testInPartition(this::testRunWithUsedName);
	}

	@Test
	@TestInfo("LPD-110216")
	public void testRunWithUsedNameOnOtherTable() throws Exception {
		DBType dbType = _sourceDB.getDBType();

		Assume.assumeTrue(
			(dbType == DBType.MARIADB) || (dbType == DBType.MYSQL));

		_createTable("TestTable");
		_createTable("TestTable2");

		String indexName = _randomIndexName();

		_sourceDB.runSQL("create index " + indexName + " on TestTable (c)");
		_sourceDB.runSQL("create index " + indexName + " on TestTable2 (c)");

		List<String> messages = _run();

		Assert.assertEquals(messages.toString(), 1, messages.size());

		String customIndexName = StringUtil.toLowerCase(
			"custom_" +
				IndexMetadataFactoryUtil.createIndexName("testtable2", "c"));

		Assert.assertEquals(
			StringBundler.concat(
				"Created index ", customIndexName,
				" on table testtable2 instead of ", indexName,
				" in the default schema"),
			messages.get(0));

		Assert.assertEquals(
			HashMapBuilder.put(
				customIndexName,
				_getIndexDefinition(customIndexName, "testtable2", false, "c")
			).put(
				indexName,
				_getIndexDefinition(indexName, "testtable", false, "c")
			).put(
				"testtable_pkey",
				_getIndexDefinition("testtable_pkey", "testtable", true, "id")
			).put(
				"testtable2_pkey",
				_getIndexDefinition("testtable2_pkey", "testtable2", true, "id")
			).build(),
			_getIndexDefinitions());
	}

	private static DataSource _initTargetDataSource(String parameters)
		throws Exception {

		return DataSourceFactoryUtil.initDataSource(
			System.getProperty("database.postgresql.driver"),
			System.getProperty("database.postgresql.url") + parameters,
			System.getProperty("database.postgresql.username"),
			System.getProperty("database.postgresql.password"),
			StringPool.BLANK);
	}

	private void _createTable(String tableName) throws Exception {
		_sourceDB.runSQL(
			"create table " + tableName + " (id LONG not null primary key, a " +
				"VARCHAR(75) null, b VARCHAR(75) null, c VARCHAR(75) null)");

		_runTargetSQL(
			StringBundler.concat(
				"create table ", StringUtil.toLowerCase(tableName),
				" (id bigint not null, a varchar(75), b varchar(75), c ",
				"varchar(75))"));
	}

	private String _getIndexDefinition(
		String indexName, String tableName, boolean unique,
		String... columnNames) {

		String uniqueString = StringPool.BLANK;

		if (unique) {
			uniqueString = "UNIQUE ";
		}

		return StringBundler.concat(
			"CREATE ", uniqueString, "INDEX ", indexName, " ON ", tableName,
			" USING btree (",
			StringUtil.merge(columnNames, StringPool.COMMA_AND_SPACE), ")");
	}

	private Map<String, String> _getIndexDefinitions() throws Exception {
		Map<String, String> indexDefinitions = new TreeMap<>();

		try (Connection connection = _targetDataSource.getConnection();

			Statement statement = connection.createStatement();

			ResultSet resultSet = statement.executeQuery(
				"select indexname, replace(indexdef, current_schema() || " +
					"'.', '') as indexdef from pg_indexes where schemaname = " +
						"current_schema() and tablename in ('testtable', " +
							"'testtable2')")) {

			while (resultSet.next()) {
				indexDefinitions.put(
					resultSet.getString("indexname"),
					resultSet.getString("indexdef"));
			}
		}

		return indexDefinitions;
	}

	private String _randomIndexName() {
		return "ix_" + StringUtil.toLowerCase(RandomTestUtil.randomString());
	}

	private List<String> _run() {
		DBAddCustomIndexesProcess dbAddCustomIndexesProcess =
			new DBAddCustomIndexesProcess(
				_partitionName, _sourceDataSource, _targetDataSource);

		return dbAddCustomIndexesProcess.run();
	}

	private void _runTargetSQL(String sql) throws Exception {
		try (Connection connection = _targetDataSource.getConnection();

			Statement statement = connection.createStatement()) {

			statement.executeUpdate(sql);
		}
	}

	private void _testInPartition(UnsafeRunnable<Exception> unsafeRunnable)
		throws Exception {

		_partitionName = "test" + RandomTestUtil.randomInt();

		_runTargetSQL("create schema " + _partitionName);

		DataSource targetDataSource = _targetDataSource;

		_targetDataSource = _initTargetDataSource(
			"?currentSchema=" + _partitionName);

		try {
			unsafeRunnable.run();
		}
		finally {
			DataSourceFactoryUtil.destroyDataSource(_targetDataSource);

			_targetDataSource = targetDataSource;

			_runTargetSQL("drop schema " + _partitionName + " cascade");
		}
	}

	private void _testRun() throws Exception {
		_createTable("TestTable");

		String liferayIndexName = _randomIndexName();

		_runTargetSQL("create index " + liferayIndexName + " on testtable (b)");

		String indexName = _randomIndexName();
		String uniqueIndexName = _randomIndexName();

		_sourceDB.runSQL("create index " + indexName + " on TestTable (c)");
		_sourceDB.runSQL(
			"create index " + _randomIndexName() + " on TestTable (b)");
		_sourceDB.runSQL(
			"create unique index " + uniqueIndexName + " on TestTable (a, b)");

		List<String> messages = _run();

		Assert.assertEquals(messages.toString(), 0, messages.size());

		Map<String, String> indexDefinitions = HashMapBuilder.put(
			indexName, _getIndexDefinition(indexName, "testtable", false, "c")
		).put(
			liferayIndexName,
			_getIndexDefinition(liferayIndexName, "testtable", false, "b")
		).put(
			uniqueIndexName,
			_getIndexDefinition(uniqueIndexName, "testtable", true, "a", "b")
		).put(
			"testtable_pkey",
			_getIndexDefinition("testtable_pkey", "testtable", true, "id")
		).build();

		Assert.assertEquals(indexDefinitions, _getIndexDefinitions());

		messages = _run();

		Assert.assertEquals(messages.toString(), 0, messages.size());

		Assert.assertEquals(indexDefinitions, _getIndexDefinitions());
	}

	private void _testRunWithRename(String indexName) throws Exception {
		Map<String, String> indexDefinitions = _getIndexDefinitions();

		List<String> messages = _run();

		Assert.assertEquals(messages.toString(), 1, messages.size());

		String customIndexName = StringUtil.toLowerCase(
			"custom_" +
				IndexMetadataFactoryUtil.createIndexName("testtable", "c"));

		String partitionMessage = " in the default schema";

		if (_partitionName != null) {
			partitionMessage = " in partition " + _partitionName;
		}

		Assert.assertEquals(
			StringBundler.concat(
				"Created index ", customIndexName,
				" on table testtable instead of ", indexName, partitionMessage),
			messages.get(0));

		indexDefinitions.put(
			customIndexName,
			_getIndexDefinition(customIndexName, "testtable", false, "c"));
		indexDefinitions.put(
			"testtable_pkey",
			_getIndexDefinition("testtable_pkey", "testtable", true, "id"));

		Assert.assertEquals(indexDefinitions, _getIndexDefinitions());
	}

	private static DataSource _sourceDataSource;
	private static DataSource _targetDataSource;

	private String _partitionName;
	private final DB _sourceDB = DBManagerUtil.getDB();

}