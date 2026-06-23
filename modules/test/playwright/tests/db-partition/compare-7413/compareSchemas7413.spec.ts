/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {mergeTests} from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

import {apiHelpersTest} from '../../../fixtures/apiHelpersTest';
import {loginTest} from '../../../fixtures/loginTest';
import {STATE_FILE} from '../utils';

const test = mergeTests(apiHelpersTest, loginTest());

const ABLE_HOST = 'www.able.com';
const BAKER_HOST = 'www.baker.com';

test.describe.serial('ComparePartitionedUpgradedAndFreshDBSchemas7413', () => {
	test(
		'Create virtual instances for schema comparison',
		{tag: '@LPD-91814'},
		async ({apiHelpers}) => {
			await apiHelpers.headlessPortalInstance.addVirtualInstance({
				domain: ABLE_HOST,
				portalInstanceId: ABLE_HOST,
				virtualHost: ABLE_HOST,
			});

			await apiHelpers.headlessPortalInstance.addVirtualInstance({
				domain: BAKER_HOST,
				portalInstanceId: BAKER_HOST,
				virtualHost: BAKER_HOST,
			});

			const ableCompany =
				await apiHelpers.jsonWebServicesCompany.getCompanyByWebId(
					ABLE_HOST
				);

			const bakerCompany =
				await apiHelpers.jsonWebServicesCompany.getCompanyByWebId(
					BAKER_HOST
				);

			fs.mkdirSync(path.dirname(STATE_FILE), {recursive: true});

			fs.writeFileSync(
				STATE_FILE,
				JSON.stringify(
					{
						ableCompanyId: ableCompany.companyId,
						bakerCompanyId: bakerCompany.companyId,
					},
					null,
					2
				)
			);
		}
	);
});
