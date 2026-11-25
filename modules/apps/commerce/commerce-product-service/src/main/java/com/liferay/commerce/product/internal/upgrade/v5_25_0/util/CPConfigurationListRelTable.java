/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.commerce.product.internal.upgrade.v5_25_0.util;

import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.upgrade.UpgradeProcessFactory;

/**
 * @author Brian Wing Shun Chan
 * @generated
 * @see com.liferay.portal.tools.upgrade.table.builder.UpgradeTableBuilder
 */
public class CPConfigurationListRelTable {

	public static UpgradeProcess create() {
		return UpgradeProcessFactory.createTableIfMissing(
			_TABLE_NAME, _TABLE_SQL_CREATE);
	}

	private static final String _TABLE_NAME = "CPConfigurationListRel";

	private static final String _TABLE_SQL_CREATE =
		"create table CPConfigurationListRel (mvccVersion LONG default 0 not null,ctCollectionId LONG default 0 not null,CPConfigurationListRelId LONG not null,companyId LONG,userId LONG,userName VARCHAR(75) null,createDate DATE null,modifiedDate DATE null,classNameId LONG,classPK LONG,CPConfigurationListId LONG,primary key (CPConfigurationListRelId, ctCollectionId))";

}