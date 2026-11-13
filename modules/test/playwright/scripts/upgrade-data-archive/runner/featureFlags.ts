/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {request} from '@playwright/test';

async function isEnabled(
	apiRequestContext: Awaited<ReturnType<typeof request.newContext>>,
	key: string
) {
	const response = await apiRequestContext.post(
		'/o/com-liferay-feature-flag-web/is-enabled',
		{multipart: {key}}
	);

	if (!response.ok()) {
		throw new Error(
			`Unable to read feature flag "${key}": ${response.status()}`
		);
	}

	const json = await response.json();

	return json.featureFlag?.enabled === true;
}

export async function enableFeatureFlags(
	authorization: string,
	baseURL: string,
	keys: string[]
) {
	if (!keys.length) {
		return;
	}

	const apiRequestContext = await request.newContext({
		baseURL,
		extraHTTPHeaders: {Authorization: authorization},
	});

	try {
		for (const key of keys) {
			const response = await apiRequestContext.post(
				'/o/com-liferay-feature-flag-web/set-enabled',
				{multipart: {companyId: '0', enabled: 'true', key}}
			);

			if (!response.ok()) {
				throw new Error(
					`Unable to enable feature flag "${key}": ${response.status()}`
				);
			}

			if (!(await isEnabled(apiRequestContext, key))) {
				throw new Error(
					`Unable to enable feature flag "${key}" because the portal did not apply it`
				);
			}
		}
	}
	finally {
		await apiRequestContext.dispose();
	}
}
