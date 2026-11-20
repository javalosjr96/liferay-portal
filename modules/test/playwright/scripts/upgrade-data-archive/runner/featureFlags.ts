/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ApiHelpers} from '../../../helpers/ApiHelpers';

export async function enableFeatureFlags(
	apiHelpers: ApiHelpers,
	keys: string[]
) {
	for (const key of keys) {
		const response = await apiHelpers.postResponse(
			`${apiHelpers.baseUrl}com-liferay-feature-flag-web/set-enabled`,
			{headers: {}, multipart: {enabled: 'true', key}}
		);

		if (!response.ok()) {
			throw new Error(
				`Unable to enable feature flag "${key}": ${response.status()}`
			);
		}

		const json = await apiHelpers.post(
			`${apiHelpers.baseUrl}com-liferay-feature-flag-web/is-enabled`,
			{headers: {}, multipart: {key}}
		);

		if (json.featureFlag?.enabled !== true) {
			throw new Error(
				`Unable to enable feature flag "${key}" because the portal did not apply it`
			);
		}
	}
}
