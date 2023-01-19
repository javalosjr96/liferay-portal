/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.verify;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.util.PropsValues;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author Jorge Avalos
 */
public class VerifyLayout extends VerifyProcess {

	@Override
	protected void doVerify() throws Exception {
		verifyLayoutFriendlyURL();
	}

	protected String getReservedLayoutFriendlyURLS() {
		String reservedLayoutFriendlyURLS = "";
		String wildCard = "";

		for (int i = 0; i < PropsValues.LAYOUT_FRIENDLY_URL_KEYWORDS.length;
			 i++) {

			wildCard = PropsValues.LAYOUT_FRIENDLY_URL_KEYWORDS[i];

			if (PropsValues.LAYOUT_FRIENDLY_URL_KEYWORDS[i].contains(StringPool.STAR)) {
				wildCard = StringUtil.replace(wildCard, StringPool.STAR, StringPool.PERCENT);
			}

			if (PropsValues.LAYOUT_FRIENDLY_URL_KEYWORDS[i].contains("_")) {
				wildCard = StringUtil.replace(wildCard, StringPool.UNDERLINE, "!_");
			}

			if (reservedLayoutFriendlyURLS.isEmpty()) {
				reservedLayoutFriendlyURLS += StringBundler.concat(
					"LIKE '/", wildCard, "' ");
			}
			else {
				reservedLayoutFriendlyURLS += StringBundler.concat(
					"OR friendlyURL LIKE '/", wildCard, "'");
			}
		}

		reservedLayoutFriendlyURLS += "ESCAPE \'!\'";

		return reservedLayoutFriendlyURLS;
	}

	protected void verifyLayoutFriendlyURL() throws Exception {
		String sql =
			"Select * from Layout where friendlyURL " +
				getReservedLayoutFriendlyURLS();

		sql = PortalUtil.transformSQL(sql);

		PreparedStatement preparedStatement = connection.prepareStatement(sql);

		ResultSet resultSet = preparedStatement.executeQuery();

		while (resultSet.next()) {
			String invalidURL = resultSet.getString("friendlyURL");
			long plid = resultSet.getLong("plid");

			_log.error(
				StringBundler.concat(
					"Reserved layout URL detected \"", invalidURL,
					"\" Please update the friendly URL for the layout with ",
					"plid ", plid));
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(VerifyLayout.class);

}