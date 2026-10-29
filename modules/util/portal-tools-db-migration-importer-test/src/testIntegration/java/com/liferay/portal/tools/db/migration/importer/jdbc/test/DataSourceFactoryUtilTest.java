/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.tools.db.migration.importer.jdbc.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.db.DBType;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.tools.db.migration.importer.jdbc.DataSourceFactoryUtil;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jorge Avalos
 */
@RunWith(Arquillian.class)
public class DataSourceFactoryUtilTest {

	@Test
	public void testInitDataSource() throws Exception {
		String jdbcURL = PropsUtil.get(PropsKeys.JDBC_DEFAULT_URL);

		Assume.assumeTrue(
			DataSourceFactoryUtil.isValidSourceDatabase(jdbcURL) ||
			DataSourceFactoryUtil.isValidTargetDatabase(jdbcURL));

		try (HikariDataSource hikariDataSource =
				(HikariDataSource)DataSourceFactoryUtil.initDataSource(
					jdbcURL, PropsUtil.get(PropsKeys.JDBC_DEFAULT_PASSWORD),
					PropsUtil.get(PropsKeys.JDBC_DEFAULT_USERNAME))) {

			DB db = DBManagerUtil.getDB();

			if (db.getDBType() != DBType.MYSQL) {
				Properties dataSourceProperties =
					hikariDataSource.getDataSourceProperties();

				Assert.assertNull(
					dataSourceProperties.getProperty("useCursorFetch"));

				return;
			}

			try (Connection connection = hikariDataSource.getConnection()) {
				long comStmtFetch = _getComStmtFetch(connection);

				try (PreparedStatement preparedStatement =
						connection.prepareStatement(
							"select classNameId from ClassName_")) {

					preparedStatement.setFetchSize(1);

					try (ResultSet resultSet =
							preparedStatement.executeQuery()) {

						resultSet.next();
					}
				}

				Assert.assertTrue(_getComStmtFetch(connection) > comStmtFetch);
			}
		}
	}

	private long _getComStmtFetch(Connection connection) throws Exception {
		try (PreparedStatement preparedStatement = connection.prepareStatement(
				"show session status like 'Com_stmt_fetch'");

			ResultSet resultSet = preparedStatement.executeQuery()) {

			Assert.assertTrue(resultSet.next());

			return resultSet.getLong("Value");
		}
	}

}