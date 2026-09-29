/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, test} from '@playwright/test';

import {liferayConfig} from '../../../liferay.config';

test.describe('View large database partitioning upgrade', () => {
	test(
		'Can sign in to the upgraded default partition',
		{tag: '@LPD-104388'},
		async ({page}) => {
			await page.goto(
				`http://www.able.com:${liferayConfig.environment.port}`
			);

			await expect(
				page.locator('.user-personal-bar').getByPlaceholder('Search')
			).toBeVisible();

			await page.getByRole('button', {name: 'Sign In'}).click();

			const emailAddressInput = page.getByLabel('Email Address', {
				exact: true,
			});

			await expect(emailAddressInput).toBeVisible();

			await emailAddressInput.fill('test@www.able.com');

			await page.getByLabel('Password', {exact: true}).fill('1234');

			await page
				.locator('form.sign-in-form')
				.getByRole('button', {name: 'Sign In'})
				.click();

			await expect(
				page.getByLabel('Test Test', {exact: true})
			).toBeVisible({
				timeout: 30 * 1000,
			});
		}
	);
});
