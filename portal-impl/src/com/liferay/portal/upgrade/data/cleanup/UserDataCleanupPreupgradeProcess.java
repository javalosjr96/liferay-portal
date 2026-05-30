/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.upgrade.data.cleanup;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.upgrade.data.cleanup.DataCleanupPreupgradeProcess;
import com.liferay.portal.kernel.upgrade.data.cleanup.FilterableAllTablesOrphanReferencesDataCleanupPreupgradeProcess;
import com.liferay.portal.kernel.upgrade.data.cleanup.TableOrphanReferencesDataCleanupPreupgradeProcess;
import com.liferay.portal.kernel.upgrade.data.cleanup.UserAllTablesOrphanReferencesDataCleanupPreupgradeProcess;
import com.liferay.portal.kernel.util.PortletKeys;

/**
 * @author Luis Ortiz
 */
public class UserDataCleanupPreupgradeProcess
	extends DataCleanupPreupgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		_upgradeWithTiming(
			"UserAllTables",
			new UserAllTablesOrphanReferencesDataCleanupPreupgradeProcess());
		_upgradeWithTiming(
			"FilterableAllTables",
			new FilterableAllTablesOrphanReferencesDataCleanupPreupgradeProcess(
				StringBundler.concat(
					"[$SOURCE_TABLE_ALIAS$].classNameId = (select classNameId ",
					"from ClassName_ where value = '", User.class.getName(),
					"')"),
				new String[] {"classNameId"}, "classPK",
				new String[] {"userId"}, "User_"));
		_upgradeWithTiming(
			"PortalPreferences",
			new TableOrphanReferencesDataCleanupPreupgradeProcess(
				null,
				"[$SOURCE_TABLE_ALIAS$].ownerType = " +
					PortletKeys.PREFS_OWNER_TYPE_USER,
				"ownerId", "PortalPreferences", "userId", "User_"));
		_upgradeWithTiming(
			"PortletPreferences",
			new TableOrphanReferencesDataCleanupPreupgradeProcess(
				null,
				"[$SOURCE_TABLE_ALIAS$].ownerType = " +
					PortletKeys.PREFS_OWNER_TYPE_USER,
				"ownerId", "PortletPreferences", "userId", "User_"));
		_upgradeWithTiming(
			"ResourcePermission",
			new TableOrphanReferencesDataCleanupPreupgradeProcess(
				null,
				StringBundler.concat(
					"[$SOURCE_TABLE_ALIAS$].scope = ",
					ResourceConstants.SCOPE_INDIVIDUAL, " and ",
					"[$SOURCE_TABLE_ALIAS$].name = '", User.class.getName(),
					"'"),
				"primKeyId", "ResourcePermission", "userId", "User_"));
	}

	private void _upgradeWithTiming(
			String label, DataCleanupPreupgradeProcess process)
		throws Exception {

		long start = System.currentTimeMillis();

		upgrade(process);

		if (_log.isInfoEnabled()) {
			_log.info(
				StringBundler.concat(
					"UserDataCleanupPreupgradeProcess/", label,
					" completed in ", System.currentTimeMillis() - start,
					" ms"));
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		UserDataCleanupPreupgradeProcess.class);

}