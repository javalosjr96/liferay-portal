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

test.describe
	.serial('CompareLatest7413PartitionedUpgradedAndFreshDBSchemas — Phase 1', () => {
	test(
		'Create www.able.com partition on released bundle',
		{tag: '@LPD-91814'},
		async ({apiHelpers}) => {
			await apiHelpers.headlessPortalInstance.addVirtualInstance({
				domain: ABLE_HOST,
				portalInstanceId: ABLE_HOST,
				virtualHost: ABLE_HOST,
			});

			const ableCompany =
				await apiHelpers.jsonWebServicesCompany.getCompanyByWebId(
					ABLE_HOST
				);

			fs.mkdirSync(path.dirname(STATE_FILE), {recursive: true});

			fs.writeFileSync(
				STATE_FILE,
				JSON.stringify({ableCompanyId: ableCompany.companyId}, null, 2)
			);
		}
	);
});
