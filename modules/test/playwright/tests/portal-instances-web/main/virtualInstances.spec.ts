/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, mergeTests} from '@playwright/test';

import {apiHelpersTest} from '../../../fixtures/apiHelpersTest';
import {featureFlagsTest} from '../../../fixtures/featureFlagsTest';
import {loginTest} from '../../../fixtures/loginTest';
import getRandomString from '../../../utils/getRandomString';
import {virtualInstanceExportPagesTest} from './fixtures/virtualInstanceExportPagesTest';

export const test = mergeTests(
	apiHelpersTest,
	featureFlagsTest({'LPD-11342': {enabled: true, system: true}}),
	loginTest(),
	virtualInstanceExportPagesTest
);

test(
	'LPD-92619 - Exporting a virtual instance shows the exported schema name.',
	{tag: '@LPD-92619'},
	async ({apiHelpers, virtualInstanceExportPage}) => {
		test.setTimeout(240000);

		const name = getRandomString();

		try {

			// Create the instance

			await virtualInstanceExportPage.addInstance(name);

			const company =
				await apiHelpers.jsonWebServicesCompany.getCompanyByWebId(name);

			// Export it

			await virtualInstanceExportPage.export(name);

			// Skip when the database does not support partitioning

			await expect(
				virtualInstanceExportPage.successToast.or(
					virtualInstanceExportPage.errorAlert
				)
			).toBeVisible({timeout: 60000});

			if (await virtualInstanceExportPage.errorAlert.isVisible()) {
				const errorText =
					await virtualInstanceExportPage.errorAlert.textContent();

				test.skip(
					errorText?.includes('is not supported for') ?? false,
					'Database does not support partitioning'
				);
			}

			// Assert the toast

			await expect(virtualInstanceExportPage.successToast).toContainText(
				`The instance was exported to the schema lexported_${company.companyId}.`
			);
		}
		finally {

			// The exported schema is left in place; it is uniquely named per
			// company and does not need cleanup

			await virtualInstanceExportPage.deleteInstance(name);
		}
	}
);

test(
	'LPD-92619 - Exporting an already-exported virtual instance shows an error.',
	{tag: '@LPD-92619'},
	async ({virtualInstanceExportPage}) => {
		test.setTimeout(240000);

		const name = getRandomString();

		try {

			// Create the instance

			await virtualInstanceExportPage.addInstance(name);

			// Export it

			await virtualInstanceExportPage.export(name);

			// Skip when the database does not support partitioning

			await expect(
				virtualInstanceExportPage.successToast.or(
					virtualInstanceExportPage.errorAlert
				)
			).toBeVisible({timeout: 60000});

			if (await virtualInstanceExportPage.errorAlert.isVisible()) {
				const errorText =
					await virtualInstanceExportPage.errorAlert.textContent();

				test.skip(
					errorText?.includes('is not supported for') ?? false,
					'Database does not support partitioning'
				);
			}

			// Assert the first export succeeded

			await expect(virtualInstanceExportPage.successToast).toBeVisible();

			// Export it again

			await virtualInstanceExportPage.export(name);

			// Assert the error

			await expect(virtualInstanceExportPage.errorAlert).toContainText(
				'Export failed with message:',
				{timeout: 60000}
			);
		}
		finally {

			// The exported schema is left in place; it is uniquely named per
			// company and does not need cleanup

			await virtualInstanceExportPage.deleteInstance(name);
		}
	}
);
