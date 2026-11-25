/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.object.service.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.object.constants.ObjectActionExecutorConstants;
import com.liferay.object.constants.ObjectActionTriggerConstants;
import com.liferay.object.constants.ObjectDefinitionConstants;
import com.liferay.object.constants.ObjectFieldSettingConstants;
import com.liferay.object.field.builder.AttachmentObjectFieldBuilder;
import com.liferay.object.field.builder.TextObjectFieldBuilder;
import com.liferay.object.field.setting.builder.ObjectFieldSettingBuilder;
import com.liferay.object.model.ObjectAction;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectField;
import com.liferay.object.service.ObjectActionLocalService;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectFieldLocalService;
import com.liferay.object.test.util.ObjectDefinitionTestUtil;
import com.liferay.petra.lang.SafeCloseable;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.instance.PortalInstancePool;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.ResourceAction;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.ResourcePermission;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.ResourceActionsUtil;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import com.liferay.portal.kernel.service.ResourceActionLocalService;
import com.liferay.portal.kernel.service.ResourcePermissionLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.AssumeTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.CompanyTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.UnicodePropertiesBuilder;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.vulcan.util.LocalizedMapUtil;

import jakarta.portlet.Portlet;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * @author Carolina Barbosa
 */
@RunWith(Arquillian.class)
public class ObjectDefinitionLocalServiceDBPartitionTest {

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
		CompanyLocalServiceUtil.deleteCompany(_company2);

		CompanyLocalServiceUtil.deleteCompany(_company1);
	}

	@Test
	public void testDeleteCompanyRemovesResourceActions() throws Exception {
		Company company = CompanyTestUtil.addCompany();

		ObjectDefinition objectDefinition = _publishObjectDefinition(company);

		String portletId = objectDefinition.getPortletId();

		boolean deleted = false;

		try {
			Assert.assertEquals(
				objectDefinition.getResourceName(),
				ResourceActionsUtil.getPortletRootModelResource(portletId));

			_companyLocalService.deleteCompany(company);

			deleted = true;

			Assert.assertNull(
				ResourceActionsUtil.getPortletRootModelResource(portletId));
		}
		finally {
			if (!deleted) {
				_companyLocalService.deleteCompany(company);
			}
		}
	}

	@Test
	public void testDeleteObjectAction() throws Exception {
		ObjectDefinition objectDefinition = _publishObjectDefinition(_company1);

		ObjectAction objectAction = null;

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(
					_company1.getCompanyId())) {

			User user = UserTestUtil.getAdminUser(_company1.getCompanyId());

			objectAction = _objectActionLocalService.addObjectAction(
				null, user.getUserId(),
				objectDefinition.getObjectDefinitionId(), true,
				StringPool.BLANK,
				LocalizedMapUtil.getLocalizedMap(RandomTestUtil.randomString()),
				LocalizedMapUtil.getLocalizedMap(RandomTestUtil.randomString()),
				LocalizedMapUtil.getLocalizedMap(RandomTestUtil.randomString()),
				RandomTestUtil.randomString(),
				ObjectActionExecutorConstants.KEY_WEBHOOK,
				ObjectActionTriggerConstants.KEY_STANDALONE,
				UnicodePropertiesBuilder.put(
					"secret", RandomTestUtil.randomString()
				).put(
					"url", "https://" + RandomTestUtil.randomString() + ".com"
				).build(),
				false);
		}

		ResourceAction resourceAction = _fetchResourceAction(
			objectAction.getName(), _company1.getCompanyId(),
			objectDefinition.getClassName());

		_addResourcePermission(_company2.getCompanyId(), resourceAction);

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(
					_company1.getCompanyId())) {

			_objectActionLocalService.deleteObjectAction(
				objectAction.getObjectActionId());
		}

		Assert.assertNull(
			_fetchResourceAction(
				objectAction.getName(), _company1.getCompanyId(),
				objectDefinition.getClassName()));
		Assert.assertTrue(
			_hasActionId(
				resourceAction.getBitwiseValue(), _company2.getCompanyId(),
				objectDefinition.getClassName()));
	}

	@Test
	public void testDeleteObjectDefinition() throws Exception {
		ObjectDefinition objectDefinition = _publishObjectDefinition(_company1);

		_assertResourceActionsCount(
			_company2.getCompanyId(), objectDefinition, 0);
		_assertResourceActionsCount(
			PortalInstancePool.getDefaultCompanyId(), objectDefinition, 0);

		ResourceAction resourceAction = _fetchResourceAction(
			ActionKeys.VIEW, _company1.getCompanyId(),
			objectDefinition.getClassName());

		_addResourcePermission(_company2.getCompanyId(), resourceAction);

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(
					_company1.getCompanyId())) {

			_objectDefinitionLocalService.deleteObjectDefinition(
				objectDefinition.getObjectDefinitionId());
		}

		_assertResourceActionsCount(
			_company1.getCompanyId(), objectDefinition, 0);

		Assert.assertTrue(
			_hasActionId(
				resourceAction.getBitwiseValue(), _company2.getCompanyId(),
				objectDefinition.getClassName()));
	}

	@Test
	public void testDeleteObjectField() throws Exception {
		String objectFieldName = "a" + RandomTestUtil.randomString();

		ObjectDefinition objectDefinition = _publishObjectDefinition(
			_company1,
			Arrays.asList(
				new AttachmentObjectFieldBuilder(
				).labelMap(
					RandomTestUtil.randomLocaleStringMap()
				).name(
					objectFieldName
				).objectFieldSettings(
					Arrays.asList(
						new ObjectFieldSettingBuilder(
						).name(
							ObjectFieldSettingConstants.
								NAME_ACCEPTED_FILE_EXTENSIONS
						).value(
							"txt"
						).build(),
						new ObjectFieldSettingBuilder(
						).name(
							ObjectFieldSettingConstants.NAME_FILE_SOURCE
						).value(
							ObjectFieldSettingConstants.
								VALUE_USER_COMPUTER_TO_DOCS_AND_MEDIA
						).build(),
						new ObjectFieldSettingBuilder(
						).name(
							ObjectFieldSettingConstants.NAME_MAX_FILE_SIZE
						).value(
							"100"
						).build())
				).build(),
				new TextObjectFieldBuilder(
				).labelMap(
					RandomTestUtil.randomLocaleStringMap()
				).name(
					"a" + RandomTestUtil.randomString()
				).build()));

		ObjectField objectField = null;

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(
					_company1.getCompanyId())) {

			objectField = _objectFieldLocalService.getObjectField(
				objectDefinition.getObjectDefinitionId(), objectFieldName);
		}

		ResourceAction resourceAction = _fetchResourceAction(
			objectField.getAttachmentDownloadActionKey(),
			_company1.getCompanyId(), objectDefinition.getClassName());

		_addResourcePermission(_company2.getCompanyId(), resourceAction);

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(
					_company1.getCompanyId())) {

			_objectFieldLocalService.deleteObjectField(
				objectField.getObjectFieldId());
		}

		Assert.assertNull(
			_fetchResourceAction(
				objectField.getAttachmentDownloadActionKey(),
				_company1.getCompanyId(), objectDefinition.getClassName()));
		Assert.assertTrue(
			_hasActionId(
				resourceAction.getBitwiseValue(), _company2.getCompanyId(),
				objectDefinition.getClassName()));
	}

	@Test
	public void testGetClassName() throws Exception {
		ObjectDefinition objectDefinition1 = null;
		ObjectDefinition objectDefinition2 = null;

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(
					TestPropsValues.getCompanyId())) {

			objectDefinition1 =
				ObjectDefinitionTestUtil.publishObjectDefinition();
		}

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(
					PortalInstancePool.getDefaultCompanyId())) {

			User user = UserTestUtil.getAdminUser(
				PortalInstancePool.getDefaultCompanyId());

			objectDefinition2 =
				_objectDefinitionLocalService.addCustomObjectDefinition(
					null, user.getUserId(), 0, objectDefinition1.getClassName(),
					null, true, false, true, false, true, false, true, true,
					true, null, RandomTestUtil.randomLocaleStringMap(),
					objectDefinition1.getShortName(), null, null,
					RandomTestUtil.randomLocaleStringMap(), true,
					ObjectDefinitionConstants.SCOPE_COMPANY,
					ObjectDefinitionConstants.STORAGE_TYPE_DEFAULT,
					Collections.emptyList(),
					Collections.singletonList(
						new TextObjectFieldBuilder(
						).labelMap(
							RandomTestUtil.randomLocaleStringMap()
						).name(
							StringUtil.randomId()
						).build()),
					Collections.emptyList(), new ServiceContext());
		}

		Assert.assertNotEquals(
			objectDefinition1.getClassName(), objectDefinition2.getClassName());
	}

	@Test
	public void testPublishObjectDefinition() throws Exception {
		_objectDefinition = null;

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(
					TestPropsValues.getCompanyId())) {

			_objectDefinition =
				ObjectDefinitionTestUtil.publishObjectDefinition(
					Collections.singletonList(
						new TextObjectFieldBuilder(
						).labelMap(
							RandomTestUtil.randomLocaleStringMap()
						).name(
							"a" + RandomTestUtil.randomString()
						).build()),
					ObjectDefinitionConstants.SCOPE_COMPANY,
					TestPropsValues.getUserId());
		}

		_assertResourceActionsCount(
			TestPropsValues.getCompanyId(), _objectDefinition, 4);
		_assertResourceActionsCount(
			PortalInstancePool.getDefaultCompanyId(), _objectDefinition, 0);
	}

	@Test
	public void testUndeployObjectDefinition() throws Exception {
		Company company = CompanyTestUtil.addCompany();

		ObjectDefinition objectDefinition = _publishObjectDefinition(company);

		_assertServiceReferencesCount(1, objectDefinition);

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(
					PortalInstancePool.getDefaultCompanyId())) {

			_objectDefinitionLocalService.undeployObjectDefinition(
				objectDefinition);
		}

		_assertServiceReferencesCount(0, objectDefinition);

		_companyLocalService.deleteCompany(company);
	}

	@Test
	public void testUpdateClassNameWhenCompanyIsNotInPortalInstancePool()
		throws Exception {

		Company company = CompanyTestUtil.addCompany();

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(
					company.getCompanyId())) {

			User user = UserTestUtil.getAdminUser(company.getCompanyId());

			ObjectDefinition objectDefinition =
				_objectDefinitionLocalService.addCustomObjectDefinition(
					null, user.getUserId(), 0, null, null, true, false, true,
					false, true, false, true, true, true, null,
					RandomTestUtil.randomLocaleStringMap(),
					"A" + RandomTestUtil.randomString(), null, null,
					RandomTestUtil.randomLocaleStringMap(), true,
					ObjectDefinitionConstants.SCOPE_COMPANY,
					ObjectDefinitionConstants.STORAGE_TYPE_DEFAULT,
					Collections.emptyList(),
					Collections.singletonList(
						new TextObjectFieldBuilder(
						).labelMap(
							RandomTestUtil.randomLocaleStringMap()
						).name(
							StringUtil.randomId()
						).build()),
					Collections.emptyList(), new ServiceContext());

			PortalInstancePool.remove(company.getCompanyId());

			ObjectDefinition updatedObjectDefinition =
				_objectDefinitionLocalService.updateClassName(
					objectDefinition.getObjectDefinitionId());

			Assert.assertNotEquals(
				objectDefinition.getClassName(),
				updatedObjectDefinition.getClassName());
		}
		finally {
			PortalInstancePool.add(company);

			_companyLocalService.deleteCompany(company);
		}
	}

	private void _addResourcePermission(
			long companyId, ResourceAction resourceAction)
		throws Exception {

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(companyId)) {

			_resourceActionLocalService.addResourceAction(
				resourceAction.getName(), resourceAction.getActionId(),
				resourceAction.getBitwiseValue());

			Role role = _roleLocalService.getRole(
				companyId, RoleConstants.USER);

			_resourcePermissionLocalService.setResourcePermissions(
				companyId, resourceAction.getName(),
				ResourceConstants.SCOPE_COMPANY, String.valueOf(companyId),
				role.getRoleId(), new String[] {resourceAction.getActionId()});
		}

		Assert.assertTrue(
			_hasActionId(
				resourceAction.getBitwiseValue(), companyId,
				resourceAction.getName()));
	}

	private void _assertResourceActionsCount(
		long companyId, ObjectDefinition objectDefinition,
		int resourceActionsCount) {

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(companyId)) {

			Assert.assertEquals(
				resourceActionsCount,
				_resourceActionLocalService.getResourceActionsCount(
					objectDefinition.getClassName()));
		}
	}

	private void _assertServiceReferencesCount(
			int expectedCount, ObjectDefinition objectDefinition)
		throws Exception {

		Bundle bundle = FrameworkUtil.getBundle(
			ObjectDefinitionLocalServiceDBPartitionTest.class);

		BundleContext bundleContext = bundle.getBundleContext();

		Collection<ServiceReference<Portlet>> serviceReferences =
			bundleContext.getServiceReferences(
				Portlet.class,
				"(jakarta.portlet.name=" + objectDefinition.getPortletId() +
					")");

		Assert.assertEquals(
			serviceReferences.toString(), expectedCount,
			serviceReferences.size());
	}

	private ResourceAction _fetchResourceAction(
		String actionId, long companyId, String name) {

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(companyId)) {

			return _resourceActionLocalService.fetchResourceAction(
				name, actionId);
		}
	}

	private boolean _hasActionId(long bitwiseValue, long companyId, String name)
		throws Exception {

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(companyId)) {

			Role role = _roleLocalService.getRole(
				companyId, RoleConstants.USER);

			ResourcePermission resourcePermission =
				_resourcePermissionLocalService.fetchResourcePermission(
					companyId, name, ResourceConstants.SCOPE_COMPANY,
					String.valueOf(companyId), role.getRoleId());

			if ((resourcePermission.getActionIds() & bitwiseValue) != 0) {
				return true;
			}

			return false;
		}
	}

	private ObjectDefinition _publishObjectDefinition(Company company)
		throws Exception {

		return _publishObjectDefinition(
			company,
			Collections.singletonList(
				new TextObjectFieldBuilder(
				).labelMap(
					RandomTestUtil.randomLocaleStringMap()
				).name(
					"a" + RandomTestUtil.randomString()
				).build()));
	}

	private ObjectDefinition _publishObjectDefinition(
			Company company, List<ObjectField> objectFields)
		throws Exception {

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(
					company.getCompanyId())) {

			User user = UserTestUtil.getAdminUser(company.getCompanyId());

			return ObjectDefinitionTestUtil.publishObjectDefinition(
				objectFields, ObjectDefinitionConstants.SCOPE_COMPANY,
				user.getUserId());
		}
	}

	private static Company _company1;
	private static Company _company2;

	@Inject
	private CompanyLocalService _companyLocalService;

	@Inject
	private ObjectActionLocalService _objectActionLocalService;

	@DeleteAfterTestRun
	private ObjectDefinition _objectDefinition;

	@Inject
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

	@Inject
	private ObjectFieldLocalService _objectFieldLocalService;

	@Inject
	private ResourceActionLocalService _resourceActionLocalService;

	@Inject
	private ResourcePermissionLocalService _resourcePermissionLocalService;

	@Inject
	private RoleLocalService _roleLocalService;

}