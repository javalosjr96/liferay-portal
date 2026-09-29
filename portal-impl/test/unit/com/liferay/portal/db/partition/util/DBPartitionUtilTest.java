/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.db.partition.util;

import com.liferay.portal.kernel.dao.jdbc.CurrentConnectionUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.InfrastructureUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Map;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Jorge Avalos
 */
public class DBPartitionUtilTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() throws Exception {
		_originalDataSource = InfrastructureUtil.getDataSource();

		InfrastructureUtil.setDataSource(_dataSource);

		_currentConnectionUtilMockedStatic = Mockito.mockStatic(
			CurrentConnectionUtil.class);

		_currentConnectionUtilMockedStatic.when(
			() -> CurrentConnectionUtil.getConnection(_dataSource)
		).thenReturn(
			null
		);

		Mockito.when(
			_dataSource.getConnection()
		).thenReturn(
			_connection
		);

		Mockito.when(
			_connection.prepareStatement(Mockito.anyString())
		).thenReturn(
			_preparedStatement
		);

		Mockito.when(
			_preparedStatement.executeQuery()
		).thenReturn(
			_resultSet
		);
	}

	@After
	public void tearDown() {
		_currentConnectionUtilMockedStatic.close();

		InfrastructureUtil.setDataSource(_originalDataSource);
	}

	@Test
	public void testExportConfiguration() throws Exception {
		String configurationId = RandomTestUtil.randomString();
		String dictionary = RandomTestUtil.randomString();

		DBPartitionUtil.exportConfiguration(
			RandomTestUtil.randomLong(), configurationId, dictionary);

		Mockito.verify(
			_preparedStatement
		).setString(
			1, configurationId
		);

		Mockito.verify(
			_preparedStatement
		).setString(
			2, dictionary
		);

		Mockito.verify(
			_preparedStatement
		).executeUpdate();

		Mockito.verify(
			_connection
		).close();
	}

	@Test
	public void testExportConfigurationWithBoundConnection() throws Exception {
		Connection boundConnection = _mockBoundConnection();

		DBPartitionUtil.exportConfiguration(
			RandomTestUtil.randomLong(), RandomTestUtil.randomString(),
			RandomTestUtil.randomString());

		_assertBoundConnection(boundConnection);
	}

	@Test
	public void testExportConfigurationWithSQLException() throws Exception {
		SQLException sqlException1 = new SQLException(
			RandomTestUtil.randomString());

		Mockito.when(
			_connection.prepareStatement(Mockito.anyString())
		).thenThrow(
			sqlException1
		);

		try {
			DBPartitionUtil.exportConfiguration(
				RandomTestUtil.randomLong(), RandomTestUtil.randomString(),
				RandomTestUtil.randomString());

			Assert.fail();
		}
		catch (SQLException sqlException2) {
			Assert.assertSame(sqlException1, sqlException2);
		}

		Mockito.verify(
			_connection
		).close();
	}

	@Test
	public void testGetConfigurations() throws Exception {
		String configurationId = RandomTestUtil.randomString();
		String dictionary = RandomTestUtil.randomString();

		Mockito.when(
			_resultSet.getString("configurationId")
		).thenReturn(
			configurationId
		);

		Mockito.when(
			_resultSet.getString("dictionary")
		).thenReturn(
			dictionary
		);

		Mockito.when(
			_resultSet.next()
		).thenReturn(
			true, false
		);

		Map<String, String> configurations = DBPartitionUtil.getConfigurations(
			RandomTestUtil.randomLong());

		Assert.assertEquals(
			configurations.toString(), 1, configurations.size());
		Assert.assertEquals(dictionary, configurations.get(configurationId));

		Mockito.verify(
			_connection
		).close();
	}

	@Test
	public void testGetConfigurationsWithBoundConnection() throws Exception {
		String configurationId = RandomTestUtil.randomString();
		String dictionary = RandomTestUtil.randomString();

		Mockito.when(
			_resultSet.getString("configurationId")
		).thenReturn(
			configurationId
		);

		Mockito.when(
			_resultSet.getString("dictionary")
		).thenReturn(
			dictionary
		);

		Mockito.when(
			_resultSet.next()
		).thenReturn(
			true, false
		);

		Connection boundConnection = _mockBoundConnection();

		Map<String, String> configurations = DBPartitionUtil.getConfigurations(
			RandomTestUtil.randomLong());

		Assert.assertEquals(dictionary, configurations.get(configurationId));

		_assertBoundConnection(boundConnection);
	}

	private void _assertBoundConnection(Connection boundConnection)
		throws Exception {

		Mockito.verify(
			boundConnection
		).prepareStatement(
			Mockito.anyString()
		);

		Mockito.verify(
			boundConnection, Mockito.never()
		).close();

		Mockito.verify(
			_dataSource, Mockito.never()
		).getConnection();
	}

	private Connection _mockBoundConnection() throws Exception {
		Connection boundConnection = Mockito.mock(Connection.class);

		Mockito.when(
			boundConnection.prepareStatement(Mockito.anyString())
		).thenReturn(
			_preparedStatement
		);

		_currentConnectionUtilMockedStatic.when(
			() -> CurrentConnectionUtil.getConnection(_dataSource)
		).thenReturn(
			boundConnection
		);

		return boundConnection;
	}

	private final Connection _connection = Mockito.mock(Connection.class);
	private MockedStatic<CurrentConnectionUtil>
		_currentConnectionUtilMockedStatic;
	private final DataSource _dataSource = Mockito.mock(DataSource.class);
	private DataSource _originalDataSource;
	private final PreparedStatement _preparedStatement = Mockito.mock(
		PreparedStatement.class);
	private final ResultSet _resultSet = Mockito.mock(ResultSet.class);

}