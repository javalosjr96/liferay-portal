/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.upgrade.recorder;

import com.liferay.petra.lang.SafeCloseable;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.StringUtil;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	public void testSetAuditWriter() throws Exception {
		StringWriter stringWriter = _startWithAuditWriter();

		String upgradeStepId = RandomTestUtil.randomString();

		try (SafeCloseable safeCloseable =
				UpgradeSQLRecorder.setUpgradeStepIdWithSafeCloseable(
					upgradeStepId)) {

			int rowCount = RandomTestUtil.randomInt();

			PreparedStatement preparedStatement = Mockito.mock(
				PreparedStatement.class);

			Mockito.when(
				preparedStatement.executeUpdate()
			).thenReturn(
				rowCount
			);

			String sql = RandomTestUtil.randomString();

			PreparedStatement wrappedPreparedStatement = _prepareStatement(
				preparedStatement, sql);

			wrappedPreparedStatement.executeUpdate();

			int batchRowCount1 = RandomTestUtil.randomInt();
			int batchRowCount2 = RandomTestUtil.randomInt();

			Statement batchStatement = Mockito.mock(Statement.class);

			Mockito.when(
				batchStatement.executeBatch()
			).thenReturn(
				new int[] {batchRowCount1, batchRowCount2}
			);

			Statement wrappedBatchStatement = _createStatement(batchStatement);

			wrappedBatchStatement.executeBatch();

			Statement noInfoBatchStatement = Mockito.mock(Statement.class);

			Mockito.when(
				noInfoBatchStatement.executeBatch()
			).thenReturn(
				new int[] {
					RandomTestUtil.randomInt(), Statement.SUCCESS_NO_INFO
				}
			);

			Statement wrappedNoInfoBatchStatement = _createStatement(
				noInfoBatchStatement);

			wrappedNoInfoBatchStatement.executeBatch();

			PreparedStatement queryPreparedStatement = Mockito.mock(
				PreparedStatement.class);

			Mockito.when(
				queryPreparedStatement.executeQuery()
			).thenReturn(
				Mockito.mock(ResultSet.class)
			);

			PreparedStatement wrappedQueryPreparedStatement = _prepareStatement(
				queryPreparedStatement, RandomTestUtil.randomString());

			wrappedQueryPreparedStatement.executeQuery();

			int updateCount = RandomTestUtil.randomInt();

			PreparedStatement executePreparedStatement = Mockito.mock(
				PreparedStatement.class);

			Mockito.when(
				executePreparedStatement.execute()
			).thenReturn(
				false
			);

			Mockito.when(
				executePreparedStatement.getUpdateCount()
			).thenReturn(
				updateCount
			);

			PreparedStatement wrappedExecutePreparedStatement =
				_prepareStatement(
					executePreparedStatement, RandomTestUtil.randomString());

			wrappedExecutePreparedStatement.execute();

			List<String> lines = _getLines(stringWriter);

			Assert.assertEquals(lines.toString(), 5, lines.size());

			String line = lines.get(0);

			Assert.assertEquals(
				String.valueOf(CompanyThreadLocal.getCompanyId()),
				_getValue(line, "companyId"));
			Assert.assertEquals(
				String.valueOf(rowCount), _getValue(line, "rowCount"));
			Assert.assertEquals(sql, _getValue(line, "sql"));
			Assert.assertEquals(
				upgradeStepId, _getValue(line, "upgradeStepId"));

			Assert.assertEquals(
				String.valueOf((long)batchRowCount1 + batchRowCount2),
				_getValue(lines.get(1), "rowCount"));
			Assert.assertEquals("-1", _getValue(lines.get(2), "rowCount"));
			Assert.assertEquals("-1", _getValue(lines.get(3), "rowCount"));
			Assert.assertEquals(
				String.valueOf(updateCount),
				_getValue(lines.get(4), "rowCount"));
		}
	}

	@Test
	public void testSetAuditWriterWithControlCharacters() throws Exception {
		StringWriter stringWriter = _startWithAuditWriter();

		PreparedStatement preparedStatement = Mockito.mock(
			PreparedStatement.class);

		String sql = RandomTestUtil.randomString();

		PreparedStatement wrappedPreparedStatement = _prepareStatement(
			preparedStatement, sql + "\"\\\n\t\u0001");

		wrappedPreparedStatement.executeUpdate();

		List<String> lines = _getLines(stringWriter);

		Assert.assertEquals(lines.toString(), 1, lines.size());

		String line = lines.get(0);

		Assert.assertEquals(
			sql + "\\\"\\\\\\u000a\\u0009\\u0001", _getValue(line, "sql"));

		for (char c : line.toCharArray()) {
			Assert.assertTrue(line, c >= ' ');
		}
	}

	@Test
	public void testSetAuditWriterWithFailedStatement() throws Exception {
		StringWriter stringWriter = _startWithAuditWriter();

		PreparedStatement failingPreparedStatement = Mockito.mock(
			PreparedStatement.class);

		Mockito.when(
			failingPreparedStatement.executeUpdate()
		).thenThrow(
			new SQLException(RandomTestUtil.randomString())
		);

		PreparedStatement wrappedFailingPreparedStatement = _prepareStatement(
			failingPreparedStatement, RandomTestUtil.randomString());

		Assert.assertThrows(
			SQLException.class, wrappedFailingPreparedStatement::executeUpdate);

		PreparedStatement preparedStatement = Mockito.mock(
			PreparedStatement.class);

		Mockito.when(
			preparedStatement.execute()
		).thenReturn(
			false
		);

		Mockito.when(
			preparedStatement.getUpdateCount()
		).thenThrow(
			new SQLException(RandomTestUtil.randomString())
		);

		PreparedStatement wrappedPreparedStatement = _prepareStatement(
			preparedStatement, RandomTestUtil.randomString());

		Assert.assertFalse(wrappedPreparedStatement.execute());

		List<String> lines = _getLines(stringWriter);

		Assert.assertEquals(lines.toString(), 2, lines.size());

		Assert.assertEquals("-1", _getValue(lines.get(0), "rowCount"));
		Assert.assertEquals("-1", _getValue(lines.get(1), "rowCount"));

		List<UpgradeSQLRecorder.FailedSQL> failedSQLs =
			UpgradeSQLRecorder.getFailedSQLs();

		Assert.assertEquals(failedSQLs.toString(), 1, failedSQLs.size());
	}

	@Test
	public void testSetAuditWriterWithFailingWriter() throws Exception {
		Writer writer = Mockito.mock(Writer.class);

		Mockito.doThrow(
			new IOException(RandomTestUtil.randomString())
		).when(
			writer
		).write(
			Mockito.anyString()
		);

		UpgradeSQLRecorder.start();

		UpgradeSQLRecorder.setAuditWriter(writer);

		PreparedStatement wrappedPreparedStatement1 = _prepareStatement(
			Mockito.mock(PreparedStatement.class),
			RandomTestUtil.randomString());

		wrappedPreparedStatement1.executeUpdate();

		PreparedStatement wrappedPreparedStatement2 = _prepareStatement(
			Mockito.mock(PreparedStatement.class),
			RandomTestUtil.randomString());

		wrappedPreparedStatement2.executeUpdate();

		Mockito.verify(
			writer, Mockito.times(1)
		).write(
			Mockito.anyString()
		);
	}

	@Test
	public void testSetAuditWriterWithFileWriter() throws Exception {
		Path tempFilePath = Files.createTempFile(
			RandomTestUtil.randomString(), RandomTestUtil.randomString());

		try (Writer writer = Files.newBufferedWriter(
				tempFilePath, StandardCharsets.UTF_8)) {

			UpgradeSQLRecorder.start();

			UpgradeSQLRecorder.setAuditWriter(writer);

			String sql = RandomTestUtil.randomString();

			PreparedStatement wrappedPreparedStatement = _prepareStatement(
				Mockito.mock(PreparedStatement.class), sql);

			wrappedPreparedStatement.executeUpdate();

			List<String> lines = Files.readAllLines(tempFilePath);

			Assert.assertEquals(lines.toString(), 1, lines.size());
			Assert.assertEquals(sql, _getValue(lines.get(0), "sql"));
		}
		finally {
			Files.delete(tempFilePath);
		}
	}

	@Test
	public void testSetAuditWriterWithNullWriter() throws Exception {
		UpgradeSQLRecorder.start();

		UpgradeSQLRecorder.setAuditWriter(null);

		PreparedStatement preparedStatement = Mockito.mock(
			PreparedStatement.class);

		Mockito.when(
			preparedStatement.execute()
		).thenReturn(
			false
		);

		PreparedStatement wrappedPreparedStatement = _prepareStatement(
			preparedStatement, RandomTestUtil.randomString());

		Assert.assertFalse(wrappedPreparedStatement.execute());

		Mockito.verify(
			preparedStatement, Mockito.never()
		).getUpdateCount();
	}

	@Test
	public void testSetUpgradeStepIdWithSafeCloseable() {
		String upgradeStepId1 = RandomTestUtil.randomString();
		String upgradeStepId2 = RandomTestUtil.randomString();

		try (SafeCloseable safeCloseable1 =
				UpgradeSQLRecorder.setUpgradeStepIdWithSafeCloseable(
					upgradeStepId1)) {

			try (SafeCloseable safeCloseable2 =
					UpgradeSQLRecorder.setUpgradeStepIdWithSafeCloseable(
						upgradeStepId2)) {

				Assert.assertEquals(
					upgradeStepId2, UpgradeSQLRecorder.getUpgradeStepId());
			}

			Assert.assertEquals(
				upgradeStepId1, UpgradeSQLRecorder.getUpgradeStepId());
		}

		Assert.assertEquals(
			StringPool.BLANK, UpgradeSQLRecorder.getUpgradeStepId());
	}

	@Test
	public void testStop() throws Exception {
		StringWriter stringWriter = _startWithAuditWriter();

		PreparedStatement wrappedPreparedStatement = _prepareStatement(
			Mockito.mock(PreparedStatement.class),
			RandomTestUtil.randomString());

		UpgradeSQLRecorder.stop();

		wrappedPreparedStatement.executeUpdate();

		Assert.assertEquals(StringPool.BLANK, stringWriter.toString());
	}

	private Statement _createStatement(Statement statement)
		throws SQLException {

		Connection connection = Mockito.mock(Connection.class);

		Mockito.when(
			connection.createStatement()
		).thenReturn(
			statement
		);

		Connection wrappedConnection = UpgradeSQLRecorder.getConnectionWrapper(
			connection);

		return wrappedConnection.createStatement();
	}

	private List<String> _getLines(StringWriter stringWriter) {
		return ListUtil.fromArray(
			StringUtil.split(stringWriter.toString(), CharPool.NEW_LINE));
	}

	private String _getValue(String line, String name) {
		Pattern pattern = Pattern.compile(
			"\"" + name + "\":(\"((?:[^\"\\\\]|\\\\.)*)\"|-?\\d+)");

		Matcher matcher = pattern.matcher(line);

		Assert.assertTrue(line, matcher.find());

		if (matcher.group(2) != null) {
			return matcher.group(2);
		}

		return matcher.group(1);
	}

	private PreparedStatement _prepareStatement(
			PreparedStatement preparedStatement, String sql)
		throws SQLException {

		Connection connection = Mockito.mock(Connection.class);

		Mockito.when(
			connection.prepareStatement(sql)
		).thenReturn(
			preparedStatement
		);

		Connection wrappedConnection = UpgradeSQLRecorder.getConnectionWrapper(
			connection);

		return wrappedConnection.prepareStatement(sql);
	}

	private StringWriter _startWithAuditWriter() {
		StringWriter stringWriter = new StringWriter();

		UpgradeSQLRecorder.start();

		UpgradeSQLRecorder.setAuditWriter(stringWriter);

		return stringWriter;
	}

}