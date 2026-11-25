/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.permission.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.lang.SafeCloseable;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.ResourceAction;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.ResourcePermission;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.ResourceActionLocalService;
import com.liferay.portal.kernel.service.ResourcePermissionLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.test.util.CompanyTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.test.log.LogCapture;
import com.liferay.portal.test.log.LogEntry;
import com.liferay.portal.test.log.LoggerTestUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.util.PortalInstances;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Michael Bowerman
 */
@RunWith(Arquillian.class)
public class ResourceActionLocalServiceTest {

	@ClassRule
	@Rule
	public static final LiferayIntegrationTestRule liferayIntegrationTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() {
		List<String> actionIds = new ArrayList<>(3);

		actionIds.add(ActionKeys.VIEW);
		actionIds.add(_ACTION_ID_1);
		actionIds.add(_ACTION_ID_2);

		_resourceActionLocalService.checkResourceActions(_NAME, actionIds);

		actionIds.set(2, _ACTION_ID_3);

		_resourceActionLocalService.deleteResourceAction(
			_resourceActionLocalService.fetchResourceAction(
				_NAME, _ACTION_ID_2));

		_resourceActionLocalService.checkResourceActions(_NAME, actionIds);
	}

	@After
	public void tearDown() {
		List<ResourceAction> resourceActions =
			_resourceActionLocalService.getResourceActions(_NAME);

		for (ResourceAction resourceAction : resourceActions) {
			_resourceActionLocalService.deleteResourceAction(resourceAction);
		}
	}

	@Test(expected = SystemException.class)
	public void testAddMoreThan64Actions() {
		List<String> actionIds = new ArrayList<>(65);

		actionIds.add(ActionKeys.VIEW);

		for (int i = 0; i < 64; i++) {
			actionIds.add("actionId" + i);
		}

		_resourceActionLocalService.checkResourceActions(_NAME, actionIds);
	}

	@Test
	public void testAddResourceAction() {
		String actionId = RandomTestUtil.randomString();

		Assert.assertNull(
			_resourceActionLocalService.fetchResourceAction(_NAME, actionId));

		_resourceActionLocalService.addResourceAction(_NAME, actionId, 0L);

		Assert.assertNotNull(
			_resourceActionLocalService.fetchResourceAction(_NAME, actionId));
	}

	@Test
	public void testDeleteResourceAction() throws Exception {
		Assume.assumeFalse(PropsValues.DATABASE_PARTITION_ENABLED);

		Company company1 = CompanyTestUtil.addCompany();
		Company company2 = CompanyTestUtil.addCompany();

		try {
			_setResourcePermission(company1.getCompanyId());
			_setResourcePermission(company2.getCompanyId());
			_setResourcePermission(TestPropsValues.getCompanyId());

			ResourceAction resourceAction =
				_resourceActionLocalService.getResourceAction(
					_NAME, _ACTION_ID_1);

			try (LogCapture logCapture = LoggerTestUtil.configureLog4JLogger(
					"com.liferay.portal.service.impl.CompanyLocalServiceImpl",
					LoggerTestUtil.WARN);
				SafeCloseable safeCloseable =
					PortalInstances.setCompanyInDeletionProcessWithSafeCloseable(
						company2.getCompanyId())) {

				_resourceActionLocalService.deleteResourceAction(
					TestPropsValues.getCompanyId(), resourceAction);

				List<LogEntry> logEntries = logCapture.getLogEntries();

				Assert.assertEquals(
					logEntries.toString(), 0, logEntries.size());
			}

			long bitwiseValue = resourceAction.getBitwiseValue();

			Assert.assertFalse(
				_hasActionId(bitwiseValue, company1.getCompanyId()));
			Assert.assertFalse(
				_hasActionId(bitwiseValue, company2.getCompanyId()));
			Assert.assertFalse(
				_hasActionId(bitwiseValue, TestPropsValues.getCompanyId()));
		}
		finally {
			_companyLocalService.deleteCompany(company2);

			_companyLocalService.deleteCompany(company1);
		}
	}

	@Test
	public void testFirstAvailableBitwiseValueGetsGenerated()
		throws PortalException {

		ResourceAction resourceAction1 =
			_resourceActionLocalService.getResourceAction(
				_NAME, ActionKeys.VIEW);

		Assert.assertEquals(1L, resourceAction1.getBitwiseValue());

		ResourceAction resourceAction2 =
			_resourceActionLocalService.getResourceAction(_NAME, _ACTION_ID_1);

		Assert.assertEquals(2L, resourceAction2.getBitwiseValue());

		ResourceAction resourceAction3 =
			_resourceActionLocalService.getResourceAction(_NAME, _ACTION_ID_3);

		Assert.assertEquals(4L, resourceAction3.getBitwiseValue());
	}

	@Test
	public void testViewActionBitwiseValue() throws PortalException {
		ResourceAction viewResourceAction =
			_resourceActionLocalService.getResourceAction(
				_NAME, ActionKeys.VIEW);

		Assert.assertEquals(1L, viewResourceAction.getBitwiseValue());
	}

	private boolean _hasActionId(long bitwiseValue, long companyId)
		throws Exception {

		Role role = _roleLocalService.getRole(companyId, RoleConstants.USER);

		ResourcePermission resourcePermission =
			_resourcePermissionLocalService.fetchResourcePermission(
				companyId, _NAME, ResourceConstants.SCOPE_COMPANY,
				String.valueOf(companyId), role.getRoleId());

		if ((resourcePermission.getActionIds() & bitwiseValue) != 0) {
			return true;
		}

		return false;
	}

	private void _setResourcePermission(long companyId) throws Exception {
		Role role = _roleLocalService.getRole(companyId, RoleConstants.USER);

		_resourcePermissionLocalService.setResourcePermissions(
			companyId, _NAME, ResourceConstants.SCOPE_COMPANY,
			String.valueOf(companyId), role.getRoleId(),
			new String[] {_ACTION_ID_1});

		ResourceAction resourceAction =
			_resourceActionLocalService.getResourceAction(_NAME, _ACTION_ID_1);

		Assert.assertTrue(
			_hasActionId(resourceAction.getBitwiseValue(), companyId));
	}

	private static final String _ACTION_ID_1 = "actionId1";

	private static final String _ACTION_ID_2 = "actionId2";

	private static final String _ACTION_ID_3 = "actionId3";

	private static final String _NAME =
		ResourceActionLocalServiceTest.class.getName();

	@Inject
	private CompanyLocalService _companyLocalService;

	@Inject
	private ResourceActionLocalService _resourceActionLocalService;

	@Inject
	private ResourcePermissionLocalService _resourcePermissionLocalService;

	@Inject
	private RoleLocalService _roleLocalService;

}