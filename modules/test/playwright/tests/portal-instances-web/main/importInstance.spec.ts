/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, mergeTests} from '@playwright/test';

import {loginTest} from '../../../fixtures/loginTest';
import {portalInstancesPagesTest} from './fixtures/portalInstancesPagesTest';

const test = mergeTests(loginTest(), portalInstancesPagesTest);

test(
	'LPD-92621 - Importing an invalid schema name shows an error.',
	{tag: '@LPD-92621'},
	async ({virtualInstancesPage}) => {
		await virtualInstancesPage.openImportVirtualInstanceModal();

		await virtualInstancesPage.submitImportVirtualInstance({
			schemaName: 'invalid-schema-name',
		});

		await expect(
			virtualInstancesPage.importInstanceErrorMessage
		).toBeVisible();
	}
);
