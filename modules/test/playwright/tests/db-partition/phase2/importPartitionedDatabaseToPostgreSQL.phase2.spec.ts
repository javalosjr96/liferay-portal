/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, mergeTests} from '@playwright/test';

import {apiHelpersTest} from '../../../fixtures/apiHelpersTest';
import {loginTest} from '../../../fixtures/loginTest';
import {liferayConfig} from '../../../liferay.config';
import {performLoginViaApi} from '../../../utils/performLogin';

const test = mergeTests(apiHelpersTest, loginTest());

const ABLE_HOST = 'www.able.com';

test.describe.serial('ImportPartitionedDatabaseToPostgreSQL — Phase 2', () => {
	test(
		'Default portal is accessible on PostgreSQL',
		{tag: '@LPD-91814'},
		async ({page}) => {
			await page.goto('/');

			await expect(page.getByText('Welcome to Liferay')).toBeVisible();
		}
	);

	test(
		'www.able.com partition is accessible after DB migration to PostgreSQL',
		{tag: '@LPD-91814'},
		async ({browser}) => {
			const port = liferayConfig.environment.port;

			const ableBaseUrl = `http://${ABLE_HOST}:${port}`;

			const instancePage = await browser.newPage({
				baseURL: ableBaseUrl,
			});

			try {
				await performLoginViaApi({
					domain: `@${ABLE_HOST}`,
					loginUrl: ableBaseUrl,
					page: instancePage,
					screenName: 'test',
				});

				await expect(instancePage).toHaveURL(new RegExp(ABLE_HOST));
			}
			finally {
				await instancePage.close();
			}
		}
	);
});
