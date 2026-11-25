/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.site.internal.upgrade.v2_2_0.util;

import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.upgrade.UpgradeProcessFactory;

/**
 * @author Shuyang Zhou
 * @generated
 * @see com.liferay.portal.tools.upgrade.table.builder.UpgradeTableBuilder
 */
public class SiteSitemapRegenerationEntryTable {

	public static UpgradeProcess create() {
		return UpgradeProcessFactory.createTableIfMissing(
			_TABLE_NAME, _TABLE_SQL_CREATE);
	}

	private static final String _TABLE_NAME = "SiteSitemapRegenerationEntry";

	private static final String _TABLE_SQL_CREATE =
		"create table SiteSitemapRegenerationEntry (mvccVersion LONG default 0 not null,siteSitemapRegenerationEntryId LONG not null primary key,groupId LONG,companyId LONG,assetTypeKey VARCHAR(75) null)";

}