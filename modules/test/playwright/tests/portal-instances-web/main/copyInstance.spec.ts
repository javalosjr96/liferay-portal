/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, mergeTests} from '@playwright/test';

import {apiHelpersTest} from '../../../fixtures/apiHelpersTest';
import {loginTest} from '../../../fixtures/loginTest';
import getRandomString from '../../../utils/getRandomString';
import {portalInstancesPagesTest} from './fixtures/portalInstancesPagesTest';

const test = mergeTests(apiHelpersTest, loginTest(), portalInstancesPagesTest);

test(
	'LPD-92620 - Copying an instance to an existing company ID shows an error.',
	{tag: '@LPD-92620'},
	async ({apiHelpers, virtualInstancesPage}) => {
		test.setTimeout(360000);

		const name = getRandomString();

		let created = false;

		try {
			await virtualInstancesPage.addNewVirtualInstance(name);

			created = true;

			const company =
				await apiHelpers.jsonWebServicesCompany.getCompanyByWebId(name);

			await virtualInstancesPage.openCopyVirtualInstanceModal(name);

			// Reusing the source company ID as the destination collides with an
			// instance that already exists

			await virtualInstancesPage.submitCopyVirtualInstance({
				destinationCompanyId: String(company.companyId),
				name: getRandomString(),
				virtualHost: getRandomString(),
				webId: getRandomString(),
			});

			await expect(
				virtualInstancesPage.copyInstanceErrorMessage
			).toBeVisible();
		}
		finally {

			// A failed copy leaves the modal open, which blocks navigation

			if (
				await virtualInstancesPage.copyInstanceCancelButton.isVisible()
			) {
				await virtualInstancesPage.copyInstanceCancelButton.click();
			}

			if (created) {
				await virtualInstancesPage.deleteVirtualInstance(name);
			}
		}
	}
);

test(
	'LPD-92620 - Copying an instance creates it and closes the modal.',
	{tag: '@LPD-92620'},
	async ({page, virtualInstancesPage}) => {
		test.setTimeout(900000);

		const name = getRandomString();
		const copyName = getRandomString();

		let copied = false;
		let created = false;

		try {
			await virtualInstancesPage.addNewVirtualInstance(name);

			created = true;

			await virtualInstancesPage.openCopyVirtualInstanceModal(name);

			// Omitting the destination company ID auto-assigns one

			await virtualInstancesPage.submitCopyVirtualInstance({
				destinationCompanyId: '',
				name: copyName,
				timeout: 600000,
				virtualHost: copyName,
				webId: copyName,
			});

			// Success closes the modal and redirects, reloading the list

			await expect(
				page.getByRole('row').filter({hasText: copyName})
			).toHaveCount(1, {timeout: 120000});

			copied = true;
		}
		finally {

			// A failed copy leaves the modal open, which blocks navigation

			if (
				await virtualInstancesPage.copyInstanceCancelButton.isVisible()
			) {
				await virtualInstancesPage.copyInstanceCancelButton.click();
			}

			if (copied) {
				await virtualInstancesPage.deleteVirtualInstance(copyName);
			}

			if (created) {
				await virtualInstancesPage.deleteVirtualInstance(name);
			}
		}
	}
);
