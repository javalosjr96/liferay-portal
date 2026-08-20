/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.configuration.plugin.internal;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.db.partition.util.DBPartitionUtil;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.Dictionary;

import javax.sql.DataSource;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationPlugin;

/**
 * @author Mario Leandro
 */
public class GroupKeyToGroupConfigurationPluginImpl
	implements ConfigurationPlugin {

	public GroupKeyToGroupConfigurationPluginImpl(BundleContext bundleContext) {
		_bundleContext = bundleContext;
	}

	@Override
	public void modifyConfiguration(
		ServiceReference<?> serviceReference,
		Dictionary<String, Object> properties) {

		String portableIdentifier = (String)properties.get("groupKey");

		if (Validator.isNull(portableIdentifier) ||
			(properties.get("groupId") != null)) {

			return;
		}

		int index = portableIdentifier.indexOf(_SEPARATOR);

		if (index <= 0) {
			if (_log.isWarnEnabled()) {
				_log.warn("Skip malformed group key " + portableIdentifier);
			}

			return;
		}

		String webId = portableIdentifier.substring(0, index);

		String groupKey = portableIdentifier.substring(
			index + _SEPARATOR.length());

		try {
			ServiceReference<DataSource> dataSourceServiceReference =
				_bundleContext.getServiceReference(DataSource.class);

			if (dataSourceServiceReference == null) {
				if (_log.isWarnEnabled()) {
					_log.warn("Data source service is null");
				}

				return;
			}

			DataSource dataSource = _bundleContext.getService(
				dataSourceServiceReference);

			Long companyId = _getCompanyId(dataSource, webId);

			Long[] groupIds = new Long[1];

			if (companyId != null) {
				DBPartitionUtil.forEachCompanyId(
					partitionCompanyId -> {
						if ((partitionCompanyId != null) &&
							!companyId.equals(partitionCompanyId)) {

							return;
						}

						groupIds[0] = _getGroupId(
							companyId, dataSource, groupKey);
					});
			}

			if (groupIds[0] == null) {
				if (_log.isWarnEnabled()) {
					_log.warn("Skip group key " + portableIdentifier);
				}

				return;
			}

			properties.put("groupId", groupIds[0]);

			if (properties.get("companyId") == null) {
				properties.put("companyId", companyId);
			}

			if (_log.isInfoEnabled()) {
				_log.info(
					StringBundler.concat(
						"Injected company ID ", companyId, " and group ID ",
						groupIds[0], " for group key ", groupKey));
			}
		}
		catch (Exception exception) {
			if (_log.isDebugEnabled()) {
				_log.debug(exception);
			}

			if (_log.isWarnEnabled()) {
				_log.warn("Skip group key " + portableIdentifier);
			}
		}
	}

	private Long _getCompanyId(DataSource dataSource, String webId)
		throws Exception {

		try (Connection connection = dataSource.getConnection();

			PreparedStatement preparedStatement = connection.prepareStatement(
				_db.buildSQL(
					"select companyId from Company where webId = ?"))) {

			preparedStatement.setString(1, webId);

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					return resultSet.getLong("companyId");
				}
			}
		}

		return null;
	}

	private Long _getGroupId(
			long companyId, DataSource dataSource, String groupKey)
		throws Exception {

		try (Connection connection = dataSource.getConnection();

			PreparedStatement preparedStatement = connection.prepareStatement(
				_db.buildSQL(
					"select groupId from Group_ where companyId = ? and " +
						"groupKey = ?"))) {

			preparedStatement.setLong(1, companyId);
			preparedStatement.setString(2, groupKey);

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					return resultSet.getLong("groupId");
				}
			}
		}

		return null;
	}

	private static final String _SEPARATOR = "--";

	private static final Log _log = LogFactoryUtil.getLog(
		GroupKeyToGroupConfigurationPluginImpl.class);

	private final BundleContext _bundleContext;
	private final DB _db = DBManagerUtil.getDB();

}