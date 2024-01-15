package com.liferay.portal.tools.db.upgrade.client.util;


import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.tools.db.upgrade.client.AppServer;
import org.junit.Assert;
import org.junit.Test;

public class AppServerTest {

	@Test
	public void testGetTomcatServer() throws Exception {
		AppServer tomcatAppServer = AppServer.getTomcatAppServer();
		Assert.assertEquals(tomcatAppServer.getServerDetectorServerId(),"tomcat");
	}
	@Test
	public void testGetJBossEAPAppServer() throws Exception {
		AppServer JBossEAPAppServer = AppServer.getJBossEAPAppServer();
		Assert.assertEquals(JBossEAPAppServer.getServerDetectorServerId(),"jboss");
	}
	@Test
	public void testGetWebLogicAppServer() throws Exception {
		AppServer webLogicAppServer = AppServer.getWebLogicAppServer();
		Assert.assertEquals(webLogicAppServer.getServerDetectorServerId(),"weblogic");

	}
	@Test
	public void testGetWildFlyAppServer() throws Exception {
		AppServer wildFlyAppServer = AppServer.getWildFlyAppServer();
		Assert.assertEquals(wildFlyAppServer.getServerDetectorServerId(),"wildfly");
	}

	@Test
	public void testGetWebSphereAppServer() throws Exception {
		AppServer wildFlyAppServer = AppServer.getWebSphereAppServer();
		Assert.assertEquals(wildFlyAppServer.getServerDetectorServerId(),"websphere");
	}

	@Test
	public void testSetDir() throws Exception {
		AppServer tomcatAppServer = AppServer.getTomcatAppServer();
		String dir ="C:\\liferay\\";
		tomcatAppServer.setDirName(dir);
		dir = "C:\\liferay";
		Assert.assertEquals(tomcatAppServer.getDir().getCanonicalPath(),dir);
	}

	@Test
	public void testAutoDir() throws Exception {
		AppServer tomcatAppServer = AppServer.getTomcatAppServer();
		Assert.assertEquals(tomcatAppServer.getDir().getCanonicalPath(),dir);
	}

	@Test
	public void testAutoScanDir() throws Exception {

	}


}
