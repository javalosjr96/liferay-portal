/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {devices} from '@playwright/test';

export const config = {
	name: 'openid-link.main',
	testDir: 'tests/openid-link/main',
	use: {
		...devices['Desktop Chrome'],
	},
};

export const openIdConfig = {
	loginPortletLink: 'http://localhost:8080/c/portal/login',
	openIdLink: 'OpenId Connect',
	openIdProvider:
		'https://accounts.google.com/.well-known/openid-configuration',
};
