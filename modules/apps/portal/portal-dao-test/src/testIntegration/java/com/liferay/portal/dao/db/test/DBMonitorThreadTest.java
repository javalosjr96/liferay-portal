/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.dao.db.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerMethodTestRule;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
public class DBMonitorThreadTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			PermissionCheckerMethodTestRule.INSTANCE);

	@Before
	public void setUp() throws Exception {
		_db = DBManagerUtil.getDB();

		try (Connection connection = DataAccess.getConnection();
			Statement statement = connection.createStatement()) {

			statement.execute(
				"create table testTable (id int primary key, data " +
					"VARCHAR(50))");
			statement.execute(
				"insert into testTable (id, data) values (1, 'test')");
		}
	}

	@After
	public void tearDown() throws Exception {
		if ((_connection != null) && !_connection.isClosed()) {
			_connection.rollback();
			_connection.close();
		}

		try (Connection connection = DataAccess.getConnection();
			Statement statement = connection.createStatement()) {

			statement.execute("drop table if exists testTable");
		}
	}

	@Test
	public void testGetActiveQueries() throws Exception {
		CountDownLatch countDownLatch = new CountDownLatch(1);

		_connection = DataAccess.getConnection();

		_connection.setAutoCommit(false);

		Statement statement1 = _connection.createStatement();

		statement1.executeUpdate(
			"update testTable set data='locked' where id=1");

		Thread thread = new Thread(
			() -> {
				try (Connection connection = DataAccess.getConnection();
					Statement statement2 = connection.createStatement()) {

					countDownLatch.countDown();

					statement2.executeUpdate(
						"update testTable set data='active' WHERE id=1");
				}
				catch (SQLException sqlException) {
					throw new RuntimeException(sqlException);
				}
			});

		thread.start();

		countDownLatch.await(2, TimeUnit.SECONDS);
		Thread.sleep(1000);

		List<DB.RunningQuery> activeQueries = _db.getActiveQueries(_connection);

		Assert.assertNotNull(activeQueries);

		boolean foundOurLockedQuery = false;

		for (DB.RunningQuery activeQuery : activeQueries) {
			if ((activeQuery.getQuery() != null) &&
				activeQuery.getQuery(
				).contains(
					"active"
				)) {

				foundOurLockedQuery = true;

				Assert.assertTrue(activeQuery.isLocked());

				Assert.assertNotNull(activeQuery.getSchema());
				Assert.assertFalse(
					activeQuery.getSchema(
					).isEmpty());

				Assert.assertTrue(activeQuery.getDuration() >= 0);

				break;
			}
		}

		Assert.assertTrue(foundOurLockedQuery);

		thread.join(2000);
	}

	private Connection _connection;
	private DB _db;

}