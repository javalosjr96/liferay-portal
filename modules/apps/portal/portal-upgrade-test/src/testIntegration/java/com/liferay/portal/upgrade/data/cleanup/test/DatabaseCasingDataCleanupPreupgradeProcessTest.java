/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.upgrade.data.cleanup.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.lang.SafeCloseable;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.test.util.ConfigurationTestUtil;
import com.liferay.portal.db.partition.util.DBPartitionUtil;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBInspector;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.instance.PortalInstancePool;
import com.liferay.portal.kernel.model.ServiceComponent;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.ServiceComponentLocalService;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.util.PropsValuesTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.upgrade.data.cleanup.util.OrphanReferencesDataCleanupUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.HashMapDictionaryBuilder;
import com.liferay.portal.kernel.util.SetUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.test.log.LogCapture;
import com.liferay.portal.test.log.LoggerTestUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.upgrade.data.cleanup.ConfigurationDataCleanupPreupgradeProcess;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.liferay.portal.upgrade.data.cleanup.DatabaseCasingDataCleanupPreupgradeProcess;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jorge Avalos
 */
@RunWith(Arquillian.class)
public class DatabaseCasingDataCleanupPreupgradeProcessTest
	extends DatabaseCasingDataCleanupPreupgradeProcess {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		_connection = DataAccess.getConnection();
	}

	@After
	public void tearDown() throws Exception {
		DataAccess.cleanUp(_connection);
	}

	@Test
	public void testUpgrade() throws Exception {
		ServiceComponent serviceComponent =
			_serviceComponentLocalService.createServiceComponent(
				RandomTestUtil.nextLong());

		DBInspector dbInspector = new DBInspector(_connection);

		String testTableName = "TestTable";

		String testColumnName = "testColumn";

		serviceComponent.setMvccVersion(0);
		serviceComponent.setBuildNamespace("com.liferay.test.service.impl");
		serviceComponent.setData(
			StringBundler.concat(
				"<![CDATA[create table ", testTableName,
				" (	", CharPool.NEW_LINE, testColumnName," LONG"));

		_serviceComponentLocalService.addServiceComponent(serviceComponent);

		DB db = DBManagerUtil.getDB();

		try (Connection connection = DataAccess.getConnection();
			 LogCapture logCapture = LoggerTestUtil.configureLog4JLogger(
				 DatabaseCasingDataCleanupPreupgradeProcess.class.getName(),
				 LoggerTestUtil.INFO)) {

			DBPartitionUtil.forEachCompanyId(
				companyId -> db.runSQL(
					"create table testTABLE (testCOLUMN LONG)"));

			upgrade();

			List<String> messages = logCapture.getMessages();

			Assert.assertEquals(messages.toString(), 2, messages.size());

			Assert.assertTrue(
				messages.contains(
					StringBundler.concat(
						"Table ", dbInspector.normalizeName("TestTable"),
						", altered because incorrect table name casing")));

			Assert.assertTrue(
				messages.contains(
					StringBundler.concat(
						"Table ", dbInspector.normalizeName("TestTable"),
						", altered because incorrect table name casing")));

			Assert.assertTrue(
				messages.contains(
					StringBundler.concat(
						"Table ", dbInspector.normalizeName("TestTable"),
						", altered because incorrect column name casing, column: testCOLUMN")));

			DatabaseMetaData databaseMetaData = connection.getMetaData();

			try (ResultSet resultSet = databaseMetaData.getColumns(
				dbInspector.getCatalog(), dbInspector.getSchema(),dbInspector.normalizeName(testTableName), "%")) {

				while (resultSet.next()) {
					String tableName = resultSet.getString("TABLE_NAME");
					String columnName = resultSet.getString("COLUMN_NAME");

					Assert.assertEquals(tableName, testTableName, tableName);
					Assert.assertEquals(columnName, testColumnName, columnName);

				}
			}

		}
		finally {
			_serviceComponentLocalService.deleteServiceComponent(
				serviceComponent);

			DBPartitionUtil.forEachCompanyId(
				companyId -> db.runSQL(
					"DROP_TABLE_IF_EXISTS(testTABLE)"));

			DBPartitionUtil.forEachCompanyId(
				companyId -> db.runSQL(
					"DROP_TABLE_IF_EXISTS(TestTable_temp)"));

			DBPartitionUtil.forEachCompanyId(
				companyId -> db.runSQL(
					"DROP_TABLE_IF_EXISTS(TestTable)"));
		}
	}

	private Connection _connection;
	private DBInspector _dbInspector;

	@Inject
	private ServiceComponentLocalService _serviceComponentLocalService;
}