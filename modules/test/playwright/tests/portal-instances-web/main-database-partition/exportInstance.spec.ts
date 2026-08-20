/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, mergeTests} from '@playwright/test';

import {apiHelpersTest} from '../../../fixtures/apiHelpersTest';
import {loginTest} from '../../../fixtures/loginTest';
import {virtualInstancesPagesTest} from '../../../fixtures/virtualInstancesPagesTest';
import getRandomString from '../../../utils/getRandomString';

const test = mergeTests(apiHelpersTest, loginTest(), virtualInstancesPagesTest);

test(
	'LPD-92621 Exporting an instance reports the schema it was exported to',
	{tag: '@LPD-92621'},
	async ({apiHelpers, virtualInstancesPage}) => {
		test.setTimeout(5 * 180 * 1000);

		const webId = getRandomString();

		let created = false;

		try {
			await virtualInstancesPage.addNewVirtualInstance(webId, {
				timeout: 180 * 1000,
			});

			created = true;

			const company =
				await apiHelpers.jsonWebServicesCompany.getCompanyByWebId(
					webId
				);

			const schemaName = await virtualInstancesPage.exportVirtualInstance(
				webId,
				{timeout: 180 * 1000}
			);

			expect(schemaName).toBe(`lexported_${company.companyId}`);
		}
		finally {
			if (created) {
				await virtualInstancesPage.deleteVirtualInstance(webId, {
					timeout: 180 * 1000,
				});
			}
		}
	}
);
