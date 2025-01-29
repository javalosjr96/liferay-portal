/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.company.service.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.asset.link.model.adapter.StagedAssetLink;
import com.liferay.counter.kernel.service.CounterLocalService;
import com.liferay.counter.kernel.service.persistence.CounterFinder;
import com.liferay.counter.model.CounterRegister;
import com.liferay.document.library.kernel.model.DLFileEntryMetadata;
import com.liferay.document.library.kernel.model.DLFileEntryType;
import com.liferay.document.library.kernel.model.DLFileEntryTypeConstants;
import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.document.library.kernel.service.DLFileEntryTypeLocalService;
import com.liferay.dynamic.data.mapping.constants.DDMStructureConstants;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalService;
import com.liferay.dynamic.data.mapping.storage.StorageType;
import com.liferay.expando.kernel.model.adapter.StagedExpandoColumn;
import com.liferay.expando.model.adapter.StagedExpandoTable;
import com.liferay.exportimport.kernel.service.StagingLocalService;
import com.liferay.layout.friendly.url.LayoutFriendlyURLEntryHelper;
import com.liferay.layout.set.model.adapter.StagedLayoutSet;
import com.liferay.petra.lang.SafeCloseable;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.events.StartupHelperUtil;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskThreadLocal;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.db.partition.DBPartition;
import com.liferay.portal.kernel.exception.CompanyMxException;
import com.liferay.portal.kernel.exception.CompanyNameException;
import com.liferay.portal.kernel.exception.CompanyVirtualHostException;
import com.liferay.portal.kernel.exception.ModelListenerException;
import com.liferay.portal.kernel.exception.NoSuchPasswordPolicyException;
import com.liferay.portal.kernel.exception.NoSuchVirtualHostException;
import com.liferay.portal.kernel.exception.RequiredCompanyException;
import com.liferay.portal.kernel.instance.PortalInstancePool;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.BaseModelListener;
import com.liferay.portal.kernel.model.ClassName;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.model.LayoutSetPrototype;
import com.liferay.portal.kernel.model.ModelListener;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.model.OrganizationConstants;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.UserGroup;
import com.liferay.portal.kernel.model.UserGroupRole;
import com.liferay.portal.kernel.model.adapter.StagedTheme;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.ClassNameLocalService;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.LayoutPrototypeLocalService;
import com.liferay.portal.kernel.service.LayoutSetPrototypeLocalService;
import com.liferay.portal.kernel.service.OrganizationLocalService;
import com.liferay.portal.kernel.service.PasswordPolicyLocalService;
import com.liferay.portal.kernel.service.PortalPreferencesLocalService;
import com.liferay.portal.kernel.service.PortletLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserGroupLocalService;
import com.liferay.portal.kernel.service.UserGroupRoleLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.service.VirtualHostLocalService;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.randomizerbumpers.NumericStringRandomizerBumper;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DataGuard;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.PropsValuesTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.UserGroupTestUtil;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.transaction.Propagation;
import com.liferay.portal.kernel.transaction.TransactionConfig;
import com.liferay.portal.kernel.transaction.TransactionInvokerUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.HashMapDictionary;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.PortletKeys;
import com.liferay.portal.kernel.util.PrefsProps;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.UnicodePropertiesBuilder;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerMethodTestRule;
import com.liferay.portal.util.PortalInstances;
import com.liferay.portal.util.PropsValues;
import com.liferay.site.model.adapter.StagedGroup;
import com.liferay.sites.kernel.util.Sites;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Mika Koivisto
 * @author Dale Shan
 */
@DataGuard(autoDelete = true, scope = DataGuard.Scope.CLASS)
@RunWith(Arquillian.class)
public class CompanyLocalServiceTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			PermissionCheckerMethodTestRule.INSTANCE);

	public void resetBackgroundTaskThreadLocal() throws Exception {
		Class<?> backgroundTaskThreadLocalClass =
			BackgroundTaskThreadLocal.class;

		Field backgroundTaskIdField =
			backgroundTaskThreadLocalClass.getDeclaredField(
				"_backgroundTaskId");

		backgroundTaskIdField.setAccessible(true);

		Method setMethod = ThreadLocal.class.getDeclaredMethod(
			"set", Object.class);

		setMethod.invoke(backgroundTaskIdField.get(null), 0L);
	}

	@After
	public void tearDown() throws Exception {
		resetBackgroundTaskThreadLocal();

		for (ServiceRegistration<?> serviceRegistration :
				_serviceRegistrations) {

			serviceRegistration.unregister();
		}

		_serviceRegistrations.clear();

		deleteClassName(_layoutFriendlyURLEntryHelper.getClassName(true));
		deleteStagingClassNameEntries();
	}

	@Test
	public void testAddAndDeleteCompany() throws Exception {
		Company company = addCompany();

		for (String webId : PortalInstancePool.getWebIds()) {
			Assert.assertEquals(company.getWebId(), webId);
		}
	}


	protected Company addCompany() throws Exception {
		long counterCompanyId =
			_counterLocalService.increment(Company.class.getName()) + 1;

		Company company = addCompany(
			RandomTestUtil.randomString() + "test.com");

		if (PropsValues.COMPANY_PREDICTABLE_COMPANY_IDS_ENABLED) {
			Assert.assertEquals(counterCompanyId, company.getCompanyId());
		}
		else {
			Assert.assertTrue(
				(company.getCompanyId() >= (long)Math.pow(10, 13)) &&
				(company.getCompanyId() < (long)Math.pow(10, 14)));
			Assert.assertNotEquals(counterCompanyId, company.getCompanyId());
		}

		return company;
	}

	protected Company addCompany(String webId) throws Exception {
		Company company = _companyLocalService.addCompany(
			null, webId, webId, "test.com", 0, true, true, null, null, null,
			null, null, null);

		PortalInstances.initCompany(company);

		return company;
	}

	protected LayoutSetPrototype addLayoutSetPrototype(
			long companyId, long userId, String name)
		throws Exception {

		return _layoutSetPrototypeLocalService.addLayoutSetPrototype(
			userId, companyId,
			HashMapBuilder.put(
				LocaleUtil.getDefault(), name
			).build(),
			new HashMap<Locale, String>(), true, true,
			getServiceContext(companyId));
	}

	protected User addUser(
			long companyId, long userId, long groupId,
			ServiceContext serviceContext)
		throws Exception {

		return UserTestUtil.addUser(
			companyId, userId,
			RandomTestUtil.randomString(NumericStringRandomizerBumper.INSTANCE),
			LocaleUtil.getDefault(), RandomTestUtil.randomString(),
			RandomTestUtil.randomString(), new long[] {groupId},
			serviceContext);
	}

	protected void deleteClassName(String value) {
		ClassName className = _classNameLocalService.fetchClassName(value);

		if (className == null) {
			return;
		}

		_classNameLocalService.deleteClassName(className);
	}

	protected void deleteStagingClassNameEntries() {
		deleteClassName(Folder.class.getName());
		deleteClassName(StagedAssetLink.class.getName());
		deleteClassName(StagedExpandoColumn.class.getName());
		deleteClassName(StagedExpandoTable.class.getName());
		deleteClassName(StagedGroup.class.getName());
		deleteClassName(StagedLayoutSet.class.getName());
		deleteClassName(StagedTheme.class.getName());
	}

	protected ServiceContext getServiceContext(long companyId) {
		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setAddGroupPermissions(true);
		serviceContext.setAddGuestPermissions(true);
		serviceContext.setCompanyId(companyId);

		return serviceContext;
	}

	protected void testUpdateCompanyNames(
			Company company, String[] companyNames, boolean expectFailure)
		throws Exception {

		for (String companyName : companyNames) {
			try (SafeCloseable safeCloseable =
					CompanyThreadLocal.setCompanyIdWithSafeCloseable(
						company.getCompanyId())) {

				company = _companyLocalService.updateCompany(
					company.getCompanyId(), company.getVirtualHostname(),
					company.getMx(), company.getHomeURL(), true, null,
					companyName, company.getLegalName(), company.getLegalId(),
					company.getLegalType(), company.getSicCode(),
					company.getTickerSymbol(), company.getIndustry(),
					company.getType(), company.getSize());

				Assert.assertFalse(expectFailure);
			}
			catch (CompanyNameException companyNameException) {
				if (_log.isDebugEnabled()) {
					_log.debug(companyNameException);
				}

				Assert.assertTrue(expectFailure);
			}
		}
	}

	protected void testUpdateMx(String mx, boolean valid, boolean mailMxUpdate)
		throws Exception {

		Company company = addCompany();

		String originalMx = company.getMx();

		try (SafeCloseable safeCloseable1 =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"MAIL_MX_UPDATE", mailMxUpdate);
			SafeCloseable safeCloseable2 =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(
					company.getCompanyId())) {

			_companyLocalService.updateCompany(
				company.getCompanyId(), company.getVirtualHostname(), mx,
				company.getMaxUsers(), company.isActive());

			company = _companyLocalService.getCompany(company.getCompanyId());

			String updatedMx = company.getMx();

			if (valid && mailMxUpdate) {
				Assert.assertNotEquals(originalMx, updatedMx);
			}
			else {
				Assert.assertEquals(originalMx, updatedMx);
			}
		}
		catch (CompanyMxException companyMxException) {
			if (_log.isDebugEnabled()) {
				_log.debug(companyMxException);
			}

			Assert.assertFalse(valid);
			Assert.assertTrue(mailMxUpdate);
		}
		finally {
			_companyLocalService.deleteCompany(company.getCompanyId());
		}
	}

	protected void testUpdateVirtualHostnames(
			String[] virtualHostnames, boolean expectFailure)
		throws Exception {

		Company company = addCompany();

		try {
			for (String virtualHostname : virtualHostnames) {
				try (SafeCloseable safeCloseable =
						CompanyThreadLocal.setCompanyIdWithSafeCloseable(
							company.getCompanyId())) {

					_companyLocalService.updateCompany(
						company.getCompanyId(), virtualHostname,
						company.getMx(), company.getMaxUsers(),
						company.isActive());

					Assert.assertFalse(expectFailure);
				}
				catch (CompanyVirtualHostException
							companyVirtualHostException) {

					if (_log.isDebugEnabled()) {
						_log.debug(companyVirtualHostException);
					}

					Assert.assertTrue(expectFailure);
				}
			}
		}
		finally {
			_companyLocalService.deleteCompany(company.getCompanyId());
		}
	}

	private List<String> _registerModelListeners() {
		List<String> list = new CopyOnWriteArrayList<>();

		Bundle bundle = FrameworkUtil.getBundle(getClass());

		BundleContext bundleContext = bundle.getBundleContext();

		_serviceRegistrations.add(
			bundleContext.registerService(
				ModelListener.class,
				new BaseModelListener<Role>() {

					@Override
					public void onBeforeRemove(Role role)
						throws ModelListenerException {

						list.add(Role.class.getName());
					}

				},
				new HashMapDictionary<>()));
		_serviceRegistrations.add(
			bundleContext.registerService(
				ModelListener.class,
				new BaseModelListener<UserGroupRole>() {

					@Override
					public void onBeforeRemove(UserGroupRole userGroupRole)
						throws ModelListenerException {

						list.add(UserGroupRole.class.getName());
					}

				},
				new HashMapDictionary<>()));

		return list;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		CompanyLocalServiceTest.class);

	private static final TransactionConfig _transactionConfig;

	static {
		TransactionConfig.Builder builder = new TransactionConfig.Builder();

		builder.setPropagation(Propagation.SUPPORTS);
		builder.setReadOnly(true);
		builder.setRollbackForClasses(Exception.class);

		_transactionConfig = builder.build();
	}

	@Inject
	private ClassNameLocalService _classNameLocalService;

	@Inject
	private CompanyLocalService _companyLocalService;

	@Inject
	private CounterLocalService _counterLocalService;

	@Inject
	private DDMStructureLocalService _ddmStructureLocalService;

	@Inject
	private DLAppLocalService _dlAppLocalService;

	@Inject
	private DLFileEntryTypeLocalService _dlFileEntryTypeLocalService;

	@Inject
	private GroupLocalService _groupLocalService;

	@Inject
	private Language _language;

	@Inject
	private LayoutFriendlyURLEntryHelper _layoutFriendlyURLEntryHelper;

	@Inject
	private LayoutPrototypeLocalService _layoutPrototypeLocalService;

	@Inject
	private LayoutSetPrototypeLocalService _layoutSetPrototypeLocalService;

	@Inject
	private OrganizationLocalService _organizationLocalService;

	@Inject
	private PasswordPolicyLocalService _passwordPolicyLocalService;

	@Inject
	private Portal _portal;

	@Inject
	private PortalPreferencesLocalService _portalPreferencesLocalService;

	@Inject
	private PortletLocalService _portletLocalService;

	@Inject
	private PrefsProps _prefsProps;

	@Inject
	private RoleLocalService _roleLocalService;

	private final List<ServiceRegistration<?>> _serviceRegistrations =
		new CopyOnWriteArrayList<>();

	@Inject
	private Sites _sites;

	@Inject
	private StagingLocalService _stagingLocalService;

	@Inject
	private UserGroupLocalService _userGroupLocalService;

	@Inject
	private UserGroupRoleLocalService _userGroupRoleLocalService;

	@Inject
	private UserLocalService _userLocalService;

	@Inject
	private VirtualHostLocalService _virtualHostLocalService;

}