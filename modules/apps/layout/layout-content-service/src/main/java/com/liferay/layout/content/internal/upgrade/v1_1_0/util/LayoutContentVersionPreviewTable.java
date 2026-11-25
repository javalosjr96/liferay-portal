/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.layout.content.internal.upgrade.v1_1_0.util;

import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.upgrade.UpgradeProcessFactory;

/**
 * @author Lourdes Fernández Besada
 * @generated
 * @see com.liferay.portal.tools.upgrade.table.builder.UpgradeTableBuilder
 */
public class LayoutContentVersionPreviewTable {

	public static UpgradeProcess create() {
		return UpgradeProcessFactory.createTableIfMissing(
			_TABLE_NAME, _TABLE_SQL_CREATE);
	}

	private static final String _TABLE_NAME = "LayoutContentVersionPreview";

	private static final String _TABLE_SQL_CREATE =
		"create table LayoutContentVersionPreview (mvccVersion LONG default 0 not null,layoutContentVersionPreviewId LONG not null primary key,groupId LONG,companyId LONG,userId LONG,userName VARCHAR(75) null,createDate DATE null,modifiedDate DATE null,layoutContentVersionId LONG,html TEXT null,languageId VARCHAR(75) null,segmentsExperienceERC VARCHAR(75) null)";

}