/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.upgrade.data.cleanup;

import com.liferay.portal.kernel.util.LinkedHashMapBuilder;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Jorge Avalos
 */
public class DataCleanupPreupgradeProcessTest {

	@Test
	public void testGetWavedDataCleanupPreupgradeProcessesWithCircularDependency() {
		DataCleanupPreupgradeProcess process1 = _newProcess();
		DataCleanupPreupgradeProcess process2 = _newProcess();

		Map<DataCleanupPreupgradeProcess, List<DataCleanupPreupgradeProcess>>
			map =
				LinkedHashMapBuilder.
					<DataCleanupPreupgradeProcess,
					 List<DataCleanupPreupgradeProcess>>put(
						process1,
						DataCleanupPreupgradeProcess.dependsOn(process2)
					).put(
						process2,
						DataCleanupPreupgradeProcess.dependsOn(process1)
					).build();

		try {
			DataCleanupPreupgradeProcess.getWavedDataCleanupPreupgradeProcesses(
				map);

			Assert.fail();
		}
		catch (IllegalStateException illegalStateException) {
			Assert.assertEquals(
				"Circular dependency", illegalStateException.getMessage());
		}
	}

	@Test
	public void testGetWavedDataCleanupPreupgradeProcessesWithDiamondDependency() {
		DataCleanupPreupgradeProcess process1 = _newProcess();
		DataCleanupPreupgradeProcess process2 = _newProcess();
		DataCleanupPreupgradeProcess process3 = _newProcess();
		DataCleanupPreupgradeProcess process4 = _newProcess();

		Map<DataCleanupPreupgradeProcess, List<DataCleanupPreupgradeProcess>>
			map =
				LinkedHashMapBuilder.
					<DataCleanupPreupgradeProcess,
					 List<DataCleanupPreupgradeProcess>>put(
						process1, DataCleanupPreupgradeProcess.dependsOn()
					).put(
						process2,
						DataCleanupPreupgradeProcess.dependsOn(process1)
					).put(
						process3,
						DataCleanupPreupgradeProcess.dependsOn(process1)
					).put(
						process4,
						DataCleanupPreupgradeProcess.dependsOn(
							process2, process3)
					).build();

		List<List<DataCleanupPreupgradeProcess>> waves =
			DataCleanupPreupgradeProcess.getWavedDataCleanupPreupgradeProcesses(
				map);

		Assert.assertEquals(waves.toString(), 3, waves.size());

		List<DataCleanupPreupgradeProcess> wave1 = waves.get(0);

		Assert.assertEquals(wave1.toString(), 1, wave1.size());
		Assert.assertSame(process1, wave1.get(0));

		List<DataCleanupPreupgradeProcess> wave2 = waves.get(1);

		Assert.assertEquals(wave2.toString(), 2, wave2.size());
		Assert.assertTrue(wave2.contains(process2));
		Assert.assertTrue(wave2.contains(process3));

		List<DataCleanupPreupgradeProcess> wave3 = waves.get(2);

		Assert.assertEquals(wave3.toString(), 1, wave3.size());
		Assert.assertSame(process4, wave3.get(0));
	}

	@Test
	public void testGetWavedDataCleanupPreupgradeProcessesWithNoDependencies() {
		Map<DataCleanupPreupgradeProcess, List<DataCleanupPreupgradeProcess>>
			map =
				LinkedHashMapBuilder.
					<DataCleanupPreupgradeProcess,
					 List<DataCleanupPreupgradeProcess>>put(
						_newProcess(), DataCleanupPreupgradeProcess.dependsOn()
					).put(
						_newProcess(), DataCleanupPreupgradeProcess.dependsOn()
					).put(
						_newProcess(), DataCleanupPreupgradeProcess.dependsOn()
					).build();

		List<List<DataCleanupPreupgradeProcess>> waves =
			DataCleanupPreupgradeProcess.getWavedDataCleanupPreupgradeProcesses(
				map);

		Assert.assertEquals(waves.toString(), 1, waves.size());

		List<DataCleanupPreupgradeProcess> wave1 = waves.get(0);

		Assert.assertEquals(wave1.toString(), 3, wave1.size());
	}

	@Test
	public void testGetWavedDataCleanupPreupgradeProcessesWithLinearChain() {
		DataCleanupPreupgradeProcess process1 = _newProcess();
		DataCleanupPreupgradeProcess process2 = _newProcess();
		DataCleanupPreupgradeProcess process3 = _newProcess();

		Map<DataCleanupPreupgradeProcess, List<DataCleanupPreupgradeProcess>>
			map =
				LinkedHashMapBuilder.
					<DataCleanupPreupgradeProcess,
					 List<DataCleanupPreupgradeProcess>>put(
						process1, DataCleanupPreupgradeProcess.dependsOn()
					).put(
						process2,
						DataCleanupPreupgradeProcess.dependsOn(process1)
					).put(
						process3,
						DataCleanupPreupgradeProcess.dependsOn(process2)
					).build();

		List<List<DataCleanupPreupgradeProcess>> waves =
			DataCleanupPreupgradeProcess.getWavedDataCleanupPreupgradeProcesses(
				map);

		Assert.assertEquals(waves.toString(), 3, waves.size());

		for (List<DataCleanupPreupgradeProcess> wave : waves) {
			Assert.assertEquals(wave.toString(), 1, wave.size());
		}

		List<DataCleanupPreupgradeProcess> wave1 = waves.get(0);

		Assert.assertSame(process1, wave1.get(0));

		List<DataCleanupPreupgradeProcess> wave2 = waves.get(1);

		Assert.assertSame(process2, wave2.get(0));

		List<DataCleanupPreupgradeProcess> wave3 = waves.get(2);

		Assert.assertSame(process3, wave3.get(0));
	}

	@Test
	public void testGetWavedDataCleanupPreupgradeProcessesWithMissingDependency() {
		Map<DataCleanupPreupgradeProcess, List<DataCleanupPreupgradeProcess>>
			map =
				LinkedHashMapBuilder.
					<DataCleanupPreupgradeProcess,
					 List<DataCleanupPreupgradeProcess>>put(
						_newProcess(),
						DataCleanupPreupgradeProcess.dependsOn(_newProcess())
					).build();

		try {
			DataCleanupPreupgradeProcess.getWavedDataCleanupPreupgradeProcesses(
				map);

			Assert.fail();
		}
		catch (IllegalStateException illegalStateException) {
			String message = illegalStateException.getMessage();

			Assert.assertTrue(message.startsWith("Missing dependency "));
		}
	}

	private DataCleanupPreupgradeProcess _newProcess() {
		return new DataCleanupPreupgradeProcess() {

			@Override
			protected void doUpgrade() {
			}

		};
	}

}