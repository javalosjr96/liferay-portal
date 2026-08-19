/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.instances.web.internal.display.context;

import com.liferay.frontend.taglib.clay.servlet.taglib.util.CreationMenu;
import com.liferay.frontend.taglib.clay.servlet.taglib.util.DropdownItem;
import com.liferay.petra.lang.SafeCloseable;
import com.liferay.portal.kernel.feature.flag.FeatureFlagManagerUtil;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.portlet.LiferayPortletRequest;
import com.liferay.portal.kernel.portlet.LiferayPortletResponse;
import com.liferay.portal.kernel.portlet.PortletURLUtil;
import com.liferay.portal.kernel.test.util.PropsValuesTestUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import jakarta.portlet.PortletURL;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Jorge Avalos
 */
public class PortalInstancesManagementToolbarDisplayContextTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() {
		_featureFlagManagerUtilMockedStatic = Mockito.mockStatic(
			FeatureFlagManagerUtil.class);
		_languageUtilMockedStatic = Mockito.mockStatic(LanguageUtil.class);
		_portalUtilMockedStatic = Mockito.mockStatic(PortalUtil.class);
		_portletURLUtilMockedStatic = Mockito.mockStatic(PortletURLUtil.class);

		_featureFlagManagerUtilMockedStatic.when(
			() -> FeatureFlagManagerUtil.isEnabled(
				Mockito.anyLong(), Mockito.eq("LPD-11342"))
		).thenReturn(
			true
		);

		Mockito.when(
			_liferayPortletResponse.createRenderURL()
		).thenReturn(
			_liferayPortletURL
		);
	}

	@After
	public void tearDown() {
		_featureFlagManagerUtilMockedStatic.close();
		_languageUtilMockedStatic.close();
		_portalUtilMockedStatic.close();
		_portletURLUtilMockedStatic.close();
	}

	@Test
	public void testGetCreationMenu() throws Exception {
		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"DATABASE_PARTITION_ENABLED", true)) {

			Assert.assertEquals(2, _getDropdownItemsCount());
		}

		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"DATABASE_PARTITION_ENABLED", false)) {

			Assert.assertEquals(1, _getDropdownItemsCount());
		}
	}

	private int _getDropdownItemsCount() {
		PortalInstancesManagementToolbarDisplayContext
			portalInstancesManagementToolbarDisplayContext =
				new PortalInstancesManagementToolbarDisplayContext(
					_httpServletRequest, _liferayPortletRequest,
					_liferayPortletResponse);

		CreationMenu creationMenu =
			portalInstancesManagementToolbarDisplayContext.getCreationMenu();

		List<DropdownItem> dropdownItems =
			(List<DropdownItem>)creationMenu.get("primaryItems");

		return dropdownItems.size();
	}

	private MockedStatic<FeatureFlagManagerUtil>
		_featureFlagManagerUtilMockedStatic;
	private final HttpServletRequest _httpServletRequest = Mockito.mock(
		HttpServletRequest.class);
	private MockedStatic<LanguageUtil> _languageUtilMockedStatic;
	private final LiferayPortletRequest _liferayPortletRequest = Mockito.mock(
		LiferayPortletRequest.class);
	private final LiferayPortletResponse _liferayPortletResponse = Mockito.mock(
		LiferayPortletResponse.class);
	private final PortletURL _liferayPortletURL = Mockito.mock(
		PortletURL.class);
	private MockedStatic<PortalUtil> _portalUtilMockedStatic;
	private MockedStatic<PortletURLUtil> _portletURLUtilMockedStatic;

}
