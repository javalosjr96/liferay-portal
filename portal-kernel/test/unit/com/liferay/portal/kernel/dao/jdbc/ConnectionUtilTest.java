/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.dao.jdbc;

import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.ProxyUtil;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Jorge Avalos
 */
public class ConnectionUtilTest {

	@Before
	public void setUp() {
		_currentConnectionUtilMockedStatic = Mockito.mockStatic(
			CurrentConnectionUtil.class);
	}

	@After
	public void tearDown() {
		_currentConnectionUtilMockedStatic.close();
	}

	@Test
	public void testGetConnection() throws Exception {
		_currentConnectionUtilMockedStatic.when(
			() -> CurrentConnectionUtil.getConnection(_dataSource)
		).thenReturn(
			_connection
		);

		Connection connection = ConnectionUtil.getConnection(_dataSource);

		Assert.assertTrue(ProxyUtil.isProxyClass(connection.getClass()));

		connection.close();

		Mockito.verify(
			_connection, Mockito.never()
		).close();

		Connection borrowedConnection = Mockito.mock(Connection.class);

		Mockito.when(
			_dataSource.getConnection()
		).thenReturn(
			borrowedConnection
		);

		_currentConnectionUtilMockedStatic.when(
			() -> CurrentConnectionUtil.getConnection(_dataSource)
		).thenReturn(
			null
		);

		Assert.assertSame(
			borrowedConnection, ConnectionUtil.getConnection(_dataSource));
	}

	@Test
	public void testGetConnectionWithSQLException() throws Exception {
		SQLException sqlException1 = new SQLException(
			RandomTestUtil.randomString());

		Mockito.when(
			_connection.prepareStatement(Mockito.anyString())
		).thenThrow(
			sqlException1
		);

		_currentConnectionUtilMockedStatic.when(
			() -> CurrentConnectionUtil.getConnection(_dataSource)
		).thenReturn(
			_connection
		);

		Connection connection = ConnectionUtil.getConnection(_dataSource);

		Assert.assertTrue(ProxyUtil.isProxyClass(connection.getClass()));

		try {
			connection.prepareStatement(RandomTestUtil.randomString());

			Assert.fail();
		}
		catch (SQLException sqlException2) {
			Assert.assertSame(sqlException1, sqlException2);
		}
	}

	private final Connection _connection = Mockito.mock(Connection.class);
	private MockedStatic<CurrentConnectionUtil>
		_currentConnectionUtilMockedStatic;
	private final DataSource _dataSource = Mockito.mock(DataSource.class);

}