/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.upgrade.data.cleanup.util;

import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.upgrade.recorder.UpgradeSQLRecorder;
import com.liferay.portal.test.log.LogCapture;
import com.liferay.portal.test.log.LogEntry;
import com.liferay.portal.test.log.LoggerTestUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.List;
import java.util.Objects;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import org.mockito.Mockito;

/**
 * @author Jorge Avalos
 */
public class OrphanReferencesDataCleanupUtilTest {

	@After
	public void tearDown() {
		UpgradeSQLRecorder.stop();
	}

	@Test
	public void testExecuteDeleteWithRetry() throws Exception {
		_testExecuteDeleteWithRetry(LoggerTestUtil.DEBUG);
		_testExecuteDeleteWithRetry(LoggerTestUtil.INFO);
	}

	@Test
	public void testExecuteDeleteWithRetryAndWithoutRetry() throws Exception {
		PreparedStatement preparedStatement = Mockito.mock(
			PreparedStatement.class);

		Mockito.doThrow(
			new SQLException(
				RandomTestUtil.randomString(), _SQL_STATE_DEADLOCK),
			new SQLException(
				RandomTestUtil.randomString(), _SQL_STATE_CONSTRAINT_VIOLATION)
		).when(
			preparedStatement
		).executeUpdate();

		try {
			_invokeExecuteDelete(
				RandomTestUtil.randomString(), preparedStatement);

			Assert.fail();
		}
		catch (Exception exception) {
			SQLException sqlException = (SQLException)exception;

			Assert.assertEquals(
				_SQL_STATE_CONSTRAINT_VIOLATION, sqlException.getSQLState());
		}

		List<UpgradeSQLRecorder.FailedSQL> failedSQLs =
			UpgradeSQLRecorder.getFailedSQLs();

		Assert.assertEquals(failedSQLs.toString(), 2, failedSQLs.size());
	}

	@Test
	public void testExecuteDeleteWithRetryExhausted() throws Exception {
		PreparedStatement preparedStatement = Mockito.mock(
			PreparedStatement.class);

		Mockito.doThrow(
			new SQLException(RandomTestUtil.randomString(), _SQL_STATE_DEADLOCK)
		).when(
			preparedStatement
		).executeUpdate();

		int maxRetries = ReflectionTestUtil.getFieldValue(
			OrphanReferencesDataCleanupUtil.class,
			"_DELETE_DEADLOCK_MAX_RETRIES");

		try (LogCapture logCapture = LoggerTestUtil.configureLog4JLogger(
				OrphanReferencesDataCleanupUtil.class.getName(),
				LoggerTestUtil.WARN)) {

			logCapture.resetPriority(LoggerTestUtil.INFO);

			try {
				_invokeExecuteDelete(
					RandomTestUtil.randomString(), preparedStatement);

				Assert.fail();
			}
			catch (Exception exception) {
				SQLException sqlException = (SQLException)exception;

				Assert.assertEquals(
					_SQL_STATE_DEADLOCK, sqlException.getSQLState());
			}

			List<LogEntry> logEntries = logCapture.getLogEntries();

			Assert.assertEquals(
				logEntries.toString(), maxRetries, logEntries.size());

			for (LogEntry logEntry : logEntries) {
				Assert.assertEquals(
					LoggerTestUtil.INFO, logEntry.getPriority());
			}
		}

		int expectedAttempts = maxRetries + 1;

		Mockito.verify(
			preparedStatement, Mockito.times(expectedAttempts)
		).executeUpdate();

		List<UpgradeSQLRecorder.FailedSQL> failedSQLs =
			UpgradeSQLRecorder.getFailedSQLs();

		Assert.assertEquals(
			failedSQLs.toString(), expectedAttempts, failedSQLs.size());
	}

	@Test
	public void testExecuteDeleteWithoutRetry() throws Exception {
		PreparedStatement preparedStatement = Mockito.mock(
			PreparedStatement.class);

		Mockito.doThrow(
			new SQLException(
				RandomTestUtil.randomString(), _SQL_STATE_CONSTRAINT_VIOLATION)
		).when(
			preparedStatement
		).executeUpdate();

		try {
			_invokeExecuteDelete(
				RandomTestUtil.randomString(), preparedStatement);

			Assert.fail();
		}
		catch (Exception exception) {
			SQLException sqlException = (SQLException)exception;

			Assert.assertEquals(
				_SQL_STATE_CONSTRAINT_VIOLATION, sqlException.getSQLState());
		}

		Mockito.verify(
			preparedStatement, Mockito.times(1)
		).executeUpdate();

		List<UpgradeSQLRecorder.FailedSQL> failedSQLs =
			UpgradeSQLRecorder.getFailedSQLs();

		Assert.assertEquals(failedSQLs.toString(), 1, failedSQLs.size());
	}

	private void _invokeExecuteDelete(
			String deleteSQL, PreparedStatement preparedStatement)
		throws Exception {

		Connection connection = Mockito.mock(Connection.class);

		Mockito.when(
			connection.prepareStatement(Mockito.anyString())
		).thenReturn(
			preparedStatement
		);

		UpgradeSQLRecorder.start();

		ReflectionTestUtil.invoke(
			OrphanReferencesDataCleanupUtil.class, "_executeDelete",
			new Class<?>[] {Connection.class, String.class},
			UpgradeSQLRecorder.getConnectionWrapper(connection), deleteSQL);
	}

	private void _testExecuteDeleteWithRetry(String priority) throws Exception {
		PreparedStatement preparedStatement = Mockito.mock(
			PreparedStatement.class);

		SQLException[] sqlExceptions = {
			new SQLException(
				RandomTestUtil.randomString(), _SQL_STATE_DEADLOCK),
			new SQLException(RandomTestUtil.randomString(), _SQL_STATE_DEADLOCK)
		};

		Mockito.doThrow(
			sqlExceptions
		).doReturn(
			RandomTestUtil.randomInt()
		).when(
			preparedStatement
		).executeUpdate();

		try (LogCapture logCapture = LoggerTestUtil.configureLog4JLogger(
				OrphanReferencesDataCleanupUtil.class.getName(),
				LoggerTestUtil.WARN)) {

			logCapture.resetPriority(priority);

			String deleteSQL = RandomTestUtil.randomString();

			_invokeExecuteDelete(deleteSQL, preparedStatement);

			List<LogEntry> logEntries = logCapture.getLogEntries();

			Assert.assertEquals(
				logEntries.toString(), sqlExceptions.length, logEntries.size());

			for (int i = 0; i < sqlExceptions.length; i++) {
				LogEntry logEntry = logEntries.get(i);

				Assert.assertEquals(
					"Retrying delete after a database deadlock: " + deleteSQL,
					logEntry.getMessage());
				Assert.assertEquals(priority, logEntry.getPriority());

				if (Objects.equals(priority, LoggerTestUtil.DEBUG)) {
					Assert.assertSame(
						sqlExceptions[i], logEntry.getThrowable());
				}
				else {
					Assert.assertNull(logEntry.getThrowable());
				}
			}
		}

		Mockito.verify(
			preparedStatement, Mockito.times(sqlExceptions.length + 1)
		).executeUpdate();

		List<UpgradeSQLRecorder.FailedSQL> failedSQLs =
			UpgradeSQLRecorder.getFailedSQLs();

		Assert.assertEquals(failedSQLs.toString(), 0, failedSQLs.size());
	}

	private static final String _SQL_STATE_CONSTRAINT_VIOLATION = "23000";

	private static final String _SQL_STATE_DEADLOCK = "40001";

}