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
const BAKER_HOST = 'www.baker.com';

test.describe.serial('ExportAndAddDBPartitionWithUpgradedDB — Phase 2', () => {
	test(
		'Default portal instance is accessible after DB rebuild',
		{tag: '@LPD-91814'},
		async ({page}) => {
			await page.goto('/');

			await expect(page.getByText('Welcome to Liferay')).toBeVisible();
		}
	);

	test(
		'www.able.com and www.baker.com are accessible after partition import',
		{tag: '@LPD-91814'},
		async ({browser}) => {
			const port = liferayConfig.environment.port;

			for (const host of [ABLE_HOST, BAKER_HOST]) {
				const baseURL = `http://${host}:${port}`;

				const instancePage = await browser.newPage({baseURL});

				try {
					await performLoginViaApi({
						domain: `@${host}`,
						loginUrl: baseURL,
						page: instancePage,
						screenName: 'test',
					});

					await expect(instancePage).toHaveURL(new RegExp(host));
				}
				finally {
					await instancePage.close();
				}
			}
		}
	);
});
