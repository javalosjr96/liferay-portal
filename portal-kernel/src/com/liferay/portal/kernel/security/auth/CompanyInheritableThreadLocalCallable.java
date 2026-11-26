/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.security.auth;

import com.liferay.petra.lang.SafeCloseable;

import java.util.concurrent.Callable;

/**
 * @author István András Dézsi
 */
public class CompanyInheritableThreadLocalCallable<T> implements Callable<T> {

	public CompanyInheritableThreadLocalCallable(Callable<T> callable) {
		_callable = callable;

		_companyId = CompanyThreadLocal.getCompanyId();
		_partitionCompanyId = CompanyThreadLocal.getPartitionCompanyId();
	}

	@Override
	public T call() throws Exception {
		try (SafeCloseable safeCloseable = _open()) {
			return _callable.call();
		}
	}

	private SafeCloseable _open() {
		if (_partitionCompanyId != null) {
			return CompanyThreadLocal.carryPartitionScope(
				_companyId, _partitionCompanyId);
		}

		return CompanyThreadLocal.setCompanyIdWithSafeCloseable(_companyId);
	}

	private final Callable<T> _callable;
	private final Long _companyId;
	private final Long _partitionCompanyId;

}