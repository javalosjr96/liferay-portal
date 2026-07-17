/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.db.partition.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.lang.SafeCloseable;
import com.liferay.portal.db.partition.test.util.BaseDBPartitionTestCase;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.RoleConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.test.util.CompanyTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.transaction.Propagation;
import com.liferay.portal.kernel.transaction.TransactionConfig;
import com.liferay.portal.kernel.transaction.TransactionInvokerUtil;
import com.liferay.portal.test.rule.Inject;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jorge Avalos
 */
@RunWith(Arquillian.class)
public class TransactionFlushDBPartitionTest extends BaseDBPartitionTestCase {

	@BeforeClass
	public static void setUpClass() throws Exception {
		_company = CompanyTestUtil.addCompany();

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(
					_company.getCompanyId())) {

			_adminUser = UserTestUtil.getAdminUser(_company.getCompanyId());
		}
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		companyLocalService.deleteCompany(_company.getCompanyId());
	}

	@Test
	public void testSetCompanyIdWithSafeCloseableFlushesSessionOnClose()
		throws Throwable {

		String name = RandomTestUtil.randomString();

		TransactionInvokerUtil.invoke(
			_transactionConfig,
			() -> {
				try (SafeCloseable safeCloseable =
						CompanyThreadLocal.setCompanyIdWithSafeCloseable(
							_company.getCompanyId())) {

					_roleLocalService.addRole(
						null, _adminUser.getUserId(), null, 0, name, null,
						null, RoleConstants.TYPE_REGULAR, null,
						new ServiceContext());
				}

				return null;
			});

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(
					_company.getCompanyId())) {

			Role role = _roleLocalService.fetchRole(
				_company.getCompanyId(), name);

			Assert.assertNotNull(role);
		}

		Role defaultRole = _roleLocalService.fetchRole(
			TestPropsValues.getCompanyId(), name);

		Assert.assertNull(defaultRole);
	}

	@Test
	public void testSetCompanyIdWithSafeCloseableFlushesSessionOnEntry()
		throws Throwable {

		String name = RandomTestUtil.randomString();

		TransactionInvokerUtil.invoke(
			_transactionConfig,
			() -> {
				_roleLocalService.addRole(
					null, TestPropsValues.getUserId(), null, 0, name, null,
					null, RoleConstants.TYPE_REGULAR, null,
					new ServiceContext());

				try (SafeCloseable safeCloseable =
						CompanyThreadLocal.setCompanyIdWithSafeCloseable(
							_company.getCompanyId())) {

					DynamicQuery dynamicQuery =
						_roleLocalService.dynamicQuery();

					_roleLocalService.dynamicQuery(dynamicQuery);
				}

				return null;
			});

		Role role = null;

		try {
			role = _roleLocalService.fetchRole(
				TestPropsValues.getCompanyId(), name);

			Assert.assertNotNull(role);

			Role otherRole = _roleLocalService.fetchRole(
				_company.getCompanyId(), name);

			Assert.assertNull(otherRole);
		}
		finally {
			if (role != null) {
				_roleLocalService.deleteRole(role.getRoleId());
			}
		}
	}

	private static final TransactionConfig _transactionConfig;

	static {
		TransactionConfig.Builder builder = new TransactionConfig.Builder();

		builder.setPropagation(Propagation.REQUIRED);
		builder.setRollbackForClasses(Exception.class);

		_transactionConfig = builder.build();
	}

	private static User _adminUser;
	private static Company _company;

	@Inject
	private RoleLocalService _roleLocalService;

}
