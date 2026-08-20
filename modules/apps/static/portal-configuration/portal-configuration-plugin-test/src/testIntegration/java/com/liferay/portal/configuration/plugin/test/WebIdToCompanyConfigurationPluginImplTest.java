/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.configuration.plugin.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.configuration.test.util.ConfigurationTestUtil;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.HashMapDictionaryBuilder;
import com.liferay.portal.kernel.util.MapUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.util.Dictionary;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * @author Raymond Augé
 */
@RunWith(Arquillian.class)
public class WebIdToCompanyConfigurationPluginImplTest {

	@ClassRule
	@Rule
	public static AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Test
	public void testModifyConfiguration() throws Exception {
		_testModifyConfiguration(
			"dxp.lxc.liferay.com.virtualInstanceId",
			TestPropsValues.COMPANY_WEB_ID);
		_testModifyConfiguration(
			"dxp.lxc.liferay.com.virtualInstanceId", "default");
	}

	@Test
	public void testModifyConfigurationWithCompanyWebId() throws Exception {
		_testModifyConfiguration(
			"companyWebId", TestPropsValues.COMPANY_WEB_ID);
	}

	@Test
	public void testModifyConfigurationWithExistingCompanyId()
		throws Exception {

		long companyId = RandomTestUtil.randomLong();

		Dictionary<String, Object> processedProperties =
			_getProcessedProperties(
				HashMapDictionaryBuilder.<String, Object>put(
					"companyId", companyId
				).put(
					"dxp.lxc.liferay.com.virtualInstanceId",
					TestPropsValues.COMPANY_WEB_ID
				).build());

		Assert.assertEquals(companyId, processedProperties.get("companyId"));
	}

	private Dictionary<String, Object> _getProcessedProperties(
			Dictionary<String, Object> properties)
		throws Exception {

		Configuration configuration = _configurationAdmin.getConfiguration(
			"test.pid");

		ConfigurationTestUtil.saveConfiguration(configuration, properties);

		configuration = _configurationAdmin.getConfiguration("test.pid");

		Dictionary<String, Object> processedProperties =
			configuration.getProcessedProperties(null);

		ConfigurationTestUtil.deleteConfiguration(configuration);

		return processedProperties;
	}

	private void _testModifyConfiguration(String propertyKey, String webId)
		throws Exception {

		Dictionary<String, Object> processedProperties =
			_getProcessedProperties(
				MapUtil.singletonDictionary(propertyKey, webId));

		Assert.assertEquals(
			TestPropsValues.getCompanyId(),
			processedProperties.get("companyId"));
	}

	@Inject
	private ConfigurationAdmin _configurationAdmin;

}