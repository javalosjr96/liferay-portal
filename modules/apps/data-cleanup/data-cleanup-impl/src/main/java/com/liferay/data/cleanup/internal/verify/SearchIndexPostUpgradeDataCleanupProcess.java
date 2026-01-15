/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.data.cleanup.internal.verify;

import com.liferay.data.cleanup.internal.verify.util.PostUpgradeDataCleanupProcessUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.module.util.SystemBundleUtil;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.search.index.IndexInformation;
import com.liferay.portal.search.index.IndexNameBuilder;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * @author Jorge Avalos
 */
public class SearchIndexPostUpgradeDataCleanupProcess
	implements PostUpgradeDataCleanupProcess {

	public SearchIndexPostUpgradeDataCleanupProcess(
		CompanyLocalService companyLocalService) {

		_companyLocalService = companyLocalService;
	}

	@Override
	public void cleanUp() throws Exception {
		if (!PostUpgradeDataCleanupProcessUtil.isEveryLiferayBundleResolved()) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					StringBundler.concat(
						ClassNamePostUpgradeDataCleanupProcess.class.
							getSimpleName(),
						" cannot be executed because there are modules with ",
						"unsatisfied references"));
			}

			return;
		}

		IndexNameBuilder indexNameBuilder = _getService(IndexNameBuilder.class);

		if (indexNameBuilder == null) {
			return;
		}

		IndexInformation indexInformation = _getService(IndexInformation.class);

		if (indexInformation != null) {
			Set<Long> companyIdSet = new HashSet<>();

			_companyLocalService.forEachCompanyId(companyIdSet::add);

			String[] indexNames = indexInformation.getIndexNames();

			Pattern pattern = Pattern.compile(
				"^" + Pattern.quote(indexNameBuilder.getIndexNamePrefix()) +
					"(\\d+)");

			for (String indexName : indexNames) {
				if (indexName == null) {
					continue;
				}

				Matcher matcher = pattern.matcher(indexName);

				if (matcher.find()) {
					try {
						long extractedId = Long.parseLong(matcher.group(1));

						if (!companyIdSet.contains(extractedId)) {
							if (_log.isWarnEnabled()) {
								_log.warn(
									"Found stale index from deleted company: " +
										indexName);
							}
						}
					}
					catch (Exception exception) {
						_log.error(
							"Unable to parse company ID from search index: " +
								indexName,
							exception);
					}
				}
			}
		}
	}

	private <T> T _getService(Class<T> serviceClass) {
		BundleContext bundleContext = SystemBundleUtil.getBundleContext();

		if (bundleContext == null) {
			return null;
		}

		ServiceReference<T> serviceReference =
			bundleContext.getServiceReference(serviceClass);

		if (serviceReference != null) {
			return bundleContext.getService(serviceReference);
		}

		return null;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		SearchIndexPostUpgradeDataCleanupProcess.class);

	private final CompanyLocalService _companyLocalService;

}