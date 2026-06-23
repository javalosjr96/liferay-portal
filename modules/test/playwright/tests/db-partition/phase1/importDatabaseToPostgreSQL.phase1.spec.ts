/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect} from '@playwright/test';

import {loginTest} from '../../../fixtures/loginTest';

const test = loginTest();

test.describe.serial('CanImportDatabaseToPostgreSQL — Phase 1', () => {
	test(
		'Portal is accessible before DB migration',
		{tag: '@LPD-91814'},
		async ({page}) => {
			await page.goto('/');

			await expect(page.getByText('Welcome to Liferay')).toBeVisible();
		}
	);
});
