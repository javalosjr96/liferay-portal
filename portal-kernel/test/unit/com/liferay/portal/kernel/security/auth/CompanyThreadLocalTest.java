/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.security.auth;

import com.liferay.petra.lang.SafeCloseable;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.change.tracking.CTCollectionIdSupplier;
import com.liferay.portal.kernel.change.tracking.CTCollectionThreadLocal;
import com.liferay.portal.kernel.model.CompanyConstants;
import com.liferay.portal.kernel.module.util.SystemBundleUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.GroupThreadLocal;
import com.liferay.portal.kernel.util.LocaleThreadLocal;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.ProxyFactory;
import com.liferay.portal.kernel.util.TimeZoneThreadLocal;
import com.liferay.portal.test.log.LogCapture;
import com.liferay.portal.test.log.LogEntry;
import com.liferay.portal.test.log.LoggerTestUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.mockito.Mockito;

import org.osgi.framework.BundleContext;

/**
 * @author Alberto Chaparro
 */
public class CompanyThreadLocalTest {

	@BeforeClass
	public static void setUpClass() {
		BundleContext bundleContext = SystemBundleUtil.getBundleContext();

		bundleContext.registerService(
			CTCollectionIdSupplier.class,
			ProxyFactory.newDummyInstance(CTCollectionIdSupplier.class), null);
	}

	@Before
	public void setUp() {
		Set<String> warnedCallSites = ReflectionTestUtil.getFieldValue(
			CompanyThreadLocal.class, "_warnedCallSites");

		warnedCallSites.clear();
	}

	@Test
	public void testCarryPartitionScope() {
		long companyId1 = RandomTestUtil.randomLong();
		long companyId2 = RandomTestUtil.randomLong();

		Long originalCompanyId = CompanyThreadLocal.getCompanyId();

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.carryPartitionScope(
					companyId1, companyId2)) {

			Assert.assertEquals(
				Long.valueOf(companyId1), CompanyThreadLocal.getCompanyId());
			Assert.assertEquals(
				Long.valueOf(companyId2),
				CompanyThreadLocal.getPartitionCompanyId());
		}

		Assert.assertEquals(
			originalCompanyId, CompanyThreadLocal.getCompanyId());
		Assert.assertNull(CompanyThreadLocal.getPartitionCompanyId());

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.carryPartitionScope(
					companyId2, companyId2)) {

			Assert.assertEquals(
				Long.valueOf(companyId2), CompanyThreadLocal.getCompanyId());
			Assert.assertEquals(
				Long.valueOf(companyId2),
				CompanyThreadLocal.getPartitionCompanyId());
		}

		Assert.assertEquals(
			originalCompanyId, CompanyThreadLocal.getCompanyId());
		Assert.assertNull(CompanyThreadLocal.getPartitionCompanyId());
	}

	@Test
	public void testIsInPartitionScope() {
		Assert.assertFalse(CompanyThreadLocal.isInPartitionScope());

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setPartitionCompanyIdWithSafeCloseable(
					RandomTestUtil.randomLong())) {

			Assert.assertTrue(CompanyThreadLocal.isInPartitionScope());
		}

		Assert.assertFalse(CompanyThreadLocal.isInPartitionScope());
	}

	@Test
	public void testSetCompanyId() {
		GroupThreadLocal.setGroupId(1L);

		LocaleThreadLocal.setDefaultLocale(LocaleUtil.GERMAN);

		PasswordModificationThreadLocal.setPasswordUnencrypted("passwordTest");

		PrincipalThreadLocal.setName("userTest");
		PrincipalThreadLocal.setPassword("passwordTest");

		ServiceContext serviceContext = Mockito.mock(ServiceContext.class);

		ServiceContextThreadLocal.pushServiceContext(serviceContext);

		TimeZone pstTimeZone = TimeZone.getTimeZone("PST");

		TimeZoneThreadLocal.setDefaultTimeZone(pstTimeZone);

		_originalCompanyId = CompanyThreadLocal.getCompanyId();

		try {
			CompanyThreadLocal.setCompanyId(1L);

			for (CompanyCentralizedThreadLocal<?>
					companyCentralizedThreadLocal :
						CompanyCentralizedThreadLocal.
							getCompanyCentralizedThreadLocals()) {

				Object initialValue = ReflectionTestUtil.invoke(
					companyCentralizedThreadLocal, "initialValue",
					new Class<?>[0]);

				Assert.assertEquals(
					initialValue, companyCentralizedThreadLocal.get());
			}
		}
		finally {
			CompanyThreadLocal.setCompanyId(_originalCompanyId);
		}
	}

	@Test
	public void testSetCompanyIdWithSafeCloseable() {
		GroupThreadLocal.setGroupId(1L);

		LocaleThreadLocal.setDefaultLocale(LocaleUtil.GERMAN);

		PasswordModificationThreadLocal.setPasswordUnencrypted("passwordTest");

		PrincipalThreadLocal.setName("userTest");
		PrincipalThreadLocal.setPassword("passwordTest");

		ServiceContext serviceContext = Mockito.mock(ServiceContext.class);

		ServiceContextThreadLocal.pushServiceContext(serviceContext);

		TimeZone pstTimeZone = TimeZone.getTimeZone("PST");

		TimeZoneThreadLocal.setDefaultTimeZone(pstTimeZone);

		Map<CompanyCentralizedThreadLocal<?>, Object>
			companyCentralizedThreadLocalMap = new HashMap<>();

		for (CompanyCentralizedThreadLocal<?> companyCentralizedThreadLocal :
				CompanyCentralizedThreadLocal.
					getCompanyCentralizedThreadLocals()) {

			companyCentralizedThreadLocalMap.put(
				companyCentralizedThreadLocal,
				companyCentralizedThreadLocal.get());
		}

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(1L)) {

			for (CompanyCentralizedThreadLocal<?>
					companyCentralizedThreadLocal :
						CompanyCentralizedThreadLocal.
							getCompanyCentralizedThreadLocals()) {

				Object initialValue = ReflectionTestUtil.invoke(
					companyCentralizedThreadLocal, "initialValue",
					new Class<?>[0]);

				Assert.assertEquals(
					initialValue, companyCentralizedThreadLocal.get());
			}
		}

		for (CompanyCentralizedThreadLocal<?> companyCentralizedThreadLocal :
				CompanyCentralizedThreadLocal.
					getCompanyCentralizedThreadLocals()) {

			Assert.assertEquals(
				companyCentralizedThreadLocalMap.get(
					companyCentralizedThreadLocal),
				companyCentralizedThreadLocal.get());
		}
	}

	@Test
	public void testSetCompanyIdWithSafeCloseableInPartitionScope() {
		long companyId1 = RandomTestUtil.randomLong();
		long companyId2 = RandomTestUtil.randomLong();

		try (LogCapture logCapture = LoggerTestUtil.configureLog4JLogger(
				CompanyThreadLocal.class.getName(), LoggerTestUtil.WARN)) {

			try (SafeCloseable safeCloseable =
					CompanyThreadLocal.setCompanyIdWithSafeCloseable(
						companyId1)) {

				Assert.assertEquals(
					Long.valueOf(companyId1),
					CompanyThreadLocal.getCompanyId());
			}

			List<LogEntry> logEntries = logCapture.getLogEntries();

			Assert.assertEquals(logEntries.toString(), 0, logEntries.size());

			try (SafeCloseable safeCloseable1 =
					CompanyThreadLocal.setPartitionCompanyIdWithSafeCloseable(
						companyId1)) {

				try (SafeCloseable safeCloseable2 =
						CompanyThreadLocal.setRawCompanyIdWithSafeCloseable(
							companyId2);
					SafeCloseable safeCloseable3 =
						CompanyThreadLocal.
							setPartitionCompanyIdWithSafeCloseable(
								companyId1)) {

					Assert.assertEquals(
						Long.valueOf(companyId1),
						CompanyThreadLocal.getCompanyId());
				}

				Assert.assertEquals(
					logEntries.toString(), 0, logEntries.size());

				for (int i = 0; i < 2; i++) {
					try (SafeCloseable safeCloseable2 =
							CompanyThreadLocal.setCompanyIdWithSafeCloseable(
								companyId2)) {

						Assert.assertEquals(
							Long.valueOf(companyId2),
							CompanyThreadLocal.getCompanyId());
					}
				}

				Assert.assertEquals(
					logEntries.toString(), 1, logEntries.size());

				LogEntry logEntry = logEntries.get(0);

				Assert.assertEquals(
					StringBundler.concat(
						"Switching from company ID ", companyId1,
						" to company ID ", companyId2,
						" inside a database partition scope"),
					logEntry.getMessage());
				Assert.assertNotNull(logEntry.getThrowable());

				try (SafeCloseable safeCloseable2 =
						CompanyThreadLocal.setCompanyIdWithSafeCloseable(
							CompanyConstants.SYSTEM)) {

					Assert.assertEquals(
						Long.valueOf(CompanyConstants.SYSTEM),
						CompanyThreadLocal.getCompanyId());
				}

				try (SafeCloseable safeCloseable2 =
						CompanyThreadLocal.
							setPartitionCompanyIdWithSafeCloseable(
								companyId2)) {

					Assert.assertEquals(
						Long.valueOf(companyId2),
						CompanyThreadLocal.getCompanyId());
				}

				CompanyThreadLocal.setCompanyId(companyId2);

				CompanyThreadLocal.setCompanyId(companyId1);

				Assert.assertEquals(
					logEntries.toString(), 5, logEntries.size());
			}
		}
	}

	@Test
	public void testSetPartitionCompanyIdWithSafeCloseable() {
		String name = RandomTestUtil.randomString();
		long ctCollectionId = RandomTestUtil.randomLong();
		long companyId1 = RandomTestUtil.randomLong();
		long companyId2 = RandomTestUtil.randomLong();

		Long originalCompanyId = CompanyThreadLocal.getCompanyId();

		PrincipalThreadLocal.setName(name);

		try (SafeCloseable safeCloseable1 =
				CTCollectionThreadLocal.setCTCollectionIdWithSafeCloseable(
					ctCollectionId);
			SafeCloseable safeCloseable2 =
				CompanyThreadLocal.setPartitionCompanyIdWithSafeCloseable(
					companyId1)) {

			Assert.assertEquals(
				Long.valueOf(companyId1), CompanyThreadLocal.getCompanyId());
			Assert.assertEquals(
				ctCollectionId, CTCollectionThreadLocal.getCTCollectionId());
			Assert.assertEquals(name, PrincipalThreadLocal.getName());

			try (SafeCloseable safeCloseable3 =
					CompanyThreadLocal.setPartitionCompanyIdWithSafeCloseable(
						companyId2)) {

				Assert.assertEquals(
					Long.valueOf(companyId2),
					CompanyThreadLocal.getCompanyId());
			}

			Assert.assertTrue(CompanyThreadLocal.isInPartitionScope());
			Assert.assertEquals(
				Long.valueOf(companyId1), CompanyThreadLocal.getCompanyId());
		}
		finally {
			PrincipalThreadLocal.setName(null);
		}

		Assert.assertFalse(CompanyThreadLocal.isInPartitionScope());
		Assert.assertEquals(
			originalCompanyId, CompanyThreadLocal.getCompanyId());
	}

	private long _originalCompanyId;

}