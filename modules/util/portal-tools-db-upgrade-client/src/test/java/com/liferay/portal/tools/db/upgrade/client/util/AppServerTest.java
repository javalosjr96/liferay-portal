/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.tools.db.upgrade.client.util;

import com.liferay.portal.tools.db.upgrade.client.AppServer;
import com.liferay.portal.tools.db.upgrade.client.DBUpgradeClient;

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Jorge Avalos
 */
public class AppServerTest {

	@ClassRule
	public static TemporaryFolder temporaryFolder = new TemporaryFolder();

	@BeforeClass
	public static void setUpClass() throws Exception {
		File liferayHome = temporaryFolder.getRoot();

		System.setProperty("user.dir", liferayHome.getAbsolutePath());

		_tomcatAppDir = temporaryFolder.newFolder(
			"tomcat"
		).getAbsolutePath();

		_dbUpgradeClientMockedStatic.when(
			DBUpgradeClient::getAppServerDir
		).thenReturn(
			_tomcatAppDir
		);

		_dbUpgradeClientMockedStatic.when(
			DBUpgradeClient::getLiferayHome
		).thenReturn(
			liferayHome.getAbsolutePath()
		);
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		temporaryFolder.delete();
	}

	@Test
	public void testGetDir() {
		AppServer tomcatAppServer = AppServer.getTomcatAppServer();

		File tomcatAppDir = new File(_tomcatAppDir);

		Assert.assertEquals(tomcatAppServer.getDir(), tomcatAppDir);
	}

	@Test
	public void testGetExtraLibDirs() {
		AppServer tomcatAppServer = AppServer.getTomcatAppServer();

		File extraLibDir = new File(_tomcatAppDir + "\\bin");

		Assert.assertEquals(
			tomcatAppServer.getExtraLibDirs(
			).get(
				0
			),
			extraLibDir);
	}

	@Test
	public void testGetGlobalLibDir() {
		AppServer tomcatAppServer = AppServer.getTomcatAppServer();

		File globalLibDir = new File(_tomcatAppDir + "\\lib");

		Assert.assertEquals(tomcatAppServer.getGlobalLibDir(), globalLibDir);
	}

	@Test
	public void testGetJBossEAPAppServer() throws Exception {
		AppServer jBossEAPAppServer = AppServer.getJBossEAPAppServer();

		Assert.assertEquals(
			"jboss", jBossEAPAppServer.getServerDetectorServerId());
	}

	@Test
	public void testGetPortalClassesDir() {
		AppServer tomcatAppServer = AppServer.getTomcatAppServer();

		File portalClassesDir = new File(
			_tomcatAppDir + "\\webapps\\ROOT\\WEB-INF\\classes");

		Assert.assertEquals(
			tomcatAppServer.getPortalClassesDir(), portalClassesDir);
	}

	@Test
	public void testGetPortalDir() {
		AppServer tomcatAppServer = AppServer.getTomcatAppServer();

		File portalDir = new File(_tomcatAppDir + "\\webapps\\ROOT");

		Assert.assertEquals(tomcatAppServer.getPortalDir(), portalDir);
	}

	@Test
	public void testGetPortalLibDir() {
		AppServer tomcatAppServer = AppServer.getTomcatAppServer();

		File portalLibDir = new File(
			_tomcatAppDir + "\\webapps\\ROOT\\WEB-INF\\lib");

		Assert.assertEquals(tomcatAppServer.getPortalLibDir(), portalLibDir);
	}

	@Test
	public void testGetPortalShieldedContainerLibDir() {
		AppServer tomcatAppServer = AppServer.getTomcatAppServer();

		File portalShieldContainerLibDir = new File(
			_tomcatAppDir + "\\webapps\\ROOT\\WEB-INF\\shielded-container-lib");

		Assert.assertEquals(
			tomcatAppServer.getPortalShieldedContainerLibDir(),
			portalShieldContainerLibDir);
	}

	@Test
	public void testGetTomcatServer() throws Exception {
		AppServer tomcatAppServer = AppServer.getTomcatAppServer();

		Assert.assertEquals(
			"tomcat", tomcatAppServer.getServerDetectorServerId());
	}

	@Test
	public void testGetWebLogicAppServer() throws Exception {
		AppServer webLogicAppServer = AppServer.getWebLogicAppServer();

		Assert.assertEquals(
			"weblogic", webLogicAppServer.getServerDetectorServerId());
	}

	@Test
	public void testGetWebSphereAppServer() throws Exception {
		AppServer wildFlyAppServer = AppServer.getWebSphereAppServer();

		Assert.assertEquals(
			"websphere", wildFlyAppServer.getServerDetectorServerId());
	}

	@Test
	public void testGetWildFlyAppServer() throws Exception {
		AppServer wildFlyAppServer = AppServer.getWildFlyAppServer();

		Assert.assertEquals(
			"wildfly", wildFlyAppServer.getServerDetectorServerId());
	}

	@Test
	public void testSetDirName() throws IOException {
		AppServer tomcatAppServer = AppServer.getTomcatAppServer();

		tomcatAppServer.setDirName(_LIFERAY_DIR);

		Assert.assertEquals(
			tomcatAppServer.getDir(
			).getCanonicalPath(),
			_LIFERAY_DIR);
	}

	@Test
	public void testSetExtraLibDirNames() {
		AppServer tomcatAppServer = AppServer.getTomcatAppServer();

		tomcatAppServer.setExtraLibDirNames(_LIFERAY_DIR);

		Assert.assertEquals(
			tomcatAppServer.getExtraLibDirNames(), _LIFERAY_DIR);
	}

	@Test
	public void testSetGlobalLibDirName() {
		AppServer tomcatAppServer = AppServer.getTomcatAppServer();

		tomcatAppServer.setGlobalLibDirName(_LIFERAY_DIR);

		Assert.assertEquals(
			tomcatAppServer.getGlobalLibDirName(), _LIFERAY_DIR);
	}

	@Test
	public void testSetPortalDirName() {
		AppServer tomcatAppServer = AppServer.getTomcatAppServer();

		tomcatAppServer.setPortalDirName(_LIFERAY_DIR);

		Assert.assertEquals(tomcatAppServer.getPortalDirName(), _LIFERAY_DIR);
	}

	private static final String _LIFERAY_DIR = "C:\\liferay";

	private static final MockedStatic<DBUpgradeClient>
		_dbUpgradeClientMockedStatic = Mockito.mockStatic(
			DBUpgradeClient.class);
	private static String _tomcatAppDir;

}