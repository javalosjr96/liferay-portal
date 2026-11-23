/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.upgrade.internal.registry;

import com.liferay.portal.kernel.upgrade.UpgradeStep;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Manuel de la Peña
 */
public class UpgradeInfoTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testEqualsReturnsFalseComparingDifferentClass() {
		UpgradeStep upgradeStep = new TestUpgradeStep();

		UpgradeInfo upgradeInfo = new UpgradeInfo(
			"1.0.0", "2.0.0", upgradeStep);

		Assert.assertNotEquals(upgradeInfo, "");
	}

	@Test
	public void testEqualsReturnsFalseComparingDifferentSchemaVersion() {
		UpgradeInfo upgradeInfo1 = new UpgradeInfo(
			"1.0.0", "2.0.0", new TestUpgradeStep());
		UpgradeInfo upgradeInfo2 = new UpgradeInfo(
			"2.0.0", "3.0.0", new TestUpgradeStep());

		Assert.assertNotEquals(upgradeInfo1, upgradeInfo2);
	}

	@Test
	public void testEqualsReturnsFalseComparingSameSchemaVersion() {
		UpgradeInfo upgradeInfo1 = new UpgradeInfo(
			"1.0.0", "2.0.0", new TestUpgradeStep());
		UpgradeInfo upgradeInfo2 = new UpgradeInfo(
			"1.0.0", "2.0.0", new TestUpgradeStep());

		Assert.assertNotEquals(upgradeInfo1, upgradeInfo2);
	}

	@Test
	public void testEqualsReturnsTrueComparingSameInstance() {
		UpgradeInfo upgradeInfo = new UpgradeInfo(
			"1.0.0", "2.0.0", new TestUpgradeStep());

		Assert.assertEquals(upgradeInfo, upgradeInfo);
	}

	@Test
	public void testEqualsReturnsTrueComparingSameSchemaVersion() {
		UpgradeStep upgradeStep = new TestUpgradeStep();

		UpgradeInfo upgradeInfo1 = new UpgradeInfo(
			"1.0.0", "2.0.0", upgradeStep);
		UpgradeInfo upgradeInfo2 = new UpgradeInfo(
			"1.0.0", "2.0.0", upgradeStep);

		Assert.assertEquals(upgradeInfo1, upgradeInfo2);
	}

	@Test
	public void testGetUpgradeStep() throws Exception {
		List<UpgradeStep> upgradedUpgradeSteps = new ArrayList<>();

		List<UpgradeStep> upgradeSteps = new ArrayList<>();

		for (int i = 0; i < 3; i++) {
			upgradeSteps.add(
				new UpgradeStep() {

					@Override
					public void upgrade() {
						upgradedUpgradeSteps.add(this);
					}

				});
		}

		UpgradeInfo upgradeInfo = new UpgradeInfo(
			"1.0.0", "2.0.0", upgradeSteps);

		Assert.assertEquals(upgradeSteps, upgradeInfo.getUpgradeSteps());

		UpgradeStep upgradeStep = upgradeInfo.getUpgradeStep();

		upgradeStep.upgrade();

		Assert.assertEquals(upgradeSteps, upgradedUpgradeSteps);

		UpgradeStep testUpgradeStep = new TestUpgradeStep();

		upgradeInfo = new UpgradeInfo("1.0.0", "2.0.0", testUpgradeStep);

		Assert.assertSame(testUpgradeStep, upgradeInfo.getUpgradeStep());
		Assert.assertEquals(
			Arrays.asList(testUpgradeStep), upgradeInfo.getUpgradeSteps());
	}

	private static class TestUpgradeStep implements UpgradeStep {

		@Override
		public void upgrade() {
		}

	}

}