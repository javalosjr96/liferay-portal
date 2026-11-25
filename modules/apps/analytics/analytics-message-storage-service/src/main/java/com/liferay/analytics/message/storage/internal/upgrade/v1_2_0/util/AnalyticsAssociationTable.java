/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.analytics.message.storage.internal.upgrade.v1_2_0.util;

import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.upgrade.UpgradeProcessFactory;

/**
 * @author Brian Wing Shun Chan
 * @generated
 * @see com.liferay.portal.tools.upgrade.table.builder.UpgradeTableBuilder
 */
public class AnalyticsAssociationTable {

	public static UpgradeProcess create() {
		return UpgradeProcessFactory.createTableIfMissing(
			_TABLE_NAME, _TABLE_SQL_CREATE);
	}

	private static final String _TABLE_NAME = "AnalyticsAssociation";

	private static final String _TABLE_SQL_CREATE =
		"create table AnalyticsAssociation (mvccVersion LONG default 0 not null,analyticsAssociationId LONG not null primary key,companyId LONG,createDate DATE null,modifiedDate DATE null,userId LONG,associationClassName VARCHAR(75) null,associationClassPK LONG,className VARCHAR(75) null,classPK LONG)";

}