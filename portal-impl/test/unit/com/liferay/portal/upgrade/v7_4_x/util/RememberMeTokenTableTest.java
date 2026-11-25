/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.upgrade.v7_4_x.util;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.upgrade.DescribableUpgradeStep;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Jorge Avalos
 */
public class RememberMeTokenTableTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testCreate() {
		UpgradeProcess upgradeProcess = RememberMeTokenTable.create();

		DescribableUpgradeStep describableUpgradeStep =
			(DescribableUpgradeStep)upgradeProcess;

		Assert.assertEquals(
			"createTableIfMissing", describableUpgradeStep.getOperation());

		Map<String, Object> arguments = describableUpgradeStep.getArguments();

		Assert.assertEquals("RememberMeToken", arguments.get("tableName"));

		String sql = (String)arguments.get("sql");

		Assert.assertTrue(sql.startsWith("create table RememberMeToken ("));

		String upgradeInfo = ReflectionTestUtil.getFieldValue(
			upgradeProcess, "_upgradeInfo");

		Assert.assertTrue(
			upgradeInfo,
			upgradeInfo.matches(
				StringBundler.concat(
					Pattern.quote(RememberMeTokenTable.class.getName()),
					":\\d+ - ",
					Pattern.quote(
						"Modifying schema to create table RememberMeToken if " +
							"it does not exist"))));
	}

}