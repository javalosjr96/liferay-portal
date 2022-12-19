package com.liferay.portal.verify;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.util.PropsValues;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class VerifyLayout extends VerifyProcess {
	@Override
	protected void doVerify() throws Exception {
		verifyLayoutFriendlyURL();
	}
	protected static void verifyLayoutFriendlyURL() {
		try {
			
			Connection connection = DataAccess.getConnection();

			String reservedURLS = getReservedFriendlyURLS();

			PreparedStatement preparedStatement1 = connection.prepareStatement(
				"Select groupId from Group_");

			ResultSet resultSet1 = preparedStatement1.executeQuery();

			while (resultSet1.next()) {
				long groupId = resultSet1.getLong("groupId");

				PreparedStatement preparedStatement2 =
					connection.prepareStatement(
						StringBundler.concat(
							"Select friendlyURL, plid from Layout where groupId = ? AND privateLayout in (0,1) AND ",
							"friendlyURL in (", reservedURLS, ")"));

				preparedStatement2.setLong(1, groupId);

				ResultSet resultSet2 = preparedStatement2.executeQuery();

				while (resultSet2.next()) {
					String invalidURL = resultSet2.getString("friendlyURL");
					long plid = resultSet2.getLong("plid");

					_log.error(
						StringBundler.concat(
							"Reserved layout URL detected \"", invalidURL,
							"\" Please update Layout PLID:", plid," after upgrade"));
				}
			}
		}
		catch (SQLException sqlException) {
			throw new RuntimeException(sqlException);
		}
	}

	protected static String getReservedFriendlyURLS() {
		String reservedFriendlyURLS = StringBundler.concat(
			"\"/", PropsValues.LAYOUT_FRIENDLY_URL_KEYWORDS[0], "\"");

		for (int i = 1; i < PropsValues.LAYOUT_FRIENDLY_URL_KEYWORDS.length;
			 i++) {

			reservedFriendlyURLS += StringBundler.concat(
				",\"/", PropsValues.LAYOUT_FRIENDLY_URL_KEYWORDS[i], "\"");
		}

		return reservedFriendlyURLS;
	}

	private static final Log _log = LogFactoryUtil.getLog(VerifyLayout.class);
}


