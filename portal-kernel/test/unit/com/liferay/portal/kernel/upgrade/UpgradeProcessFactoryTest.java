/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.upgrade;

import com.liferay.portal.kernel.dao.db.DBType;
import com.liferay.portal.kernel.dao.db.DBTypeToSQLMap;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Jorge Avalos
 */
public class UpgradeProcessFactoryTest {

	@Test
	public void testAddColumns() {
		String columnDefinition1 = RandomTestUtil.randomString() + " LONG";
		String columnDefinition2 = RandomTestUtil.randomString() + " TEXT null";
		String tableName = RandomTestUtil.randomString();

		DescribableUpgradeStep describableUpgradeStep =
			(DescribableUpgradeStep)UpgradeProcessFactory.addColumns(
				tableName, columnDefinition1, columnDefinition2);

		Assert.assertEquals(
			HashMapBuilder.<String, Object>put(
				"columnDefinitions",
				Arrays.asList(columnDefinition1, columnDefinition2)
			).put(
				"tableName", tableName
			).build(),
			describableUpgradeStep.getArguments());
		Assert.assertEquals(
			"addColumns", describableUpgradeStep.getOperation());
	}

	@Test
	public void testAlterColumnName() {
		String newColumnName = RandomTestUtil.randomString();
		String oldColumnName = RandomTestUtil.randomString();
		String tableName = RandomTestUtil.randomString();

		DescribableUpgradeStep describableUpgradeStep =
			(DescribableUpgradeStep)UpgradeProcessFactory.alterColumnName(
				tableName, oldColumnName, newColumnName);

		Assert.assertEquals(
			HashMapBuilder.<String, Object>put(
				"newColumnName", newColumnName
			).put(
				"oldColumnName", oldColumnName
			).put(
				"tableName", tableName
			).build(),
			describableUpgradeStep.getArguments());
		Assert.assertEquals(
			"alterColumnName", describableUpgradeStep.getOperation());
	}

	@Test
	public void testAlterColumnType() {
		String columnName = RandomTestUtil.randomString();
		String newColumnType = "VARCHAR(" + RandomTestUtil.randomInt() + ")";
		String tableName = RandomTestUtil.randomString();

		DescribableUpgradeStep describableUpgradeStep =
			(DescribableUpgradeStep)UpgradeProcessFactory.alterColumnType(
				tableName, columnName, newColumnType);

		Assert.assertEquals(
			HashMapBuilder.<String, Object>put(
				"columnName", columnName
			).put(
				"newColumnType", newColumnType
			).put(
				"tableName", tableName
			).build(),
			describableUpgradeStep.getArguments());
		Assert.assertEquals(
			"alterColumnType", describableUpgradeStep.getOperation());
	}

	@Test
	public void testDropColumns() {
		String columnName1 = RandomTestUtil.randomString();
		String columnName2 = RandomTestUtil.randomString();
		String tableName = RandomTestUtil.randomString();

		DescribableUpgradeStep describableUpgradeStep =
			(DescribableUpgradeStep)UpgradeProcessFactory.dropColumns(
				tableName, columnName1, columnName2);

		Assert.assertEquals(
			HashMapBuilder.<String, Object>put(
				"columnNames", Arrays.asList(columnName1, columnName2)
			).put(
				"tableName", tableName
			).build(),
			describableUpgradeStep.getArguments());
		Assert.assertEquals(
			"dropColumns", describableUpgradeStep.getOperation());

		describableUpgradeStep =
			(DescribableUpgradeStep)UpgradeProcessFactory.dropColumns(
				tableName);

		Assert.assertEquals(
			HashMapBuilder.<String, Object>put(
				"columnNames", Collections.emptyList()
			).put(
				"tableName", tableName
			).build(),
			describableUpgradeStep.getArguments());
	}

	@Test
	public void testDropTables() {
		String tableName1 = RandomTestUtil.randomString();
		String tableName2 = RandomTestUtil.randomString();

		DescribableUpgradeStep describableUpgradeStep =
			(DescribableUpgradeStep)UpgradeProcessFactory.dropTables(
				tableName1, tableName2);

		Assert.assertEquals(
			HashMapBuilder.<String, Object>put(
				"tableNames", Arrays.asList(tableName1, tableName2)
			).build(),
			describableUpgradeStep.getArguments());
		Assert.assertEquals(
			"dropTables", describableUpgradeStep.getOperation());
	}

	@Test
	public void testRunSQL() {
		String sql1 = "delete from " + RandomTestUtil.randomString();
		String sql2 = "delete from " + RandomTestUtil.randomString();

		DescribableUpgradeStep describableUpgradeStep =
			(DescribableUpgradeStep)UpgradeProcessFactory.runSQL(sql1, sql2);

		Assert.assertEquals(
			HashMapBuilder.<String, Object>put(
				"sqls", Arrays.asList(sql1, sql2)
			).build(),
			describableUpgradeStep.getArguments());
		Assert.assertEquals("runSQL", describableUpgradeStep.getOperation());
	}

	@Test
	public void testRunSQLWithDBTypeToSQLMaps() {
		String sql1 = "delete from " + RandomTestUtil.randomString();

		DBTypeToSQLMap dbTypeToSQLMap1 = new DBTypeToSQLMap(sql1);

		String sql2 = "delete from " + RandomTestUtil.randomString();

		dbTypeToSQLMap1.add(DBType.MYSQL, sql2);

		DBTypeToSQLMap dbTypeToSQLMap2 = new DBTypeToSQLMap(null);

		DescribableUpgradeStep describableUpgradeStep =
			(DescribableUpgradeStep)UpgradeProcessFactory.runSQL(
				dbTypeToSQLMap1, dbTypeToSQLMap2);

		Map<String, Object> arguments = describableUpgradeStep.getArguments();

		Assert.assertEquals(
			HashMapBuilder.<String, Object>put(
				"dbTypeToSQLMaps",
				Arrays.asList(
					HashMapBuilder.put(
						"default", sql1
					).put(
						DBType.MYSQL.getName(), sql2
					).build(),
					Collections.singletonMap("default", null))
			).build(),
			arguments);
		Assert.assertThrows(
			UnsupportedOperationException.class,
			() -> arguments.put(RandomTestUtil.randomString(), sql1));
	}

}