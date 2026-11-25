/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.dynamic.data.mapping.internal.upgrade.v4_1_0.util;

import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.upgrade.UpgradeProcessFactory;

/**
 * @author Brian Wing Shun Chan
 * @generated
 * @see com.liferay.portal.tools.upgrade.table.builder.UpgradeTableBuilder
 */
public class DDMFieldTable {

	public static UpgradeProcess create() {
		return UpgradeProcessFactory.createTableIfMissing(
			_TABLE_NAME, _TABLE_SQL_CREATE);
	}

	private static final String _TABLE_NAME = "DDMField";

	private static final String _TABLE_SQL_CREATE =
		"create table DDMField (mvccVersion LONG default 0 not null,ctCollectionId LONG default 0 not null,fieldId LONG not null,companyId LONG,parentFieldId LONG,storageId LONG,structureVersionId LONG,fieldName TEXT null,fieldType VARCHAR(255) null,instanceId VARCHAR(75) null,localizable BOOLEAN,priority INTEGER,primary key (fieldId, ctCollectionId))";

}