/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.instances.web.internal.portlet.action;

import com.liferay.portal.json.JSONObjectImpl;
import com.liferay.portal.kernel.exception.CompanyNameException;
import com.liferay.portal.kernel.exception.CompanyVirtualHostException;
import com.liferay.portal.kernel.exception.CompanyWebIdException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.feature.flag.FeatureFlagManagerUtil;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.service.CompanyService;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.portlet.MockActionRequest;
import com.liferay.portal.kernel.test.portlet.MockActionResponse;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.GetterUtil;
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
public class CopyInstanceMVCActionCommandTest {

	@ClassRule
	public static LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() {
		_jsonObject = new JSONObjectImpl();

		Mockito.when(
			_jsonFactory.createJSONObject()
		).thenReturn(
			_jsonObject
		);

		Mockito.when(
			_language.get(Mockito.nullable(Locale.class), Mockito.anyString())
		).thenAnswer(
			invocationOnMock -> invocationOnMock.getArgument(1)
		);

		ReflectionTestUtil.setFieldValue(
			_copyInstanceMVCActionCommand, "_companyService", _companyService);
		ReflectionTestUtil.setFieldValue(
			_copyInstanceMVCActionCommand, "_jsonFactory", _jsonFactory);
		ReflectionTestUtil.setFieldValue(
			_copyInstanceMVCActionCommand, "_language", _language);

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
			invocationOnMock -> null
		);
	}

	@After
	public void tearDown() {
		_featureFlagManagerUtilMockedStatic.close();
		_jsonPortletResponseUtilMockedStatic.close();
	}

	@Test
	public void testCompanyIdOnSuccess() throws Exception {
		long companyId = RandomTestUtil.randomLong();

		Company company = Mockito.mock(Company.class);

		Mockito.when(
			company.getCompanyId()
		).thenReturn(
			companyId
		);

		Mockito.when(
			_companyService.copyDBPartitionCompany(
				Mockito.anyLong(), Mockito.nullable(Long.class),
				Mockito.anyString(), Mockito.anyString(), Mockito.anyString())
		).thenReturn(
			company
		);

		MockActionRequest mockActionRequest = _getMockActionRequest();

		_copyInstanceMVCActionCommand.doProcessAction(
			mockActionRequest, new MockActionResponse());

		Assert.assertEquals(companyId, _jsonObject.getLong("companyId"));

		Mockito.verify(
			_companyService
		).copyDBPartitionCompany(
			GetterUtil.getLong(
				mockActionRequest.getParameter("sourceCompanyId")),
			GetterUtil.getLong(
				mockActionRequest.getParameter("destinationCompanyId")),
			mockActionRequest.getParameter("name"),
			mockActionRequest.getParameter("virtualHostname"),
			mockActionRequest.getParameter("webId")
		);

		_jsonPortletResponseUtilMockedStatic.verify(
			() -> JSONPortletResponseUtil.writeJSON(
				Mockito.any(ActionRequest.class),
				Mockito.any(ActionResponse.class), Mockito.eq(_jsonObject)));
	}

	@Test
	public void testDatabasePartitioningErrorForUnsupportedOperationException()
		throws Exception {

		_assertErrorMessage(
			new UnsupportedOperationException(
				"Database partitioning must be enabled"),
			"database-partitioning-must-be-enabled", _getMockActionRequest());
	}

	@Test
	public void testDestinationCompanyIdErrorForIllegalArgumentException()
		throws Exception {

		_assertErrorMessage(
			new IllegalArgumentException(),
			"please-enter-a-valid-destination-company-id",
			_getMockActionRequest());
	}

	@Test
	public void testNameErrorForCompanyNameException() throws Exception {
		_assertErrorMessage(
			new PortalException(new CompanyNameException()),
			"please-enter-a-valid-name", _getMockActionRequest());
	}

	@Test
	public void testNameErrorForDirectCompanyNameException() throws Exception {
		_assertErrorMessage(
			new CompanyNameException(), "please-enter-a-valid-name",
			_getMockActionRequest());
	}

	@Test
	public void testNullDestinationCompanyIdOnSuccess() throws Exception {
		long companyId = RandomTestUtil.randomLong();

		Company company = Mockito.mock(Company.class);

		Mockito.when(
			company.getCompanyId()
		).thenReturn(
			companyId
		);

		Mockito.when(
			_companyService.copyDBPartitionCompany(
				Mockito.anyLong(), Mockito.nullable(Long.class),
				Mockito.anyString(), Mockito.anyString(), Mockito.anyString())
		).thenReturn(
			company
		);

		MockActionRequest mockActionRequest = _getMockActionRequest();

		mockActionRequest.setParameter("destinationCompanyId", "0");

		_copyInstanceMVCActionCommand.doProcessAction(
			mockActionRequest, new MockActionResponse());

		Assert.assertEquals(companyId, _jsonObject.getLong("companyId"));

		Mockito.verify(
			_companyService
		).copyDBPartitionCompany(
			GetterUtil.getLong(
				mockActionRequest.getParameter("sourceCompanyId")),
			null, mockActionRequest.getParameter("name"),
			mockActionRequest.getParameter("virtualHostname"),
			mockActionRequest.getParameter("webId")
		);
	}

	@Test
	public void testUnexpectedErrorForIllegalArgumentExceptionWithoutDestinationCompanyId()
		throws Exception {

		MockActionRequest mockActionRequest = _getMockActionRequest();

		mockActionRequest.setParameter("destinationCompanyId", "0");

		_assertErrorMessage(
			new IllegalArgumentException(), "an-unexpected-error-occurred",
			mockActionRequest);
	}

	@Test
	public void testUnexpectedErrorForUnmappedException() throws Exception {
		_assertErrorMessage(
			new RuntimeException(), "an-unexpected-error-occurred",
			_getMockActionRequest());
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
			() -> _copyInstanceMVCActionCommand.doProcessAction(
				_getMockActionRequest(), new MockActionResponse()));

		Mockito.verifyNoInteractions(_companyService);
	}

	@Test
	public void testVirtualHostErrorForCompanyVirtualHostException()
		throws Exception {

		_assertErrorMessage(
			new PortalException(new CompanyVirtualHostException()),
			"please-enter-a-valid-virtual-host", _getMockActionRequest());
	}

	@Test
	public void testVirtualHostErrorForDirectCompanyVirtualHostException()
		throws Exception {

		_assertErrorMessage(
			new CompanyVirtualHostException(),
			"please-enter-a-valid-virtual-host", _getMockActionRequest());
	}

	@Test
	public void testWebIdErrorForCompanyWebIdException() throws Exception {
		_assertErrorMessage(
			new PortalException(new CompanyWebIdException()),
			"please-enter-a-valid-web-id", _getMockActionRequest());
	}

	@Test
	public void testWebIdErrorForDirectCompanyWebIdException()
		throws Exception {

		_assertErrorMessage(
			new CompanyWebIdException(), "please-enter-a-valid-web-id",
			_getMockActionRequest());
	}

	private void _assertErrorMessage(
			Exception exception, String expectedErrorMessage,
			MockActionRequest mockActionRequest)
		throws Exception {

		Mockito.when(
			_companyService.copyDBPartitionCompany(
				Mockito.anyLong(), Mockito.nullable(Long.class),
				Mockito.anyString(), Mockito.anyString(), Mockito.anyString())
		).thenThrow(
			exception
		);

		_copyInstanceMVCActionCommand.doProcessAction(
			mockActionRequest, new MockActionResponse());

		Assert.assertEquals(
			expectedErrorMessage, _jsonObject.getString("error"));

		_jsonPortletResponseUtilMockedStatic.verify(
			() -> JSONPortletResponseUtil.writeJSON(
				Mockito.any(ActionRequest.class),
				Mockito.any(ActionResponse.class), Mockito.eq(_jsonObject)));
	}

	private MockActionRequest _getMockActionRequest() {
		MockActionRequest mockActionRequest = new MockActionRequest();

		mockActionRequest.addParameter(
			"destinationCompanyId",
			String.valueOf(RandomTestUtil.randomLong()));
		mockActionRequest.addParameter("name", RandomTestUtil.randomString());
		mockActionRequest.addParameter(
			"sourceCompanyId", String.valueOf(RandomTestUtil.randomLong()));
		mockActionRequest.addParameter(
			"virtualHostname", RandomTestUtil.randomString());
		mockActionRequest.addParameter("webId", RandomTestUtil.randomString());
		mockActionRequest.setAttribute(
			WebKeys.THEME_DISPLAY, Mockito.mock(ThemeDisplay.class));

		return mockActionRequest;
	}

	private final CompanyService _companyService = Mockito.mock(
		CompanyService.class);

	private final CopyInstanceMVCActionCommand _copyInstanceMVCActionCommand =
		new CopyInstanceMVCActionCommand() {

			@Override
			protected void hideDefaultSuccessMessage(
				PortletRequest portletRequest) {
			}

		};

	private MockedStatic<FeatureFlagManagerUtil>
		_featureFlagManagerUtilMockedStatic;
	private final JSONFactory _jsonFactory = Mockito.mock(JSONFactory.class);
	private JSONObject _jsonObject;
	private MockedStatic<JSONPortletResponseUtil>
		_jsonPortletResponseUtilMockedStatic;
	private final Language _language = Mockito.mock(Language.class);

}