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
		_locked = CompanyThreadLocal.isLocked();
		_partitionCompanyId = CompanyThreadLocal.getPartitionCompanyId();
	}

	@Override
	public T call() throws Exception {
		if ((_partitionCompanyId == null) && _locked) {
			try (SafeCloseable safeCloseable = CompanyThreadLocal.lock(
					_companyId)) {

				return _callable.call();
			}
		}

		if (_partitionCompanyId == null) {
			try (SafeCloseable safeCloseable =
					CompanyThreadLocal.setCompanyIdWithSafeCloseable(
						_companyId)) {

				return _callable.call();
			}
		}

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.carryPartitionScope(
					_companyId, _partitionCompanyId)) {

			return _callable.call();
		}
	}

	private final Callable<T> _callable;
	private final Long _companyId;
	private final boolean _locked;
	private final Long _partitionCompanyId;

}