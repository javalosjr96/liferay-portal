/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.instances.web.internal.portlet.action;

import com.liferay.portal.instances.web.internal.constants.PortalInstancesPortletKeys;
import com.liferay.portal.kernel.exception.CompanyNameException;
import com.liferay.portal.kernel.exception.CompanyVirtualHostException;
import com.liferay.portal.kernel.exception.CompanyWebIdException;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.service.CompanyService;
import com.liferay.portal.kernel.util.ParamUtil;

import jakarta.portlet.ActionRequest;
import jakarta.portlet.ActionResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Jorge Ávalos
 */
@Component(
	property = {
		"jakarta.portlet.name=" + PortalInstancesPortletKeys.PORTAL_INSTANCES,
		"mvc.command.name=/portal_instances/copy_instance"
	},
	service = MVCActionCommand.class
)
public class CopyInstanceMVCActionCommand extends BaseMVCActionCommand {

	@Override
	protected void doProcessAction(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		JSONObject jsonObject = _jsonFactory.createJSONObject();

		try {
			_copyInstance(actionRequest);
		}
		catch (Exception exception) {
			if (exception instanceof CompanyNameException ||
				exception instanceof CompanyVirtualHostException ||
				exception instanceof CompanyWebIdException ||
				exception instanceof IllegalArgumentException) {

				if (_log.isDebugEnabled()) {
					_log.debug(exception);
				}
			}
			else {
				_log.error("Unable to copy portal instance", exception);
			}

			if (exception instanceof IllegalArgumentException) {
				jsonObject.put("error", exception.getMessage());
			}
			else {
				String errorMessage = "an-unexpected-error-occurred";

				if (exception instanceof CompanyNameException) {
					errorMessage = "please-enter-a-valid-name";
				}
				else if (exception instanceof CompanyVirtualHostException) {
					errorMessage = "please-enter-a-valid-virtual-host";
				}
				else if (exception instanceof CompanyWebIdException) {
					errorMessage = "please-enter-a-valid-web-id";
				}

				jsonObject.put(
					"error",
					_language.get(actionRequest.getLocale(), errorMessage));
			}

			hideDefaultSuccessMessage(actionRequest);
		}

		JSONPortletResponseUtil.writeJSON(
			actionRequest, actionResponse, jsonObject);
	}

	private void _copyInstance(ActionRequest actionRequest) throws Exception {
		long sourceCompanyId = ParamUtil.getLong(
			actionRequest, "sourceCompanyId");
		long destinationCompanyId = ParamUtil.getLong(
			actionRequest, "destinationCompanyId");
		String name = ParamUtil.getString(actionRequest, "name");
		String virtualHostname = ParamUtil.getString(
			actionRequest, "virtualHostname");
		String webId = ParamUtil.getString(actionRequest, "webId");

		_companyService.copyDBPartitionCompany(
			sourceCompanyId,
			(destinationCompanyId > 0) ? destinationCompanyId : null, name,
			virtualHostname, webId);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		CopyInstanceMVCActionCommand.class);

	@Reference
	private CompanyService _companyService;

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private Language _language;

}