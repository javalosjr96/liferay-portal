/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.upgrade.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.db.DBType;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.util.InfrastructureUtil;
import com.liferay.portal.kernel.util.ProxyUtil;
import com.liferay.portal.test.log.LogCapture;
import com.liferay.portal.test.log.LogEntry;
import com.liferay.portal.test.log.LoggerTestUtil;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.upgrade.monitor.UpgradeQueryMonitor;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jorge Avalos
 */
@RunWith(Arquillian.class)
public class UpgradeQueryMonitorTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Test
	public void testStop() throws Exception {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		AtomicReference<Thread> threadReference = new AtomicReference<>();

		DataSource dataSource = InfrastructureUtil.getDataSource();

		InfrastructureUtil.setDataSource(
			_createDataSource(countDownLatch, dataSource, threadReference));

		try (LogCapture logCapture = LoggerTestUtil.configureLog4JLogger(
				"com.zaxxer.hikari.pool.ProxyConnection", LoggerTestUtil.WARN)) {

			ScheduledExecutorService scheduledExecutorService =
				Executors.newSingleThreadScheduledExecutor(
					runnable -> {
						Thread thread = new Thread(runnable);

						thread.setDaemon(true);

						threadReference.set(thread);

						return thread;
					});

			ReflectionTestUtil.setFieldValue(
				UpgradeQueryMonitor.class, "_scheduledExecutorService",
				scheduledExecutorService);

			Future<?> future = scheduledExecutorService.submit(
				() -> ReflectionTestUtil.invoke(
					UpgradeQueryMonitor.class, "_poll", new Class<?>[0]));

			Assert.assertTrue(countDownLatch.await(10, TimeUnit.SECONDS));

			UpgradeQueryMonitor.stop();

			future.get(10, TimeUnit.SECONDS);

			List<LogEntry> logEntries = logCapture.getLogEntries();

			Assert.assertEquals(logEntries.toString(), 0, logEntries.size());
		}
		finally {
			UpgradeQueryMonitor.stop();

			InfrastructureUtil.setDataSource(dataSource);
		}
	}

	private DataSource _createDataSource(
		CountDownLatch countDownLatch, DataSource dataSource,
		AtomicReference<Thread> threadReference) {

		return (DataSource)ProxyUtil.newProxyInstance(
			DataSource.class.getClassLoader(),
			new Class<?>[] {DataSource.class},
			(proxy, method, args) -> {
				if (!Objects.equals(method.getName(), "getConnection") ||
					(args != null)) {

					return method.invoke(dataSource, args);
				}

				Connection connection = dataSource.getConnection();

				Thread currentThread = Thread.currentThread();

				if ((countDownLatch.getCount() == 0) ||
					(currentThread != threadReference.get())) {

					return connection;
				}

				countDownLatch.countDown();

				try {
					_sleep(connection);
				}
				catch (SQLException sqlException) {
					connection.close();

					throw sqlException;
				}

				return connection;
			});
	}

	private void _sleep(Connection connection) throws SQLException {
		DB db = DBManagerUtil.getDB();

		DBType dbType = db.getDBType();

		if ((dbType != DBType.ORACLE) && (dbType != DBType.POSTGRESQL)) {
			return;
		}

		String sql = "select pg_sleep(2)";

		if (dbType == DBType.ORACLE) {
			sql = "begin dbms_session.sleep(2); end;";
		}

		try (Statement statement = connection.createStatement()) {
			statement.execute(sql);
		}
	}

}