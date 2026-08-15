/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.instances.web.internal.portlet.action;

import com.liferay.portal.instances.web.internal.constants.PortalInstancesPortletKeys;
import com.liferay.portal.kernel.exception.CompanyNameException;
import com.liferay.portal.kernel.exception.CompanyVirtualHostException;
import com.liferay.portal.kernel.exception.CompanyWebIdException;
import com.liferay.portal.kernel.feature.flag.FeatureFlagManagerUtil;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.service.CompanyService;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.WebKeys;

import jakarta.portlet.ActionRequest;
import jakarta.portlet.ActionResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Jorge Avalos
 */
@Component(
	property = {
		"jakarta.portlet.name=" + PortalInstancesPortletKeys.PORTAL_INSTANCES,
		"mvc.command.name=/portal_instances/import_instance"
	},
	service = MVCActionCommand.class
)
public class ImportInstanceMVCActionCommand extends BaseMVCActionCommand {

	@Override
	protected void doProcessAction(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		if (!FeatureFlagManagerUtil.isEnabled(
				themeDisplay.getCompanyId(), "LPD-11342")) {

			throw new UnsupportedOperationException();
		}

		try {
			Company company = _importInstance(actionRequest);

			if (SessionMessages.contains(
					actionRequest,
					_portal.getPortletId(actionRequest) +
						SessionMessages.
							KEY_SUFFIX_HIDE_DEFAULT_SUCCESS_MESSAGE)) {

				SessionMessages.clear(actionRequest);
			}

			SessionMessages.add(
				actionRequest, "requestProcessed",
				_language.format(
					actionRequest.getLocale(), "the-instance-was-imported-to-x",
					company.getWebId()));

			JSONPortletResponseUtil.writeJSON(
				actionRequest, actionResponse,
				JSONUtil.put("companyId", company.getCompanyId()));
		}
		catch (Exception exception) {
			_log.error("Unable to import portal instance", exception);

			String errorMessage = "an-unexpected-error-occurred";

			if (exception instanceof IllegalArgumentException) {
				errorMessage = "please-enter-a-valid-schema-name";
			}
			else if (exception instanceof UnsupportedOperationException) {
				if (PropsValues.DATABASE_PARTITION_ENABLED) {
					errorMessage =
						"importing-an-instance-is-already-in-progress";
				}
				else {
					errorMessage = "database-partitioning-must-be-enabled";
				}
			}
			else {
				Throwable causeThrowable = exception.getCause();

				if ((exception instanceof CompanyNameException) ||
					(causeThrowable instanceof CompanyNameException)) {

					errorMessage = "please-enter-a-valid-name";
				}
				else if ((exception instanceof CompanyVirtualHostException) ||
						 (causeThrowable instanceof
							 CompanyVirtualHostException)) {

					errorMessage = "please-enter-a-valid-virtual-host";
				}
				else if ((exception instanceof CompanyWebIdException) ||
						 (causeThrowable instanceof CompanyWebIdException)) {

					errorMessage = "please-enter-a-valid-web-id";
				}
			}

			JSONPortletResponseUtil.writeJSON(
				actionRequest, actionResponse,
				JSONUtil.put(
					"error",
					_language.get(actionRequest.getLocale(), errorMessage)));

			hideDefaultSuccessMessage(actionRequest);
		}
	}

	private Company _importInstance(ActionRequest actionRequest)
		throws Exception {

		String schemaName = ParamUtil.getString(actionRequest, "schemaName");
		String name = ParamUtil.getString(actionRequest, "name", null);
		String virtualHostname = ParamUtil.getString(
			actionRequest, "virtualHostname", null);
		String webId = ParamUtil.getString(actionRequest, "webId", null);

		return _companyService.addDBPartitionCompany(
			schemaName, name, virtualHostname, webId);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ImportInstanceMVCActionCommand.class);

	@Reference
	private CompanyService _companyService;

	@Reference
	private Language _language;

	@Reference
	private Portal _portal;

}