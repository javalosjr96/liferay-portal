/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.security.auth;

import com.liferay.petra.lang.SafeCloseable;
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
import java.util.TimeZone;
import java.util.function.Consumer;

import org.junit.Assert;
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
	public void testLock() {
		_testLock(CompanyThreadLocal::setCompanyId);
	}

	@Test
	public void testLockWithSetWithSafeCloseable() {
		_testLock(CompanyThreadLocal::setCompanyIdWithSafeCloseable);
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
		long companyId3 = RandomTestUtil.randomLong();

		try (LogCapture logCapture = LoggerTestUtil.configureLog4JLogger(
				CompanyThreadLocal.class.getName(), LoggerTestUtil.WARN)) {

			try (SafeCloseable safeCloseable =
					CompanyThreadLocal.setCompanyIdWithSafeCloseable(
						companyId2)) {

				Assert.assertEquals(
					Long.valueOf(companyId2), CompanyThreadLocal.getCompanyId());
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
					"Switching from company ID " + companyId1 +
						" to company ID " + companyId2 +
							" inside a database partition scope",
					logEntry.getMessage());
				Assert.assertNotNull(logEntry.getThrowable());

				try (SafeCloseable safeCloseable2 = CompanyThreadLocal.lock(
						companyId3)) {

					Assert.assertTrue(CompanyThreadLocal.isLocked());
				}

				try (SafeCloseable safeCloseable2 =
						CompanyThreadLocal.setCompanyIdWithSafeCloseable(
							CompanyConstants.SYSTEM)) {

					Assert.assertEquals(
						Long.valueOf(CompanyConstants.SYSTEM),
						CompanyThreadLocal.getCompanyId());
				}

				Assert.assertEquals(
					logEntries.toString(), 3, logEntries.size());
			}
		}
	}

	@Test
	public void testSetPartitionCompanyIdWithSafeCloseable() {
		long companyId1 = RandomTestUtil.randomLong();
		long companyId2 = RandomTestUtil.randomLong();
		long ctCollectionId = RandomTestUtil.randomLong();
		String name = RandomTestUtil.randomString();
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
		Assert.assertEquals(originalCompanyId, CompanyThreadLocal.getCompanyId());
	}

	@Test
	public void testUnlock() {
		try (SafeCloseable safeCloseable1 = CompanyThreadLocal.lock(
				CompanyConstants.SYSTEM)) {

			try (SafeCloseable safeCloseable2 = CompanyThreadLocal.unlock();
				SafeCloseable safeCloseable3 =
					CompanyThreadLocal.setCompanyIdWithSafeCloseable(1L)) {

				Assert.assertEquals(
					Long.valueOf(1), CompanyThreadLocal.getCompanyId());
			}

			Assert.assertTrue(CompanyThreadLocal.isLocked());
		}

		try (SafeCloseable safeCloseable = CompanyThreadLocal.unlock()) {
			Assert.assertFalse(CompanyThreadLocal.isLocked());
		}

		Assert.assertFalse(CompanyThreadLocal.isLocked());
	}

	private void _testLock(Consumer<Long> consumer) {
		long companyId = RandomTestUtil.randomLong();

		try (SafeCloseable safeCloseable = CompanyThreadLocal.lock(
				CompanyConstants.SYSTEM)) {

			consumer.accept(companyId);

			Assert.assertEquals(
				Long.valueOf(companyId), CompanyThreadLocal.getCompanyId());
			Assert.assertTrue(CompanyThreadLocal.isLocked());
		}
	}

	private long _originalCompanyId;

}