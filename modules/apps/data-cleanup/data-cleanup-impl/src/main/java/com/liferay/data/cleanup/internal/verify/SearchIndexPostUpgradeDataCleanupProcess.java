/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.data.cleanup.internal.verify;

import com.liferay.portal.kernel.instance.PortalInstancePool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.search.index.IndexInformation;
import com.liferay.portal.search.index.IndexNameBuilder;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Jorge Avalos
 */
public class SearchIndexPostUpgradeDataCleanupProcess
	implements PostUpgradeDataCleanupProcess {

	public SearchIndexPostUpgradeDataCleanupProcess(
		IndexInformation indexInformation, IndexNameBuilder indexNameBuilder) {

		_indexInformation = indexInformation;
		_indexNameBuilder = indexNameBuilder;
	}

	@Override
	public void cleanUp() throws Exception {
		if (_indexNameBuilder == null) {
			return;
		}

		if (_indexInformation != null) {
			long[] companyIds = PortalInstancePool.getCompanyIds();

			Arrays.sort(companyIds);

			String[] indexNames = _indexInformation.getIndexNames();

			Pattern pattern = Pattern.compile(
				"^" + Pattern.quote(_indexNameBuilder.getIndexNamePrefix()) +
					"(\\d+)");

			for (String indexName : indexNames) {
				if (indexName == null) {
					continue;
				}

				Matcher matcher = pattern.matcher(indexName);

				if (matcher.find()) {
					long searchIndexCompanyId = GetterUtil.getLong(
						matcher.group(1));

					if (searchIndexCompanyId == 0) {
						_log.error(
							"Unable to parse company ID from search index: " +
								indexName);

						continue;
					}

					if (Arrays.binarySearch(companyIds, searchIndexCompanyId) <
							0) {

						if (_log.isWarnEnabled()) {
							_log.warn(
									"Found orphan index from deleted company: " +
									indexName);
						}
					}
				}
			}
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		SearchIndexPostUpgradeDataCleanupProcess.class);

	private final IndexInformation _indexInformation;
	private final IndexNameBuilder _indexNameBuilder;

}