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
const BAKER_HOST = 'www.baker.com';

test.describe.serial('ExportAndAddDBPartitionWithUpgradedDB — Phase 1', () => {
	test(
		'Verify www.able.com and www.baker.com partitions are accessible',
		{tag: '@LPD-91814'},
		async ({apiHelpers, browser}) => {
			const [ableCompany, bakerCompany] = await Promise.all([
				apiHelpers.jsonWebServicesCompany.getCompanyByWebId(ABLE_HOST),
				apiHelpers.jsonWebServicesCompany.getCompanyByWebId(BAKER_HOST),
			]);

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

			fs.mkdirSync(path.dirname(STATE_FILE), {recursive: true});

			fs.writeFileSync(
				STATE_FILE,
				JSON.stringify(
					{
						partitionCompanyIds: [
							ableCompany.companyId,
							bakerCompany.companyId,
						],
					},
					null,
					2
				)
			);
		}
	);
});
