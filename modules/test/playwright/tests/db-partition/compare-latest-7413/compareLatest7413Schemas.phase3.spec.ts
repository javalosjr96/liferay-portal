/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {mergeTests} from '@playwright/test';
import * as fs from 'fs';

import {apiHelpersTest} from '../../../fixtures/apiHelpersTest';
import {loginTest} from '../../../fixtures/loginTest';
import {STATE_FILE} from '../utils';

const test = mergeTests(apiHelpersTest, loginTest());

const BAKER_HOST = 'www.baker.com';

test.describe
	.serial('CompareLatest7413PartitionedUpgradedAndFreshDBSchemas — Phase 3', () => {
	test(
		'Create www.baker.com partition on fresh server',
		{tag: '@LPD-91814'},
		async ({apiHelpers}) => {
			await apiHelpers.headlessPortalInstance.addVirtualInstance({
				domain: BAKER_HOST,
				portalInstanceId: BAKER_HOST,
				virtualHost: BAKER_HOST,
			});

			const bakerCompany =
				await apiHelpers.jsonWebServicesCompany.getCompanyByWebId(
					BAKER_HOST
				);

			const existingState = fs.existsSync(STATE_FILE)
				? JSON.parse(fs.readFileSync(STATE_FILE, 'utf-8'))
				: {};

			fs.writeFileSync(
				STATE_FILE,
				JSON.stringify(
					{
						...existingState,
						bakerCompanyId: bakerCompany.companyId,
					},
					null,
					2
				)
			);
		}
	);
});
