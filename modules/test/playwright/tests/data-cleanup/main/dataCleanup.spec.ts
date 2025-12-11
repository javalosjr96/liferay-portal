/**
* SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
* SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
*/

import {expect, mergeTests} from '@playwright/test';

import {applicationsMenuPageTest} from '../../../fixtures/applicationsMenuPageTest';
import {serverAdministrationPageTest} from '../../../fixtures/serverAdministrationPageTest';
import {loginTest} from '../../../fixtures/loginTest';

export const test = mergeTests(
	loginTest(),
	applicationsMenuPageTest,
	serverAdministrationPageTest
	);

test('execute all system cleanup actions', async ({ page, applicationsMenuPage }) => {
	await applicationsMenuPage.goToServerAdministration();

	const cleanupPanel = page.locator('.card, .panel',
	{ has: page.getByText('System Cleanup Actions') }).last();
	const panelHeader = cleanupPanel.getByRole('button',
	{ name: /System Cleanup Actions/i });

	if (await panelHeader.getAttribute('aria-expanded') === 'false') {
	await panelHeader.click();
	await expect(panelHeader).toHaveAttribute('aria-expanded', 'true');
	}

	const executeButtons = cleanupPanel.getByRole('button', { name: 'Execute' });
	const count = await executeButtons.count();

	for (let i = 0; i < count; i++) {

		const button = executeButtons.nth(i);

		await button.click();

		const successMessage =
		page.getByText('Success:Your request completed successfully.').first();

		await expect(successMessage).toBeVisible();
	}
});

