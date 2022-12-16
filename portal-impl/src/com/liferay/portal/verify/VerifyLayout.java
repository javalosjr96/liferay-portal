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
	protected static void verifyLayoutFriendlyURL()  {
		try {
			String reservedURLS = getReservedFriendlyURLS();

			Connection connection = DataAccess.getConnection();

			PreparedStatement preparedStatement2 =
					connection.prepareStatement(
						StringBundler.concat(
							"Select friendlyURL, plid from Layout where ",
							"friendlyURL in (", reservedURLS, ")"));

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


