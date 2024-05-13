/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package  com.liferay.portal.background.task.internal.upgrade.v2_0_1;

import com.liferay.portal.background.task.model.BackgroundTask;
import com.liferay.portal.background.task.service.BackgroundTaskLocalService;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;


public class BackgroundTaskCompanyIdUpgradeProcess extends UpgradeProcess {
	public BackgroundTaskCompanyIdUpgradeProcess(
		BackgroundTaskLocalService backgroundTaskLocalService) {

		_backgroundTaskLocalService = backgroundTaskLocalService;
	}
	@Override
	protected void doUpgrade() throws Exception {
		try (PreparedStatement preparedStatement1 = connection.prepareStatement(
			"SELECT backgroundTaskId FROM BackgroundTask WHERE taskContextMap LIKE '%companyId%'")) {

			try (ResultSet resultSet = preparedStatement1.executeQuery()) {
				while (resultSet.next()) {
					try {
						BackgroundTask backgroundTask = _backgroundTaskLocalService.getBackgroundTask(resultSet.getLong("backgroundTaskId"));

						Map<String, Serializable> taskContextMap =
							backgroundTask.getTaskContextMap();

						taskContextMap.remove("companyId");

						backgroundTask.setTaskContextMap(taskContextMap);

						_backgroundTaskLocalService.updateBackgroundTask(backgroundTask);

					}
					catch (RuntimeException runtimeException) {
						throw new RuntimeException(runtimeException);
					}
					}
				}
			}
		}

	private final BackgroundTaskLocalService _backgroundTaskLocalService;

}