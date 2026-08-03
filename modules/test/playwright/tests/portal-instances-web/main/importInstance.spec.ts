/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, mergeTests} from '@playwright/test';

import {loginTest} from '../../../fixtures/loginTest';
import getRandomString from '../../../utils/getRandomString';
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

test(
	'LPD-92621 - Importing an exported schema creates the instance.',
	{tag: ['@LPD-92619', '@LPD-92621']},
	async ({page, virtualInstancesPage}) => {
		test.setTimeout(900000);

		const name = getRandomString();
		const importName = getRandomString();

		let created = false;
		let imported = false;

		try {
			await virtualInstancesPage.addNewVirtualInstance(name);

			created = true;

			await virtualInstancesPage.exportVirtualInstance(name);

			await expect(
				virtualInstancesPage.exportInstanceSuccessMessage.or(
					virtualInstancesPage.exportInstanceErrorMessage
				)
			).toBeVisible({timeout: 60000});

			if (
				await virtualInstancesPage.exportInstanceErrorMessage.isVisible()
			) {
				const errorText =
					await virtualInstancesPage.exportInstanceErrorMessage.textContent();

				test.skip(
					errorText?.includes('is not supported for') ?? false,
					'Database does not support partitioning'
				);
			}

			// The exported schema name is reported only in the success message

			const successText =
				await virtualInstancesPage.exportInstanceSuccessMessage.textContent();

			const schemaName = successText?.match(/schema (\S+)\./)?.[1] ?? '';

			expect(schemaName).not.toBe('');

			await virtualInstancesPage.openImportVirtualInstanceModal();

			await virtualInstancesPage.submitImportVirtualInstance({
				name: importName,
				schemaName,
				timeout: 600000,
				virtualHost: importName,
				webId: importName,
			});

			imported = true;

			// Success closes the modal and redirects, reloading the list

			await expect(
				page.getByRole('row').filter({hasText: importName})
			).toHaveCount(1, {timeout: 120000});
		}
		finally {
			if (imported) {
				await virtualInstancesPage.deleteVirtualInstance(importName);
			}

			if (created) {
				await virtualInstancesPage.deleteVirtualInstance(name);
			}
		}
	}
);
