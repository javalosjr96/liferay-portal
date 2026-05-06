/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.upgrade.monitor;

import com.liferay.petra.lang.SafeCloseable;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.util.PropsValuesTestUtil;

import java.util.concurrent.ScheduledExecutorService;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Jorge Avalos
 */
public class UpgradeQueryMonitorTest {

	@After
	public void tearDown() {
		UpgradeQueryMonitor.stop();
	}

	@Test
	public void testStartIsNoOpWhenDisabled() throws Exception {
		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"UPGRADE_QUERY_MONITOR_ENABLED", false)) {

			UpgradeQueryMonitor.start();

			Assert.assertNull(
				ReflectionTestUtil.getFieldValue(
					UpgradeQueryMonitor.class, "_scheduledExecutorService"));
		}
	}

	@Test
	public void testStartIsNoOpWhenAlreadyStarted() throws Exception {
		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"UPGRADE_QUERY_MONITOR_ENABLED", true)) {

			UpgradeQueryMonitor.start();

			ScheduledExecutorService scheduledExecutorService =
				ReflectionTestUtil.getFieldValue(
					UpgradeQueryMonitor.class, "_scheduledExecutorService");

			UpgradeQueryMonitor.start();

			Assert.assertSame(
				scheduledExecutorService,
				ReflectionTestUtil.getFieldValue(
					UpgradeQueryMonitor.class, "_scheduledExecutorService"));
		}
	}

	@Test
	public void testStartSchedulesExecutor() throws Exception {
		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"UPGRADE_QUERY_MONITOR_ENABLED", true)) {

			UpgradeQueryMonitor.start();

			Assert.assertNotNull(
				ReflectionTestUtil.getFieldValue(
					UpgradeQueryMonitor.class, "_scheduledExecutorService"));
		}
	}

	@Test
	public void testStopIsIdempotentWhenNotStarted() {
		UpgradeQueryMonitor.stop();

		Assert.assertNull(
			ReflectionTestUtil.getFieldValue(
				UpgradeQueryMonitor.class, "_scheduledExecutorService"));
	}

	@Test
	public void testStopShutsDownExecutor() throws Exception {
		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"UPGRADE_QUERY_MONITOR_ENABLED", true)) {

			UpgradeQueryMonitor.start();

			Assert.assertNotNull(
				ReflectionTestUtil.getFieldValue(
					UpgradeQueryMonitor.class, "_scheduledExecutorService"));

			UpgradeQueryMonitor.stop();

			Assert.assertNull(
				ReflectionTestUtil.getFieldValue(
					UpgradeQueryMonitor.class, "_scheduledExecutorService"));
		}
	}

}