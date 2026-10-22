/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.tools.db.migration.importer.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.jdbc.DataSourceFactoryUtil;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.ProxyUtil;
import com.liferay.portal.tools.db.migration.importer.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.tools.db.migration.importer.jdbc.ConnectionConfigUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

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
public class AutoBatchPreparedStatementUtilTest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		Assume.assumeTrue(
			System.getProperty("database.postgresql.driver") != null);

		AutoBatchPreparedStatementUtil.start();

		_targetDataSource = DataSourceFactoryUtil.initDataSource(
			System.getProperty("database.postgresql.driver"),
			System.getProperty("database.postgresql.url"),
			System.getProperty("database.postgresql.username"),
			System.getProperty("database.postgresql.password"),
			StringPool.BLANK);
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		if (System.getProperty("database.postgresql.driver") == null) {
			return;
		}

		AutoBatchPreparedStatementUtil.stop();

		DataSourceFactoryUtil.destroyDataSource(_targetDataSource);
	}

	@After
	public void tearDown() throws Exception {
		ConnectionConfigUtil.setBatchSize("0");

		try (Connection connection = _targetDataSource.getConnection();

			Statement statement = connection.createStatement()) {

			statement.execute("drop table if exists TestTable");
		}
	}

	@Test
	public void testClose() throws Exception {
		int rows = _getRows();

		int id = RandomTestUtil.randomInt(
			1, (rows / _BATCH_SIZE) * _BATCH_SIZE);

		_createTable("id integer check (id <> " + id + ")");

		AtomicInteger closedCount = new AtomicInteger();
		AtomicInteger preparedCount = new AtomicInteger();

		try (Connection connection = _targetDataSource.getConnection()) {
			PreparedStatement preparedStatement =
				AutoBatchPreparedStatementUtil.concurrentAutoBatch(
					_getConnection(closedCount, connection, preparedCount),
					"insert into TestTable (id) values (?)");

			_addBatches(preparedStatement, rows);

			RuntimeException runtimeException = Assert.assertThrows(
				RuntimeException.class, preparedStatement::close);

			Assert.assertTrue(
				runtimeException.getCause() instanceof BatchUpdateException);
		}

		Assert.assertEquals(preparedCount.get(), closedCount.get());
	}

	@Test
	public void testConcurrentAutoBatch() throws Exception {
		int rows = _getRows();

		_createTable("id integer");

		AtomicInteger closedCount = new AtomicInteger();
		AtomicInteger preparedCount = new AtomicInteger();

		try (Connection connection = _targetDataSource.getConnection()) {
			try (PreparedStatement preparedStatement =
					AutoBatchPreparedStatementUtil.concurrentAutoBatch(
						_getConnection(closedCount, connection, preparedCount),
						"insert into TestTable (id) values (?)")) {

				_addBatches(preparedStatement, rows);
			}
		}

		Assert.assertEquals(
			(rows + _BATCH_SIZE - 1) / _BATCH_SIZE, preparedCount.get());
		Assert.assertEquals(preparedCount.get(), closedCount.get());
		Assert.assertEquals(rows, _getCount());
	}

	@Test
	public void testStart() throws Exception {
		ThreadPoolExecutor threadPoolExecutor =
			ReflectionTestUtil.getFieldValue(
				AutoBatchPreparedStatementUtil.class, "_executorService");

		BlockingQueue<Runnable> blockingQueue = threadPoolExecutor.getQueue();

		Assert.assertEquals(
			10, blockingQueue.remainingCapacity() + blockingQueue.size());
	}

	private void _addBatches(PreparedStatement preparedStatement, int rows)
		throws Exception {

		for (int i = 1; i <= rows; i++) {
			preparedStatement.setInt(1, i);

			preparedStatement.addBatch();
		}

		preparedStatement.executeBatch();
	}

	private void _createTable(String columnDefinition) throws Exception {
		ConnectionConfigUtil.setBatchSize(String.valueOf(_BATCH_SIZE));

		try (Connection connection = _targetDataSource.getConnection();

			Statement statement = connection.createStatement()) {

			statement.execute(
				"create table TestTable (" + columnDefinition + ")");
		}
	}

	private Connection _getConnection(
		AtomicInteger closedCount, Connection connection,
		AtomicInteger preparedCount) {

		ClassLoader classLoader =
			AutoBatchPreparedStatementUtilTest.class.getClassLoader();

		return (Connection)ProxyUtil.newProxyInstance(
			classLoader, new Class<?>[] {Connection.class},
			(proxy, method, args) -> {
				Object result = _invoke(connection, method, args);

				if (!Objects.equals(method.getName(), "prepareStatement")) {
					return result;
				}

				preparedCount.incrementAndGet();

				return ProxyUtil.newProxyInstance(
					classLoader, new Class<?>[] {PreparedStatement.class},
					(statementProxy, statementMethod, statementArgs) -> {
						if (Objects.equals(
								statementMethod.getName(), "close")) {

							closedCount.incrementAndGet();
						}

						return _invoke(result, statementMethod, statementArgs);
					});
			});
	}

	private int _getCount() throws Exception {
		try (Connection connection = _targetDataSource.getConnection();

			Statement statement = connection.createStatement();

			ResultSet resultSet = statement.executeQuery(
				"select count(*) from TestTable")) {

			resultSet.next();

			return resultSet.getInt(1);
		}
	}

	private int _getRows() {
		int rows = RandomTestUtil.randomInt(_BATCH_SIZE + 1, _BATCH_SIZE * 10);

		if ((rows % _BATCH_SIZE) == 0) {
			rows++;
		}

		return rows;
	}

	private Object _invoke(Object object, Method method, Object[] args)
		throws Throwable {

		try {
			return method.invoke(object, args);
		}
		catch (InvocationTargetException invocationTargetException) {
			throw invocationTargetException.getCause();
		}
	}

	private static final int _BATCH_SIZE = 10;

	private static DataSource _targetDataSource;

}