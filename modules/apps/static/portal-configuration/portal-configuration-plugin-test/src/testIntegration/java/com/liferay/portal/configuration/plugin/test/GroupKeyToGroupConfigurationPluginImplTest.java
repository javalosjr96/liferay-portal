/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.configuration.plugin.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.configuration.test.util.ConfigurationTestUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.util.TestPropsValues;
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
 * @author Mario Leandro
 */
@RunWith(Arquillian.class)
public class GroupKeyToGroupConfigurationPluginImplTest {

	@ClassRule
	@Rule
	public static AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Test
	public void testModifyConfiguration() throws Exception {
		Dictionary<String, Object> processedProperties =
			_getProcessedProperties(
				TestPropsValues.COMPANY_WEB_ID + "--" + GroupConstants.GUEST);

		Assert.assertEquals(
			TestPropsValues.getCompanyId(),
			processedProperties.get("companyId"));

		Group group = _groupLocalService.getGroup(
			TestPropsValues.getCompanyId(), GroupConstants.GUEST);

		Assert.assertEquals(
			group.getGroupId(), processedProperties.get("groupId"));
	}

	private Dictionary<String, Object> _getProcessedProperties(
			String portableIdentifier)
		throws Exception {

		Configuration configuration = _configurationAdmin.getConfiguration(
			"test.pid");

		ConfigurationTestUtil.saveConfiguration(
			configuration,
			MapUtil.singletonDictionary("groupKey", portableIdentifier));

		configuration = _configurationAdmin.getConfiguration("test.pid");

		Dictionary<String, Object> processedProperties =
			configuration.getProcessedProperties(null);

		ConfigurationTestUtil.deleteConfiguration(configuration);

		return processedProperties;
	}

	@Inject
	private ConfigurationAdmin _configurationAdmin;

	@Inject
	private GroupLocalService _groupLocalService;

}