/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {test as baseTest, Page, expect, mergeTests} from '@playwright/test';

import {loginTest} from '../../../fixtures/loginTest';

const test = mergeTests(loginTest());

const ARCHIVE_ANNOTATIONS = [
	{type: 'data.archive.type', description: 'data-archive-portal'},
	{type: 'database.types', description: 'postgresql'},
	{type: 'portal.version', description: '6.2.5'},
];

const ARCHIVE_USER_EMAIL = 'user@liferay.com';
const ARCHIVE_USER_PASSWORD = 'test';

async function viewUpgradedPortalContent(page: Page) {
	await test.step('View web content after upgrade', async () => {
		await page.goto('/web/guest/web-content');

		await expect(
			page.getByText('Web Content Title', {exact: true})
		).toBeVisible();

		await expect(
			page.getByText('Web Content Content', {exact: true})
		).toBeVisible();
	});

	await test.step('View document after upgrade', async () => {
		await page.goto('/web/guest/document');

		await expect(page.getByText('Document1', {exact: true})).toBeVisible();
	});

	await test.step('View message boards after upgrade', async () => {
		await page.goto('/web/guest/message-boards');

		await expect(
			page.getByText('Message Boards Subject', {exact: true})
		).toBeVisible();
	});

	await test.step('View wiki after upgrade', async () => {
		await page.goto('/web/guest/wiki');

		await expect(
			page.getByText('Wiki Front Page Content', {exact: true})
		).toBeVisible();
	});

	await test.step('View blogs after upgrade', async () => {
		await page.goto('/web/guest/blogs');

		await expect(
			page.getByText('Blogs Entry Title', {exact: true})
		).toBeVisible();

		await expect(
			page.getByText('Blogs Entry Content', {exact: true})
		).toBeVisible();
	});

	await test.step('View site page after upgrade', async () => {
		await page.goto('/web/site-name/site-page');

		await expect(page).toHaveTitle(/Site Page/);
	});
}

test.describe.serial('View portal smoke upgrade', () => {
	test(
		'Can view upgraded portal content as admin',
		{annotation: ARCHIVE_ANNOTATIONS, tag: ['@6.2.5', '@data-archive-portal', '@postgresql', '@LPD-96642']},
		async ({page}) => {
			await viewUpgradedPortalContent(page);
		}
	);

	baseTest(
		'Can view upgraded portal content as regular user',
		{annotation: ARCHIVE_ANNOTATIONS, tag: ['@6.2.5', '@data-archive-portal', '@postgresql', '@LPD-96642']},
		async ({page}) => {
			await page.goto('/c/portal/login');

			await page.getByLabel('Email Address').fill(ARCHIVE_USER_EMAIL);

			await page.getByLabel('Password').fill(ARCHIVE_USER_PASSWORD);

			await page.getByRole('button', {name: 'Sign In'}).click();

			await expect(page).toHaveURL(/\/web\/guest/);

			await viewUpgradedPortalContent(page);
		}
	);
});
