/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {mergeTests} from '@playwright/test';

import {apiHelpersTest} from '../../../fixtures/apiHelpersTest';
import {loginTest} from '../../../fixtures/loginTest';

const test = mergeTests(apiHelpersTest, loginTest());

const ABLE_HOST = 'www.able.com';
const BAKER_HOST = 'www.baker.com';

test.describe.serial('ExecuteSchemaValidator', () => {
	test(
		'Create virtual instances for schema validation',
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
		}
	);
});
