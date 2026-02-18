/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.data.cleanup.internal.verify;

import com.liferay.document.library.kernel.store.Store;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.instance.PortalInstancePool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.util.Arrays;

/**
 * @author Jorge Avalos
 */
public class StorePostUpgradeDataCleanupProcess
	implements PostUpgradeDataCleanupProcess {

	public StorePostUpgradeDataCleanupProcess(Store store) {
		_store = store;
	}

	public void cleanUp() throws Exception {
		long[] companyIds = PortalInstancePool.getCompanyIds();

		Arrays.sort(companyIds);

		for (long storeCompanyId : _store.getCompanyIds()) {
			if (Arrays.binarySearch(companyIds, storeCompanyId) < 0) {
				if (_log.isInfoEnabled()) {
					_log.info(
						StringBundler.concat(
							"Found orphan store from deleted company ",
							storeCompanyId,
							". Manual analysis and deletion of this store is ",
							"recommended."));
				}
			}
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		StorePostUpgradeDataCleanupProcess.class);

	private final Store _store;

}