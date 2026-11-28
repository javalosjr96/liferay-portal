/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.db.migration.schema.exporter.internal.exporter.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.service.ObjectDefinitionLocalServiceUtil;
import com.liferay.object.test.util.ObjectDefinitionTestUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.db.migration.schema.exporter.DBMigrationSchemaExporter;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.AssumeTestRule;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.io.File;

import java.sql.Connection;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jorge Avalos
 */
@RunWith(Arquillian.class)
public class ObjectDBMigrationSchemaExportTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new AssumeTestRule("assume"), new LiferayIntegrationTestRule());

	public static void assume() {
		Assume.assumeFalse(PropsValues.DATABASE_PARTITION_ENABLED);
	}

	@Test
	public void testExport() throws Exception {
		ObjectDefinition objectDefinition =
			ObjectDefinitionTestUtil.publishObjectDefinition();

		File folder = FileUtil.createTempFolder();

		try {
			DB db = DBManagerUtil.getDB();

			try (Connection connection = DataAccess.getConnection()) {
				db.alterTableAddColumn(
					connection, objectDefinition.getDBTableName(),
					"typeVarcharLarge", "VARCHAR(1000) null");
			}

			db.runSQL(
				StringBundler.concat(
					"create index IX_TEMP on ",
					objectDefinition.getDBTableName(),
					" (typeVarcharLarge[$COLUMN_LENGTH:1000$])"));

			_dbMigrationSchemaExporter.export(folder.getAbsolutePath());

			String indexesSQL = StringUtil.toLowerCase(
				FileUtil.read(new File(folder, "indexes.sql")));

			Assert.assertTrue(
				indexesSQL, indexesSQL.contains("left(typevarcharlarge, 676)"));
		}
		finally {
			FileUtil.deltree(folder);

			ObjectDefinitionLocalServiceUtil.deleteObjectDefinition(
				objectDefinition.getObjectDefinitionId());
		}
	}

	@Inject
	private DBMigrationSchemaExporter _dbMigrationSchemaExporter;

}