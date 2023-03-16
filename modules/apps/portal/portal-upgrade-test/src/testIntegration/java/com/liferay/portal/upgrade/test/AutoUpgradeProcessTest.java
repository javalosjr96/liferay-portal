/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.upgrade.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.model.Release;
import com.liferay.portal.kernel.model.ReleaseConstants;
import com.liferay.portal.kernel.service.ReleaseLocalService;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.upgrade.DummyUpgradeStep;
import com.liferay.portal.kernel.version.Version;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.upgrade.PortalUpgradeProcess;
import com.liferay.portal.upgrade.registry.UpgradeStepRegistrator;
import com.liferay.portal.util.PropsValues;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Alberto Chaparro
 */
@RunWith(Arquillian.class)
public class AutoUpgradeProcessTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@BeforeClass
	public static void setUpClass() throws Exception {
		try (Connection connection = DataAccess.getConnection()) {
			_currentSchemaVersion =
				PortalUpgradeProcess.getCurrentSchemaVersion(connection);
		}

		_originalUpgradeDatabaseAutoRun = ReflectionTestUtil.getFieldValue(
			PropsValues.class, "UPGRADE_DATABASE_AUTO_RUN");
	}

	@After
	public void tearDown() throws Exception {
		ReflectionTestUtil.setFieldValue(
			PropsValues.class, "UPGRADE_DATABASE_AUTO_RUN",
			_originalUpgradeDatabaseAutoRun);

		updateSchemaVersion(_currentSchemaVersion);

		if (_serviceRegistration != null) {
			_serviceRegistration.unregister();
		}

		Release release = _releaseLocalService.fetchRelease(
			_SERVLET_CONTEXT_NAME);

		if (release != null) {
			_releaseLocalService.deleteRelease(release);
		}
	}

	@Test
	public void testNewUpgradeProcessWhenAutoUpgradeDisabled()
		throws Exception {

		ReflectionTestUtil.setFieldValue(
			PropsValues.class, "UPGRADE_DATABASE_AUTO_RUN", false);

		Assert.assertEquals(
			"1.0.0", _registerNewUpgradeProcess().getSchemaVersion());
	}

	@Test
	public void testNewUpgradeProcessWhenAutoUpgradeEnabled() throws Exception {
		ReflectionTestUtil.setFieldValue(
			PropsValues.class, "UPGRADE_DATABASE_AUTO_RUN", true);

		Assert.assertEquals(
			"2.0.0", _registerNewUpgradeProcess().getSchemaVersion());
	}

	@Test
	public void testnonUpgradeProcessWhenAutoUpgradeDisabled()
		throws Exception {

		Version previousMajorSchemaVersion = new Version(
			_currentSchemaVersion.getMajor(),
			_currentSchemaVersion.getMinor() - 1, 0);

		updateSchemaVersion(previousMajorSchemaVersion);

		ReflectionTestUtil.setFieldValue(
			PropsValues.class, "UPGRADE_DATABASE_AUTO_RUN", false);

		Assert.assertEquals(
			"1.0.0", _registerNewUpgradeProcess().getSchemaVersion());
	}

	protected void updateSchemaVersion(Version version) throws SQLException {
		Connection connection = DataAccess.getConnection();

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				"update Release_ set schemaVersion = ? where " +
					"servletContextName = ?")) {

			preparedStatement.setString(1, version.toString());
			preparedStatement.setString(
				2, ReleaseConstants.DEFAULT_SERVLET_CONTEXT_NAME);

			preparedStatement.execute();
		}
	}

	private Release _registerNewUpgradeProcess() throws Exception {
		_releaseLocalService.addRelease(_SERVLET_CONTEXT_NAME, "1.0.0");

		Bundle bundle = FrameworkUtil.getBundle(AutoUpgradeProcessTest.class);

		BundleContext bundleContext = bundle.getBundleContext();

		_serviceRegistration = bundleContext.registerService(
			UpgradeStepRegistrator.class, new TestUpgradeStepRegistrator(),
			null);

		return _releaseLocalService.fetchRelease(_SERVLET_CONTEXT_NAME);
	}

	private static final String _SERVLET_CONTEXT_NAME =
		"com.liferay.portal.upgrade.test";

	private static Version _currentSchemaVersion;
	private static boolean _originalUpgradeDatabaseAutoRun;

	@Inject
	private static ReleaseLocalService _releaseLocalService;

	private ServiceRegistration<UpgradeStepRegistrator> _serviceRegistration;

	private static class TestUpgradeStepRegistrator
		implements UpgradeStepRegistrator {

		@Override
		public void register(Registry registry) {
			registry.registerInitialization();

			registry.register("1.0.0", "2.0.0", new DummyUpgradeStep());
		}

	}

}