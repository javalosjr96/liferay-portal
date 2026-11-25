/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.security.auth;

import com.liferay.petra.lang.SafeCloseable;
import com.liferay.portal.kernel.model.CompanyConstants;
import com.liferay.portal.kernel.test.util.RandomTestUtil;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author István András Dézsi
 * @author Alberto Chaparro
 */
public class CompanyInheritableThreadLocalCallableTest {

	@Test
	public void test() throws Exception {
		_test(1L, false);
		_test(2L, false);
		_test(CompanyConstants.SYSTEM, false);
	}

	@Test
	public void testCallInPartitionScope() throws Exception {
		long companyId1 = RandomTestUtil.randomLong();
		long companyId2 = RandomTestUtil.randomLong();

		try (SafeCloseable safeCloseable1 =
				CompanyThreadLocal.setPartitionCompanyIdWithSafeCloseable(
					companyId1);
			SafeCloseable safeCloseable2 =
				CompanyThreadLocal.setRawCompanyIdWithSafeCloseable(
					companyId2)) {

			Assert.assertEquals(
				Long.valueOf(companyId2),
				_call(CompanyThreadLocal::getCompanyId));
			Assert.assertEquals(
				Long.valueOf(companyId1),
				_call(CompanyThreadLocal::getPartitionCompanyId));
			Assert.assertTrue(_call(CompanyThreadLocal::isInPartitionScope));
			Assert.assertFalse(_call(CompanyThreadLocal::isLocked));
		}
	}

	@Test
	public void testCallWithLock() throws Exception {
		long companyId = RandomTestUtil.randomLong();

		try (SafeCloseable safeCloseable = CompanyThreadLocal.lock(
				companyId)) {

			Assert.assertEquals(
				Long.valueOf(companyId),
				_call(CompanyThreadLocal::getCompanyId));
			Assert.assertFalse(_call(CompanyThreadLocal::isInPartitionScope));
			Assert.assertTrue(_call(CompanyThreadLocal::isLocked));
		}
	}

	private <T> T _call(Callable<T> callable) throws Exception {
		ExecutorService executorService = Executors.newSingleThreadExecutor();

		try {
			Future<T> future = executorService.submit(
				new CompanyInheritableThreadLocalCallable<>(callable));

			return future.get();
		}
		finally {
			executorService.shutdownNow();
		}
	}

	private void _test(Long companyId, boolean locked) throws Exception {
		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(companyId)) {

			Assert.assertEquals(
				companyId,
				new CompanyInheritableThreadLocalCallable<Long>(
					CompanyThreadLocal::getCompanyId
				).call());

			Assert.assertEquals(
				locked,
				new CompanyInheritableThreadLocalCallable<>(
					CompanyThreadLocal::isLocked
				).call());
		}
	}

}