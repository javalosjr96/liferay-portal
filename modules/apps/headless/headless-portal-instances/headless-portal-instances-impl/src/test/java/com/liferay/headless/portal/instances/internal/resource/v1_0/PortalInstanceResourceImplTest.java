/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.headless.portal.instances.internal.resource.v1_0;

import com.liferay.petra.io.unsync.UnsyncByteArrayOutputStream;
import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.Hashtable;

import org.apache.felix.cm.file.ConfigurationHandler;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mockito;

/**
 * @author Jorge Avalos
 */
public class PortalInstanceResourceImplTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@BeforeClass
	public static void setUpClass() {
		_portalInstanceResourceImpl = new PortalInstanceResourceImpl();
	}

	@Test
	public void testGetScopedConfiguration() throws Exception {
		_testGetScopedConfigurationCompanyScope();
		_testGetScopedConfigurationGroupScope();
		_testGetScopedConfigurationNoScope();
		_testGetScopedConfigurationPortletInstanceScope();
	}

	@Test
	public void testIsApplicable() throws Exception {
		_testIsApplicableCompanyScopeDifferentCompany();
		_testIsApplicableCompanyScopeMatchingCompany();
		_testIsApplicableGroupScopeDifferentCompany();
		_testIsApplicableGroupScopeGroupNotFound();
		_testIsApplicableGroupScopeMatchingCompany();
		_testIsApplicablePortletInstanceScope();
	}

	private String _encodedDictionary(String key, Object value)
		throws Exception {

		Hashtable<String, Object> properties = new Hashtable<>();

		properties.put(key, value);

		UnsyncByteArrayOutputStream unsyncByteArrayOutputStream =
			new UnsyncByteArrayOutputStream();

		ConfigurationHandler.write(unsyncByteArrayOutputStream, properties);

		return unsyncByteArrayOutputStream.toString();
	}

	private Object _getScopedConfiguration(
			String configurationId, String encodedDictionary)
		throws Exception {

		return ReflectionTestUtil.invoke(
			_portalInstanceResourceImpl, "_getScopedConfiguration",
			new Class<?>[] {String.class, String.class}, configurationId,
			encodedDictionary);
	}

	private boolean _isApplicable(long companyId, Object scopedConfiguration)
		throws Exception {

		return (boolean)ReflectionTestUtil.invoke(
			_portalInstanceResourceImpl, "_isApplicable",
			new Class<?>[] {long.class, scopedConfiguration.getClass()},
			companyId, scopedConfiguration);
	}

	private void _testGetScopedConfigurationCompanyScope() throws Exception {
		Object scopedConfiguration = _getScopedConfiguration(
			"test.configuration",
			_encodedDictionary(
				ExtendedObjectClassDefinition.Scope.COMPANY.getPropertyKey(),
				_COMPANY_ID));

		Assert.assertEquals(
			ExtendedObjectClassDefinition.Scope.COMPANY,
			ReflectionTestUtil.invoke(
				scopedConfiguration, "getScope", new Class<?>[0]));
		Assert.assertEquals(
			Long.valueOf(_COMPANY_ID),
			ReflectionTestUtil.invoke(
				scopedConfiguration, "getScopePK", new Class<?>[0]));
	}

	private void _testGetScopedConfigurationGroupScope() throws Exception {
		Object scopedConfiguration = _getScopedConfiguration(
			"test.configuration",
			_encodedDictionary(
				ExtendedObjectClassDefinition.Scope.GROUP.getPropertyKey(),
				_GROUP_ID));

		Assert.assertEquals(
			ExtendedObjectClassDefinition.Scope.GROUP,
			ReflectionTestUtil.invoke(
				scopedConfiguration, "getScope", new Class<?>[0]));
		Assert.assertEquals(
			Long.valueOf(_GROUP_ID),
			ReflectionTestUtil.invoke(
				scopedConfiguration, "getScopePK", new Class<?>[0]));
	}

	private void _testGetScopedConfigurationNoScope() throws Exception {
		Assert.assertNull(
			_getScopedConfiguration(
				"test.configuration", _encodedDictionary("otherKey", "value")));
	}

	private void _testGetScopedConfigurationPortletInstanceScope()
		throws Exception {

		Object scopedConfiguration = _getScopedConfiguration(
			"test.configuration",
			_encodedDictionary(
				ExtendedObjectClassDefinition.Scope.PORTLET_INSTANCE.
					getPropertyKey(),
				_PORTLET_INSTANCE_ID));

		Assert.assertEquals(
			ExtendedObjectClassDefinition.Scope.PORTLET_INSTANCE,
			ReflectionTestUtil.invoke(
				scopedConfiguration, "getScope", new Class<?>[0]));
		Assert.assertEquals(
			_PORTLET_INSTANCE_ID,
			ReflectionTestUtil.invoke(
				scopedConfiguration, "getScopePK", new Class<?>[0]));
	}

	private void _testIsApplicableCompanyScopeDifferentCompany()
		throws Exception {

		Assert.assertFalse(
			_isApplicable(
				_COMPANY_ID + 1,
				_getScopedConfiguration(
					"test.configuration",
					_encodedDictionary(
						ExtendedObjectClassDefinition.Scope.COMPANY.
							getPropertyKey(),
						_COMPANY_ID))));
	}

	private void _testIsApplicableCompanyScopeMatchingCompany()
		throws Exception {

		Assert.assertTrue(
			_isApplicable(
				_COMPANY_ID,
				_getScopedConfiguration(
					"test.configuration",
					_encodedDictionary(
						ExtendedObjectClassDefinition.Scope.COMPANY.
							getPropertyKey(),
						_COMPANY_ID))));
	}

	private void _testIsApplicableGroupScopeDifferentCompany()
		throws Exception {

		Group group = Mockito.mock(Group.class);

		Mockito.when(
			group.getCompanyId()
		).thenReturn(
			_COMPANY_ID + 1
		);

		GroupLocalService groupLocalService = Mockito.mock(
			GroupLocalService.class);

		Mockito.when(
			groupLocalService.fetchGroup(_GROUP_ID)
		).thenReturn(
			group
		);

		ReflectionTestUtil.setFieldValue(
			_portalInstanceResourceImpl, "_groupLocalService",
			groupLocalService);

		Assert.assertFalse(
			_isApplicable(
				_COMPANY_ID,
				_getScopedConfiguration(
					"test.configuration",
					_encodedDictionary(
						ExtendedObjectClassDefinition.Scope.GROUP.
							getPropertyKey(),
						_GROUP_ID))));
	}

	private void _testIsApplicableGroupScopeGroupNotFound() throws Exception {
		GroupLocalService groupLocalService = Mockito.mock(
			GroupLocalService.class);

		Mockito.when(
			groupLocalService.fetchGroup(_GROUP_ID)
		).thenReturn(
			null
		);

		ReflectionTestUtil.setFieldValue(
			_portalInstanceResourceImpl, "_groupLocalService",
			groupLocalService);

		Assert.assertFalse(
			_isApplicable(
				_COMPANY_ID,
				_getScopedConfiguration(
					"test.configuration",
					_encodedDictionary(
						ExtendedObjectClassDefinition.Scope.GROUP.
							getPropertyKey(),
						_GROUP_ID))));
	}

	private void _testIsApplicableGroupScopeMatchingCompany() throws Exception {
		Group group = Mockito.mock(Group.class);

		Mockito.when(
			group.getCompanyId()
		).thenReturn(
			_COMPANY_ID
		);

		GroupLocalService groupLocalService = Mockito.mock(
			GroupLocalService.class);

		Mockito.when(
			groupLocalService.fetchGroup(_GROUP_ID)
		).thenReturn(
			group
		);

		ReflectionTestUtil.setFieldValue(
			_portalInstanceResourceImpl, "_groupLocalService",
			groupLocalService);

		Assert.assertTrue(
			_isApplicable(
				_COMPANY_ID,
				_getScopedConfiguration(
					"test.configuration",
					_encodedDictionary(
						ExtendedObjectClassDefinition.Scope.GROUP.
							getPropertyKey(),
						_GROUP_ID))));
	}

	private void _testIsApplicablePortletInstanceScope() throws Exception {
		Assert.assertFalse(
			_isApplicable(
				_COMPANY_ID,
				_getScopedConfiguration(
					"test.configuration",
					_encodedDictionary(
						ExtendedObjectClassDefinition.Scope.PORTLET_INSTANCE.
							getPropertyKey(),
						_PORTLET_INSTANCE_ID))));
	}

	private static final long _COMPANY_ID = 12345L;

	private static final long _GROUP_ID = 20116L;

	private static final String _PORTLET_INSTANCE_ID = "test_portlet";

	private static PortalInstanceResourceImpl _portalInstanceResourceImpl;

}