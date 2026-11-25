/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.db.partition.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.lang.SafeCloseable;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.ResourceAction;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.ResourcePermission;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.ResourceActionLocalService;
import com.liferay.portal.kernel.service.ResourcePermissionLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.AssumeTestRule;
import com.liferay.portal.kernel.test.util.CompanyTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jorge Avalos
 */
@RunWith(Arquillian.class)
public class ResourceActionLocalServiceDBPartitionTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new AssumeTestRule("assume"), new LiferayIntegrationTestRule());

	public static void assume() {
		Assume.assumeTrue(PropsValues.DATABASE_PARTITION_ENABLED);

		DB db = DBManagerUtil.getDB();

		Assume.assumeTrue(db.isSupportsDBPartition());
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
		_company1 = CompanyTestUtil.addCompany();
		_company2 = CompanyTestUtil.addCompany();
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		_companyLocalService.deleteCompany(_company2);

		_companyLocalService.deleteCompany(_company1);
	}

	@Test
	public void testDeleteResourceAction() throws Exception {
		String actionId = RandomTestUtil.randomString();
		String name = RandomTestUtil.randomString();

		ResourceAction resourceAction1 = _addResourceAction(
			actionId, _company1.getCompanyId(), name);
		ResourceAction resourceAction2 = _addResourceAction(
			actionId, _company2.getCompanyId(), name);

		Assert.assertEquals(
			resourceAction1.getBitwiseValue(),
			resourceAction2.getBitwiseValue());

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(
					_company1.getCompanyId())) {

			_resourceActionLocalService.deleteResourceAction(
				_company1.getCompanyId(), resourceAction1);
		}

		Assert.assertFalse(
			_hasActionId(
				resourceAction1.getBitwiseValue(), _company1.getCompanyId(),
				name));
		Assert.assertTrue(
			_hasActionId(
				resourceAction2.getBitwiseValue(), _company2.getCompanyId(),
				name));
	}

	@Test
	public void testDeleteResourceActionFromAnotherCompany() throws Exception {
		String actionId = RandomTestUtil.randomString();
		String name = RandomTestUtil.randomString();

		ResourceAction resourceAction = _addResourceAction(
			actionId, _company1.getCompanyId(), name);

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(
					_company2.getCompanyId())) {

			Assert.assertThrows(
				UnsupportedOperationException.class,
				() -> _resourceActionLocalService.deleteResourceAction(
					_company1.getCompanyId(), resourceAction));
		}
	}

	private ResourceAction _addResourceAction(
			String actionId, long companyId, String name)
		throws Exception {

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(companyId)) {

			_resourceActionLocalService.checkResourceActions(
				name, Arrays.asList(ActionKeys.VIEW, actionId));

			Role role = _roleLocalService.getRole(
				companyId, RoleConstants.USER);

			_resourcePermissionLocalService.setResourcePermissions(
				companyId, name, ResourceConstants.SCOPE_COMPANY,
				String.valueOf(companyId), role.getRoleId(),
				new String[] {actionId});

			ResourceAction resourceAction =
				_resourceActionLocalService.getResourceAction(name, actionId);

			Assert.assertTrue(
				_hasActionId(
					resourceAction.getBitwiseValue(), companyId, name));

			return resourceAction;
		}
	}

	private boolean _hasActionId(long bitwiseValue, long companyId, String name)
		throws Exception {

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(companyId)) {

			Role role = _roleLocalService.getRole(
				companyId, RoleConstants.USER);

			ResourcePermission resourcePermission =
				_resourcePermissionLocalService.getResourcePermission(
					companyId, name, ResourceConstants.SCOPE_COMPANY,
					String.valueOf(companyId), role.getRoleId());

			if ((resourcePermission.getActionIds() & bitwiseValue) != 0) {
				return true;
			}

			return false;
		}
	}

	private static Company _company1;
	private static Company _company2;

	@Inject
	private static CompanyLocalService _companyLocalService;

	@Inject
	private ResourceActionLocalService _resourceActionLocalService;

	@Inject
	private ResourcePermissionLocalService _resourcePermissionLocalService;

	@Inject
	private RoleLocalService _roleLocalService;

}