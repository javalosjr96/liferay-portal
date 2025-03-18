/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.upgrade;

/**
 * @author Jorge Avalos
 */
public class DefaultDuplicateRemovalProcess
	extends BaseDuplicateRemovalProcess {

	public DefaultDuplicateRemovalProcess(String tableName, String... columns) {
		super(tableName, null, columns);
	}

	@Override
	protected void doUpgrade() {
		super.doUpgrade();
	}

}