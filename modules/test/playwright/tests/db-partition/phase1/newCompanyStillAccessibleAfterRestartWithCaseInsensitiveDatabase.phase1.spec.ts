/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, mergeTests} from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

import {apiHelpersTest} from '../../../fixtures/apiHelpersTest';
import {loginTest} from '../../../fixtures/loginTest';
import {liferayConfig} from '../../../liferay.config';
import {performLoginViaApi} from '../../../utils/performLogin';
import {STATE_FILE} from '../utils';

const test = mergeTests(apiHelpersTest, loginTest());

const ABLE_HOST = 'www.able.com';

test.describe
	.serial('NewCompanyStillAccessibleAfterRestartWithCaseInsensitiveDatabase — Phase 1', () => {
	test(
		'Create www.able.com and verify accessible before restart',
		{tag: '@LPD-91814'},
		async ({apiHelpers, browser}) => {
			await apiHelpers.headlessPortalInstance.addVirtualInstance({
				domain: ABLE_HOST,
				portalInstanceId: ABLE_HOST,
				virtualHost: ABLE_HOST,
			});

			const ableCompany =
				await apiHelpers.jsonWebServicesCompany.getCompanyByWebId(
					ABLE_HOST
				);

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

			fs.mkdirSync(path.dirname(STATE_FILE), {recursive: true});

			fs.writeFileSync(
				STATE_FILE,
				JSON.stringify(
					{partitionCompanyId: ableCompany.companyId},
					null,
					2
				)
			);
		}
	);
});
