/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.upgrade.recorder;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.test.util.RandomTestUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import org.mockito.Mockito;

/**
 * @author Jorge Avalos
 */
public class UpgradeSQLRecorderTest {

	@After
	public void tearDown() {
		UpgradeSQLRecorder.stop();
	}

	@Test
	public void testGetFailedSQLs() throws Exception {
		UpgradeSQLRecorder.start();

		Connection connection = Mockito.mock(Connection.class);
		PreparedStatement preparedStatement = Mockito.mock(
			PreparedStatement.class);

		Mockito.when(
			connection.prepareStatement(Mockito.anyString())
		).thenReturn(
			preparedStatement
		);

		Mockito.when(
			preparedStatement.executeUpdate()
		).thenThrow(
			new SQLException()
		);

		_executeUpdate(UpgradeSQLRecorder.getConnectionWrapper(connection));

		List<UpgradeSQLRecorder.FailedSQL> failedSQLs =
			UpgradeSQLRecorder.getFailedSQLs();

		Assert.assertEquals(failedSQLs.toString(), 1, failedSQLs.size());

		UpgradeSQLRecorder.FailedSQL failedSQL = failedSQLs.get(0);

		Assert.assertEquals(StringPool.BLANK, failedSQL.getMessage());
	}

	@Test
	public void testRemoveFailedSQL() throws Exception {
		UpgradeSQLRecorder.start();

		Connection connection = Mockito.mock(Connection.class);
		PreparedStatement preparedStatement1 = Mockito.mock(
			PreparedStatement.class);
		PreparedStatement preparedStatement2 = Mockito.mock(
			PreparedStatement.class);

		Mockito.when(
			connection.prepareStatement(Mockito.anyString())
		).thenReturn(
			preparedStatement1, preparedStatement2
		);

		String message = RandomTestUtil.randomString();

		SQLException sqlException = new SQLException(message);

		Mockito.when(
			preparedStatement1.executeUpdate()
		).thenThrow(
			sqlException
		);

		Mockito.when(
			preparedStatement2.executeUpdate()
		).thenThrow(
			new SQLException(message)
		);

		Connection wrappedConnection = UpgradeSQLRecorder.getConnectionWrapper(
			connection);

		_executeUpdate(wrappedConnection);
		_executeUpdate(wrappedConnection);

		List<UpgradeSQLRecorder.FailedSQL> failedSQLs =
			UpgradeSQLRecorder.getFailedSQLs();

		Assert.assertEquals(failedSQLs.toString(), 2, failedSQLs.size());

		UpgradeSQLRecorder.FailedSQL failedSQL = failedSQLs.get(1);

		UpgradeSQLRecorder.removeFailedSQL(sqlException);

		Assert.assertEquals(failedSQLs.toString(), 1, failedSQLs.size());
		Assert.assertSame(failedSQL, failedSQLs.get(0));

		UpgradeSQLRecorder.removeFailedSQL(sqlException);

		Assert.assertEquals(failedSQLs.toString(), 1, failedSQLs.size());
		Assert.assertSame(failedSQL, failedSQLs.get(0));
	}

	private void _executeUpdate(Connection connection) throws Exception {
		PreparedStatement preparedStatement = connection.prepareStatement(
			RandomTestUtil.randomString());

		Assert.assertThrows(
			SQLException.class, preparedStatement::executeUpdate);
	}

}