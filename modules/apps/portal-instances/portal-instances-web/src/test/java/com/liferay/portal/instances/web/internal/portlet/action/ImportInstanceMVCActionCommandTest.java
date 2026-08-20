/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.instances.web.internal.portlet.action;

import com.liferay.petra.lang.SafeCloseable;
import com.liferay.portal.kernel.exception.CompanyNameException;
import com.liferay.portal.kernel.exception.CompanyVirtualHostException;
import com.liferay.portal.kernel.exception.CompanyWebIdException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.feature.flag.FeatureFlagManagerUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.service.CompanyService;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.portlet.MockActionRequest;
import com.liferay.portal.kernel.test.portlet.MockActionResponse;
import com.liferay.portal.kernel.test.util.PropsValuesTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import jakarta.portlet.ActionRequest;
import jakarta.portlet.ActionResponse;
import jakarta.portlet.PortletRequest;

import java.util.Locale;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Jorge Avalos
 */
public class ImportInstanceMVCActionCommandTest {

	@ClassRule
	public static LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() {
		Mockito.when(
			_company.getCompanyId()
		).thenReturn(
			_COMPANY_ID
		);

		Mockito.when(
			_company.getWebId()
		).thenReturn(
			_COMPANY_WEB_ID
		);

		Mockito.when(
			_language.format(
				Mockito.nullable(Locale.class), Mockito.anyString(),
				Mockito.<Object>any())
		).thenAnswer(
			invocationOnMock ->
				invocationOnMock.getArgument(1) + ":" +
					invocationOnMock.getArgument(2)
		);

		Mockito.when(
			_language.get(Mockito.nullable(Locale.class), Mockito.anyString())
		).thenAnswer(
			invocationOnMock -> invocationOnMock.getArgument(1)
		);

		Mockito.when(
			_portal.getPortletId(Mockito.any(PortletRequest.class))
		).thenReturn(
			_PORTLET_ID
		);

		_featureFlagManagerUtilMockedStatic = Mockito.mockStatic(
			FeatureFlagManagerUtil.class);

		_featureFlagManagerUtilMockedStatic.when(
			() -> FeatureFlagManagerUtil.isEnabled(
				Mockito.anyLong(), Mockito.eq("LPD-11342"))
		).thenReturn(
			true
		);

		_jsonPortletResponseUtilMockedStatic = Mockito.mockStatic(
			JSONPortletResponseUtil.class);

		_jsonPortletResponseUtilMockedStatic.when(
			() -> JSONPortletResponseUtil.writeJSON(
				Mockito.any(ActionRequest.class),
				Mockito.any(ActionResponse.class),
				Mockito.any(JSONObject.class))
		).then(
			invocationOnMock -> {
				_jsonObject = invocationOnMock.getArgument(2);

				return null;
			}
		);

		_sessionMessagesMockedStatic = Mockito.mockStatic(
			SessionMessages.class);

		ReflectionTestUtil.setFieldValue(
			_importInstanceMVCActionCommand, "_companyService",
			_companyService);
		ReflectionTestUtil.setFieldValue(
			_importInstanceMVCActionCommand, "_language", _language);
		ReflectionTestUtil.setFieldValue(
			_importInstanceMVCActionCommand, "_portal", _portal);
	}

	@After
	public void tearDown() {
		_featureFlagManagerUtilMockedStatic.close();
		_jsonPortletResponseUtilMockedStatic.close();
		_sessionMessagesMockedStatic.close();
	}

	@Test
	public void testClearedHiddenDefaultSuccessMessageOnSuccess()
		throws Exception {

		Mockito.when(
			_companyService.addDBPartitionCompany(
				_SCHEMA_NAME, _NAME, _VIRTUAL_HOSTNAME, _WEB_ID)
		).thenReturn(
			_company
		);

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

		_importInstanceMVCActionCommand.doProcessAction(
			_getMockActionRequest(), new MockActionResponse());

		_sessionMessagesMockedStatic.verify(
			() -> SessionMessages.clear(Mockito.any(PortletRequest.class)));
	}

	@Test
	public void testCompanyIdOnSuccess() throws Exception {
		Mockito.when(
			_companyService.addDBPartitionCompany(
				_SCHEMA_NAME, _NAME, _VIRTUAL_HOSTNAME, _WEB_ID)
		).thenReturn(
			_company
		);

		MockActionRequest mockActionRequest = _getMockActionRequest();

		_importInstanceMVCActionCommand.doProcessAction(
			mockActionRequest, new MockActionResponse());

		Assert.assertEquals(_COMPANY_ID, _jsonObject.getLong("companyId"));

		Assert.assertEquals(0, _hideDefaultSuccessMessageCount);

		Assert.assertFalse(_jsonObject.has("error"));

		Mockito.verify(
			_companyService
		).addDBPartitionCompany(
			_SCHEMA_NAME, _NAME, _VIRTUAL_HOSTNAME, _WEB_ID
		);

		_jsonPortletResponseUtilMockedStatic.verify(
			() -> JSONPortletResponseUtil.writeJSON(
				Mockito.any(ActionRequest.class),
				Mockito.any(ActionResponse.class), Mockito.eq(_jsonObject)));

		_sessionMessagesMockedStatic.verify(
			() -> SessionMessages.add(
				mockActionRequest, "requestProcessed",
				"the-instance-was-imported-to-x:" + _COMPANY_WEB_ID));
	}

	@Test
	public void testErrorForCompanyException() throws Exception {
		_assertError(new CompanyNameException(), "please-enter-a-valid-name");
		_assertError(
			new CompanyVirtualHostException(),
			"please-enter-a-valid-virtual-host");
		_assertError(
			new CompanyWebIdException(), "please-enter-a-valid-web-id");
	}

	@Test
	public void testErrorForIllegalArgumentException() throws Exception {
		_assertError(
			new IllegalArgumentException(), "please-enter-a-valid-schema-name");
	}

	@Test
	public void testErrorForUnmappedException() throws Exception {
		_assertError(new RuntimeException(), "an-unexpected-error-occurred");
		_assertError(
			new PrincipalException.MustBeOmniadmin(_permissionChecker),
			"an-unexpected-error-occurred");
	}

	@Test
	public void testErrorForUnsupportedOperationException() throws Exception {
		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"DATABASE_PARTITION_ENABLED", false)) {

			_assertError(
				new UnsupportedOperationException(),
				"database-partitioning-must-be-enabled");
		}

		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"DATABASE_PARTITION_ENABLED", true)) {

			_assertError(
				new UnsupportedOperationException(),
				"importing-an-instance-is-already-in-progress");
		}
	}

	@Test
	public void testErrorForWrappedCompanyException() throws Exception {
		_assertError(
			new PortalException(new CompanyNameException()),
			"please-enter-a-valid-name");
		_assertError(
			new PortalException(new CompanyVirtualHostException()),
			"please-enter-a-valid-virtual-host");
		_assertError(
			new PortalException(new CompanyWebIdException()),
			"please-enter-a-valid-web-id");
	}

	@Test
	public void testUnclearedHiddenDefaultSuccessMessageOnSuccess()
		throws Exception {

		Mockito.when(
			_companyService.addDBPartitionCompany(
				_SCHEMA_NAME, _NAME, _VIRTUAL_HOSTNAME, _WEB_ID)
		).thenReturn(
			_company
		);

		_sessionMessagesMockedStatic.when(
			() -> SessionMessages.contains(
				Mockito.any(PortletRequest.class), Mockito.anyString())
		).thenReturn(
			false
		);

		_importInstanceMVCActionCommand.doProcessAction(
			_getMockActionRequest(), new MockActionResponse());

		_sessionMessagesMockedStatic.verify(
			() -> SessionMessages.clear(Mockito.any(PortletRequest.class)),
			Mockito.never());
	}

	@Test
	public void testUnsupportedOperationExceptionForDisabledFeatureFlag() {
		_featureFlagManagerUtilMockedStatic.when(
			() -> FeatureFlagManagerUtil.isEnabled(
				Mockito.anyLong(), Mockito.eq("LPD-11342"))
		).thenReturn(
			false
		);

		Assert.assertThrows(
			UnsupportedOperationException.class,
			() -> _importInstanceMVCActionCommand.doProcessAction(
				_getMockActionRequest(), new MockActionResponse()));

		Mockito.verifyNoInteractions(_companyService);
	}

	private void _assertError(Exception exception, String expectedError)
		throws Exception {

		_hideDefaultSuccessMessageCount = 0;

		Mockito.doThrow(
			exception
		).when(
			_companyService
		).addDBPartitionCompany(
			_SCHEMA_NAME, _NAME, _VIRTUAL_HOSTNAME, _WEB_ID
		);

		_importInstanceMVCActionCommand.doProcessAction(
			_getMockActionRequest(), new MockActionResponse());

		Assert.assertEquals(expectedError, _jsonObject.getString("error"));
		Assert.assertEquals(1, _hideDefaultSuccessMessageCount);
		Assert.assertFalse(_jsonObject.has("companyId"));

		_jsonPortletResponseUtilMockedStatic.verify(
			() -> JSONPortletResponseUtil.writeJSON(
				Mockito.any(ActionRequest.class),
				Mockito.any(ActionResponse.class), Mockito.eq(_jsonObject)));
	}

	private MockActionRequest _getMockActionRequest() {
		MockActionRequest mockActionRequest = new MockActionRequest();

		mockActionRequest.addParameter("name", _NAME);
		mockActionRequest.addParameter("schemaName", _SCHEMA_NAME);
		mockActionRequest.addParameter("virtualHostname", _VIRTUAL_HOSTNAME);
		mockActionRequest.addParameter("webId", _WEB_ID);
		mockActionRequest.setAttribute(
			WebKeys.THEME_DISPLAY, Mockito.mock(ThemeDisplay.class));

		return mockActionRequest;
	}

	private static final long _COMPANY_ID = RandomTestUtil.randomLong();

	private static final String _COMPANY_WEB_ID = RandomTestUtil.randomString();

	private static final String _NAME = RandomTestUtil.randomString();

	private static final String _PORTLET_ID = RandomTestUtil.randomString();

	private static final String _SCHEMA_NAME = RandomTestUtil.randomString();

	private static final String _VIRTUAL_HOSTNAME =
		RandomTestUtil.randomString();

	private static final String _WEB_ID = RandomTestUtil.randomString();

	private final Company _company = Mockito.mock(Company.class);
	private final CompanyService _companyService = Mockito.mock(
		CompanyService.class);
	private MockedStatic<FeatureFlagManagerUtil>
		_featureFlagManagerUtilMockedStatic;
	private int _hideDefaultSuccessMessageCount;

	private final ImportInstanceMVCActionCommand
		_importInstanceMVCActionCommand = new ImportInstanceMVCActionCommand() {

			@Override
			protected void hideDefaultSuccessMessage(
				PortletRequest portletRequest) {

				_hideDefaultSuccessMessageCount++;
			}

		};

	private JSONObject _jsonObject;
	private MockedStatic<JSONPortletResponseUtil>
		_jsonPortletResponseUtilMockedStatic;
	private final Language _language = Mockito.mock(Language.class);
	private final PermissionChecker _permissionChecker = Mockito.mock(
		PermissionChecker.class);
	private final Portal _portal = Mockito.mock(Portal.class);
	private MockedStatic<SessionMessages> _sessionMessagesMockedStatic;

}