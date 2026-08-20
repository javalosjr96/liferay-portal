/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.instances.web.internal.portlet.action;

import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.service.CompanyService;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.portlet.MockActionRequest;
import com.liferay.portal.kernel.test.portlet.MockActionResponse;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.test.rule.LiferayUnitTestRule;
import com.liferay.portal.util.PortalInstances;

import jakarta.portlet.PortletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Jorge Avalos
 */
public class AddInstanceMVCActionCommandTest {

	@ClassRule
	public static LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() {
		Mockito.when(
			_jsonFactory.createJSONObject()
		).thenReturn(
			JSONFactoryUtil.createJSONObject()
		);

		Mockito.when(
			_portal.getPortletId(Mockito.any(PortletRequest.class))
		).thenReturn(
			_PORTLET_ID
		);

		_jsonPortletResponseUtilMockedStatic = Mockito.mockStatic(
			JSONPortletResponseUtil.class);
		_portalInstancesMockedStatic = Mockito.mockStatic(
			PortalInstances.class);
		_sessionMessagesMockedStatic = Mockito.mockStatic(
			SessionMessages.class);

		_portalInstancesMockedStatic.when(
			() -> PortalInstances.addCompany(
				Mockito.nullable(String.class), Mockito.any())
		).thenReturn(
			Mockito.mock(Company.class)
		);

		ReflectionTestUtil.setFieldValue(
			_addInstanceMVCActionCommand, "_companyService", _companyService);
		ReflectionTestUtil.setFieldValue(
			_addInstanceMVCActionCommand, "_jsonFactory", _jsonFactory);
		ReflectionTestUtil.setFieldValue(
			_addInstanceMVCActionCommand, "_portal", _portal);
	}

	@After
	public void tearDown() {
		_jsonPortletResponseUtilMockedStatic.close();
		_portalInstancesMockedStatic.close();
		_sessionMessagesMockedStatic.close();
	}

	@Test
	public void testClearedHiddenDefaultSuccessMessageOnSuccess()
		throws Exception {

		_sessionMessagesMockedStatic.when(
			() -> SessionMessages.contains(
				Mockito.any(PortletRequest.class),
				Mockito.eq(
					_PORTLET_ID +
						SessionMessages.
							KEY_SUFFIX_HIDE_DEFAULT_SUCCESS_MESSAGE))
		).thenReturn(
			true
		);

		_addInstanceMVCActionCommand.doProcessAction(
			new MockActionRequest(), new MockActionResponse());

		_sessionMessagesMockedStatic.verify(
			() -> SessionMessages.clear(Mockito.any(PortletRequest.class)));
	}

	@Test
	public void testUnclearedHiddenDefaultSuccessMessageOnSuccess()
		throws Exception {

		_sessionMessagesMockedStatic.when(
			() -> SessionMessages.contains(
				Mockito.any(PortletRequest.class), Mockito.anyString())
		).thenReturn(
			false
		);

		_addInstanceMVCActionCommand.doProcessAction(
			new MockActionRequest(), new MockActionResponse());

		_sessionMessagesMockedStatic.verify(
			() -> SessionMessages.clear(Mockito.any(PortletRequest.class)),
			Mockito.never());
	}

	private static final String _PORTLET_ID = RandomTestUtil.randomString();

	private final AddInstanceMVCActionCommand _addInstanceMVCActionCommand =
		new AddInstanceMVCActionCommand();
	private final CompanyService _companyService = Mockito.mock(
		CompanyService.class);
	private final JSONFactory _jsonFactory = Mockito.mock(JSONFactory.class);
	private MockedStatic<JSONPortletResponseUtil>
		_jsonPortletResponseUtilMockedStatic;
	private final Portal _portal = Mockito.mock(Portal.class);
	private MockedStatic<PortalInstances> _portalInstancesMockedStatic;
	private MockedStatic<SessionMessages> _sessionMessagesMockedStatic;

}