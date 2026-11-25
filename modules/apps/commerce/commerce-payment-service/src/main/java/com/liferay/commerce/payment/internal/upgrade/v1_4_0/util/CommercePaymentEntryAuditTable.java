/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.commerce.payment.internal.upgrade.v1_4_0.util;

import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.upgrade.UpgradeProcessFactory;

/**
 * @author Brian Wing Shun Chan
 * @generated
 * @see com.liferay.portal.tools.upgrade.table.builder.UpgradeTableBuilder
 */
public class CommercePaymentEntryAuditTable {

	public static UpgradeProcess create() {
		return UpgradeProcessFactory.createTableIfMissing(
			_TABLE_NAME, _TABLE_SQL_CREATE);
	}

	private static final String _TABLE_NAME = "CommercePaymentEntryAudit";

	private static final String _TABLE_SQL_CREATE =
		"create table CommercePaymentEntryAudit (mvccVersion LONG default 0 not null,commercePaymentEntryAuditId LONG not null primary key,companyId LONG,userId LONG,userName VARCHAR(75) null,createDate DATE null,modifiedDate DATE null,commercePaymentEntryId LONG,amount BIGDECIMAL null,currencyCode VARCHAR(75) null,logType VARCHAR(75) null,logTypeSettings TEXT null)";

}