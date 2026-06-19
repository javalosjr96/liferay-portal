/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.data.cleanup.internal.verify;

import com.liferay.portal.search.index.IndexInformation;
import com.liferay.portal.search.index.IndexNameBuilder;
import com.liferay.portal.test.log.LogCapture;
import com.liferay.portal.test.log.LogEntry;
import com.liferay.portal.test.log.LoggerTestUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mockito;

/**
 * @author Jorge Avalos
 */
public class PostUpgradeDataCleanupVerifyProcessTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testGetPostUpgradeDataCleanupProcessesWhenSearchIsAvailable() {
		PostUpgradeDataCleanupVerifyProcess
			postUpgradeDataCleanupVerifyProcess =
				new PostUpgradeDataCleanupVerifyProcess();

		IndexInformation indexInformation = Mockito.mock(
			IndexInformation.class);

		IndexNameBuilder indexNameBuilder = Mockito.mock(
			IndexNameBuilder.class);

		Mockito.when(
			indexNameBuilder.getIndexNamePrefix()
		).thenReturn(
			""
		);

		List<PostUpgradeDataCleanupProcess> baseProcesses = new ArrayList<>();

		baseProcesses.add(Mockito.mock(PostUpgradeDataCleanupProcess.class));
		baseProcesses.add(Mockito.mock(PostUpgradeDataCleanupProcess.class));
		baseProcesses.add(Mockito.mock(PostUpgradeDataCleanupProcess.class));
		baseProcesses.add(Mockito.mock(PostUpgradeDataCleanupProcess.class));

		try (LogCapture logCapture = LoggerTestUtil.configureLog4JLogger(
				PostUpgradeDataCleanupVerifyProcess.class.getName(),
				LoggerTestUtil.WARN)) {

			List<PostUpgradeDataCleanupProcess>
				postUpgradeDataCleanupProcesses =
					postUpgradeDataCleanupVerifyProcess.
						getPostUpgradeDataCleanupProcesses(
							indexInformation, indexNameBuilder, baseProcesses);

			Assert.assertEquals(
				postUpgradeDataCleanupProcesses.toString(), 5,
				postUpgradeDataCleanupProcesses.size());

			List<LogEntry> logEntries = logCapture.getLogEntries();

			Assert.assertTrue(logEntries.toString(), logEntries.isEmpty());
		}
	}

	@Test
	public void testGetPostUpgradeDataCleanupProcessesWhenSearchIsUnavailable() {
		PostUpgradeDataCleanupVerifyProcess
			postUpgradeDataCleanupVerifyProcess =
				new PostUpgradeDataCleanupVerifyProcess();

		List<PostUpgradeDataCleanupProcess> baseProcesses = new ArrayList<>();

		baseProcesses.add(Mockito.mock(PostUpgradeDataCleanupProcess.class));
		baseProcesses.add(Mockito.mock(PostUpgradeDataCleanupProcess.class));
		baseProcesses.add(Mockito.mock(PostUpgradeDataCleanupProcess.class));
		baseProcesses.add(Mockito.mock(PostUpgradeDataCleanupProcess.class));

		try (LogCapture logCapture = LoggerTestUtil.configureLog4JLogger(
				PostUpgradeDataCleanupVerifyProcess.class.getName(),
				LoggerTestUtil.WARN)) {

			List<PostUpgradeDataCleanupProcess>
				postUpgradeDataCleanupProcesses =
					postUpgradeDataCleanupVerifyProcess.
						getPostUpgradeDataCleanupProcesses(
							null, null, baseProcesses);

			Assert.assertEquals(
				postUpgradeDataCleanupProcesses.toString(), 4,
				postUpgradeDataCleanupProcesses.size());

			List<LogEntry> logEntries = logCapture.getLogEntries();

			Assert.assertEquals(logEntries.toString(), 1, logEntries.size());

			LogEntry logEntry = logEntries.get(0);

			Assert.assertEquals(
				"Unable to clean up orphaned search indexes because the " +
					"search engine is unavailable",
				logEntry.getMessage());
		}
	}

}