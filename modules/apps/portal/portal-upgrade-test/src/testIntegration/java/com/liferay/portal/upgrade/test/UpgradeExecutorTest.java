/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.upgrade.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.lang.SafeCloseable;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBInspector;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.Release;
import com.liferay.portal.kernel.model.ReleaseConstants;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.ReleaseLocalService;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.util.PropsValuesTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.upgrade.DummyUpgradeStep;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.test.log.LogCapture;
import com.liferay.portal.test.log.LoggerTestUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.upgrade.registry.UpgradeStepRegistrator;

import java.io.File;

import java.nio.file.Files;

import java.sql.Connection;
import java.sql.PreparedStatement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

/**
 * @author István András Dézsi
 */
@RunWith(Arquillian.class)
public class UpgradeExecutorTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@After
	public void tearDown() {
		if (_serviceRegistration != null) {
			_serviceRegistration.unregister();
		}
	}

	@Test
	public void testExecuteUpgradeInfosWithInterruptedUpgrade()
		throws Exception {

		Bundle bundle = FrameworkUtil.getBundle(UpgradeExecutorTest.class);

		String bundleSymbolicName = bundle.getSymbolicName();

		_testExecuteUpgradeInfos(
			bundle, ReleaseConstants.STATE_IN_PROGRESS,
			StringBundler.concat(
				"Resuming the upgrade of ", bundleSymbolicName,
				" from schema version 1.0.0 after an interruption"));
	}

	@Test
	public void testExecuteUpgradeInfosWithSQLAudit() throws Exception {
		Bundle bundle = FrameworkUtil.getBundle(UpgradeExecutorTest.class);

		String bundleSymbolicName = bundle.getSymbolicName();

		_releaseLocalService.addRelease(bundleSymbolicName, "1.0.0");

		int rowCount = RandomTestUtil.randomInt(2, 10);

		UpgradeProcess insertUpgradeProcess = new UpgradeProcess() {

			@Override
			protected void doUpgrade() throws Exception {
				runSQL(
					"create table UpgradeSQLAuditTest (id_ LONG not null " +
						"primary key, value INTEGER)");

				try (PreparedStatement preparedStatement =
						AutoBatchPreparedStatementUtil.autoBatch(
							connection,
							"insert into UpgradeSQLAuditTest (id_, value) " +
								"values (?, 0)")) {

					for (int i = 0; i < rowCount; i++) {
						preparedStatement.setLong(1, i);

						preparedStatement.addBatch();
					}

					preparedStatement.executeBatch();
				}
			}

		};

		AtomicLong companyIdAtomicLong = new AtomicLong();

		UpgradeProcess updateUpgradeProcess = new UpgradeProcess() {

			@Override
			protected void doUpgrade() throws Exception {
				companyIdAtomicLong.set(CompanyThreadLocal.getCompanyId());

				runSQL("update UpgradeSQLAuditTest set value = 1");

				processConcurrently(
					"select id_ from UpgradeSQLAuditTest where id_ = 0",
					"update UpgradeSQLAuditTest set value = 2 where id_ = ?",
					resultSet -> new Object[] {resultSet.getLong(1)},
					(values, preparedStatement) -> {
						preparedStatement.setLong(1, (Long)values[0]);

						preparedStatement.addBatch();
					},
					"Unable to update UpgradeSQLAuditTest");

				try (PreparedStatement preparedStatement =
						AutoBatchPreparedStatementUtil.concurrentAutoBatch(
							connection,
							"update UpgradeSQLAuditTest set value = 3 where " +
								"id_ = ?")) {

					preparedStatement.setLong(1, 0);

					preparedStatement.addBatch();

					preparedStatement.executeBatch();
				}

				try {
					runSQL("update UpgradeSQLAuditTestMissing set value = 4");
				}
				catch (Exception exception) {
				}
			}

		};

		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"UPGRADE_REPORT_SQL_AUDIT_ENABLED", true)) {

			BundleContext bundleContext = bundle.getBundleContext();

			try (SafeCloseable autoRunSafeCloseable =
					PropsValuesTestUtil.swapWithSafeCloseable(
						"UPGRADE_DATABASE_AUTO_RUN", false, false)) {

				_serviceRegistration = bundleContext.registerService(
					UpgradeStepRegistrator.class,
					upgradeStepRegistry -> upgradeStepRegistry.register(
						"1.0.0", "2.0.0", insertUpgradeProcess,
						updateUpgradeProcess),
					null);
			}

			ReflectionTestUtil.invoke(
				_upgradeRecorder, "start", new Class<?>[0]);

			try {
				ReflectionTestUtil.invoke(
					_upgradeExecutor, "executeUpgradeInfos",
					new Class<?>[] {Bundle.class, List.class}, bundle,
					_getUpgradeInfos(bundleSymbolicName));
			}
			finally {
				ReflectionTestUtil.invoke(
					_upgradeRecorder, "stop", new Class<?>[0]);
			}

			Map<String, List<JSONObject>> jsonObjectsMap = _getJSONObjectsMap(
				bundleSymbolicName);

			List<JSONObject> jsonObjects = jsonObjectsMap.get(
				bundleSymbolicName + ":1.0.0:2.0.0:0");

			Assert.assertNotNull(jsonObjectsMap.toString(), jsonObjects);

			_getJSONObject(jsonObjects, "insert into UpgradeSQLAuditTest");

			jsonObjects = jsonObjectsMap.get(
				bundleSymbolicName + ":1.0.0:2.0.0:1");

			Assert.assertNotNull(jsonObjectsMap.toString(), jsonObjects);

			JSONObject jsonObject = _getJSONObject(jsonObjects, "value = 1");

			Assert.assertEquals(rowCount, jsonObject.getLong("rowCount"));

			jsonObject = _getJSONObject(jsonObjects, "value = 2");

			Assert.assertEquals(1, jsonObject.getLong("rowCount"));

			jsonObject = _getJSONObject(jsonObjects, "value = 3");

			Assert.assertEquals(
				companyIdAtomicLong.get(), jsonObject.getLong("companyId"));
			Assert.assertEquals(1, jsonObject.getLong("rowCount"));

			jsonObject = _getJSONObject(
				jsonObjects, "UpgradeSQLAuditTestMissing");

			Assert.assertEquals(-1, jsonObject.getLong("rowCount"));
		}
		finally {
			try (Connection connection = DataAccess.getConnection()) {
				DBInspector dbInspector = new DBInspector(connection);

				if (dbInspector.hasTable("UpgradeSQLAuditTest")) {
					DB db = DBManagerUtil.getDB();

					db.runSQL("drop table UpgradeSQLAuditTest");
				}
			}

			Release release = _releaseLocalService.fetchRelease(
				bundleSymbolicName);

			if (release != null) {
				_releaseLocalService.deleteRelease(release);
			}
		}
	}

	@Test
	public void testExecuteWithFailedRegistration() throws Exception {
		Bundle bundle = FrameworkUtil.getBundle(UpgradeExecutorTest.class);

		String bundleSymbolicName = bundle.getSymbolicName();

		_registerUpgradeStepRegistrator(bundle);

		try {
			Set<String> bundleSymbolicNames = ReflectionTestUtil.invoke(
				_upgradeExecutor, "getBundleSymbolicNames", new Class<?>[0]);

			Assert.assertTrue(
				bundleSymbolicNames.toString(),
				bundleSymbolicNames.contains(bundleSymbolicName));

			Set<String> failedBundleSymbolicNames = ReflectionTestUtil.invoke(
				_upgradeExecutor, "getFailedBundleSymbolicNames",
				new Class<?>[0]);

			Assert.assertTrue(
				failedBundleSymbolicNames.toString(),
				failedBundleSymbolicNames.contains(bundleSymbolicName));

			ReflectionTestUtil.invoke(
				_upgradeExecutor, "execute",
				new Class<?>[] {Bundle.class, List.class}, bundle,
				_getUpgradeInfos(bundleSymbolicName));

			failedBundleSymbolicNames = ReflectionTestUtil.invoke(
				_upgradeExecutor, "getFailedBundleSymbolicNames",
				new Class<?>[0]);

			Assert.assertFalse(
				failedBundleSymbolicNames.toString(),
				failedBundleSymbolicNames.contains(bundleSymbolicName));
		}
		finally {
			Release release = _releaseLocalService.fetchRelease(
				bundleSymbolicName);

			if (release != null) {
				_releaseLocalService.deleteRelease(release);
			}
		}
	}

	@Test
	public void testGetUpgradeInfosWithFailedRegistration() {
		Bundle bundle = FrameworkUtil.getBundle(UpgradeExecutorTest.class);

		String bundleSymbolicName = bundle.getSymbolicName();

		AtomicInteger registerCount = _registerUpgradeStepRegistrator(bundle);

		List<?> upgradeInfos = _getUpgradeInfos(bundleSymbolicName);

		Assert.assertEquals(2, registerCount.get());
		Assert.assertEquals(upgradeInfos.toString(), 1, upgradeInfos.size());

		Set<String> failedBundleSymbolicNames = ReflectionTestUtil.invoke(
			_upgradeExecutor, "getFailedBundleSymbolicNames", new Class<?>[0]);

		Assert.assertTrue(
			failedBundleSymbolicNames.toString(),
			failedBundleSymbolicNames.contains(bundleSymbolicName));

		_serviceRegistration.unregister();

		_serviceRegistration = null;

		failedBundleSymbolicNames = ReflectionTestUtil.invoke(
			_upgradeExecutor, "getFailedBundleSymbolicNames", new Class<?>[0]);

		Assert.assertFalse(
			failedBundleSymbolicNames.toString(),
			failedBundleSymbolicNames.contains(bundleSymbolicName));

		Release release = _releaseLocalService.addRelease(
			bundleSymbolicName, "1.0.0");

		Class<?> clazz = _upgradeExecutor.getClass();

		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"UPGRADE_DATABASE_AUTO_RUN", false, false);
			LogCapture logCapture = LoggerTestUtil.configureLog4JLogger(
				clazz.getName(), LoggerTestUtil.OFF)) {

			_registerUpgradeStepRegistrator(bundle);

			try {
				_getUpgradeInfos(bundleSymbolicName);

				Assert.fail();
			}
			catch (IllegalStateException illegalStateException) {
			}

			failedBundleSymbolicNames = ReflectionTestUtil.invoke(
				_upgradeExecutor, "getFailedBundleSymbolicNames",
				new Class<?>[0]);

			Assert.assertTrue(
				failedBundleSymbolicNames.toString(),
				failedBundleSymbolicNames.contains(bundleSymbolicName));
		}
		finally {
			_releaseLocalService.deleteRelease(release);
		}
	}

	private JSONObject _getJSONObject(
		List<JSONObject> jsonObjects, String sql) {

		for (JSONObject jsonObject : jsonObjects) {
			String jsonObjectSQL = jsonObject.getString("sql");

			if (!jsonObjectSQL.contains(sql)) {
				continue;
			}

			return jsonObject;
		}

		throw new AssertionError(jsonObjects.toString());
	}

	private Map<String, List<JSONObject>> _getJSONObjectsMap(
			String bundleSymbolicName)
		throws Exception {

		Map<String, List<JSONObject>> jsonObjectsMap = new HashMap<>();

		File reportsDir = new File(PropsValues.LIFERAY_HOME, "reports");

		if (Validator.isNotNull(PropsValues.UPGRADE_REPORT_DIR)) {
			reportsDir = new File(PropsValues.UPGRADE_REPORT_DIR);
		}

		for (String line :
				Files.readAllLines(
					new File(reportsDir, "upgrade_sql_audit.jsonl").toPath())) {

			JSONObject jsonObject = JSONFactoryUtil.createJSONObject(line);

			String upgradeStepId = jsonObject.getString("upgradeStepId");

			if (!upgradeStepId.startsWith(bundleSymbolicName)) {
				continue;
			}

			List<JSONObject> jsonObjects = jsonObjectsMap.computeIfAbsent(
				upgradeStepId, key -> new ArrayList<>());

			jsonObjects.add(jsonObject);
		}

		return jsonObjectsMap;
	}

	private List<?> _getUpgradeInfos(String bundleSymbolicName) {
		return ReflectionTestUtil.invoke(
			_upgradeExecutor, "getUpgradeInfos", new Class<?>[] {String.class},
			bundleSymbolicName);
	}

	private AtomicInteger _registerUpgradeStepRegistrator(Bundle bundle) {
		AtomicInteger registerCount = new AtomicInteger();

		BundleContext bundleContext = bundle.getBundleContext();

		Class<?> clazz = _upgradeExecutor.getClass();

		try (LogCapture logCapture = LoggerTestUtil.configureLog4JLogger(
				clazz.getName(), LoggerTestUtil.OFF)) {

			_serviceRegistration = bundleContext.registerService(
				UpgradeStepRegistrator.class,
				upgradeStepRegistry -> {
					upgradeStepRegistry.register(
						"0.0.0", "1.0.0", new DummyUpgradeStep());

					if (registerCount.incrementAndGet() == 1) {
						throw new IllegalStateException();
					}
				},
				null);
		}

		return registerCount;
	}

	private void _testExecuteUpgradeInfos(
			Bundle bundle, int state, String... expectedMessages)
		throws Exception {

		String bundleSymbolicName = bundle.getSymbolicName();

		Release release = _releaseLocalService.addRelease(
			bundleSymbolicName, "1.0.0");

		release.setState(state);

		release = _releaseLocalService.updateRelease(release);

		try {
			BundleContext bundleContext = bundle.getBundleContext();

			try (SafeCloseable safeCloseable =
					PropsValuesTestUtil.swapWithSafeCloseable(
						"UPGRADE_DATABASE_AUTO_RUN", false, false)) {

				_serviceRegistration = bundleContext.registerService(
					UpgradeStepRegistrator.class,
					upgradeStepRegistry -> upgradeStepRegistry.register(
						"1.0.0", "2.0.0", new DummyUpgradeStep()),
					null);
			}

			Class<?> clazz = _upgradeExecutor.getClass();

			try (LogCapture logCapture = LoggerTestUtil.configureLog4JLogger(
					clazz.getName(), LoggerTestUtil.WARN)) {

				ReflectionTestUtil.invoke(
					_upgradeExecutor, "executeUpgradeInfos",
					new Class<?>[] {Bundle.class, List.class}, bundle,
					_getUpgradeInfos(bundleSymbolicName));

				Assert.assertEquals(
					Arrays.asList(expectedMessages), logCapture.getMessages());
			}

			Release persistedRelease = _releaseLocalService.fetchRelease(
				bundleSymbolicName);

			Assert.assertEquals("2.0.0", persistedRelease.getSchemaVersion());
			Assert.assertEquals(
				ReleaseConstants.STATE_GOOD, persistedRelease.getState());
		}
		finally {
			release = _releaseLocalService.fetchRelease(bundleSymbolicName);

			if (release != null) {
				_releaseLocalService.deleteRelease(release);
			}
		}
	}

	@Inject
	private ReleaseLocalService _releaseLocalService;

	private ServiceRegistration<UpgradeStepRegistrator> _serviceRegistration;

	@Inject(
		filter = "component.name=com.liferay.portal.upgrade.internal.executor.UpgradeExecutor",
		type = Inject.NoType.class
	)
	private Object _upgradeExecutor;

	@Inject(
		filter = "component.name=com.liferay.portal.upgrade.internal.recorder.UpgradeRecorder",
		type = Inject.NoType.class
	)
	private Object _upgradeRecorder;

}