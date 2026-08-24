/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.data.cleanup.internal.verify;

import com.liferay.portal.search.index.IndexInformation;
import com.liferay.portal.search.index.IndexNameBuilder;
import com.liferay.portal.verify.VerifyProcess;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Runs search index post-upgrade cleanup when the search engine is available.
 * This component intentionally omits <code>run.on.portal.upgrade=true</code>
 * so that a missing search engine does not block the upgrade. As a consequence,
 * if the search engine connects after the bundle's Release record is already
 * marked verified, this cleanup may not run automatically and would need to be
 * triggered manually via the <code>verify:execute</code> Gogo shell command.
 *
 * @author Jorge Avalos
 */
@Component(service = VerifyProcess.class)
public class SearchIndexPostUpgradeDataCleanupVerifyProcess
	extends VerifyProcess {

	@Override
	protected void doVerify() throws Exception {
		SearchIndexPostUpgradeDataCleanupProcess cleanupProcess =
			new SearchIndexPostUpgradeDataCleanupProcess(
				_indexInformation, _indexNameBuilder);

		cleanupProcess.cleanUp();
	}

	@Reference
	private IndexInformation _indexInformation;

	@Reference
	private IndexNameBuilder _indexNameBuilder;

}
