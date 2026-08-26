/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {test as baseTest, expect, mergeTests} from '@playwright/test';

import {loginTest} from '../../../fixtures/loginTest';
import {viewUpgradedPortalContent} from '../utils/viewUpgradedPortalContent';

const test = mergeTests(loginTest());

const ARCHIVE_ANNOTATIONS = [
	{type: 'data.archive.type', description: 'data-archive-portal'},
	{type: 'database.types', description: 'postgresql'},
	{type: 'portal.version', description: '6.2.5'},
];

const ARCHIVE_USER_EMAIL = 'user@liferay.com';
const ARCHIVE_USER_FULL_NAME = 'userfn userln';
const ARCHIVE_USER_PASSWORD = 'test';

test.describe.serial('View portal smoke upgrade from 6.2.5', () => {
	test(
		'Can view upgraded portal content as admin',
		{annotation: ARCHIVE_ANNOTATIONS, tag: '@LPD-96642'},
		async ({page}) => {
			await viewUpgradedPortalContent(page);
		}
	);

	baseTest(
		'Can view upgraded portal content as regular user',
		{annotation: ARCHIVE_ANNOTATIONS, tag: '@LPD-96642'},
		async ({page}) => {
			await page.goto('/c/portal/login');

			await page.getByLabel('Email Address').fill(ARCHIVE_USER_EMAIL);

			await page.getByLabel('Password').fill(ARCHIVE_USER_PASSWORD);

			await page.getByRole('button', {name: 'Sign In'}).click();

			await expect(page.getByLabel(ARCHIVE_USER_FULL_NAME)).toBeVisible({
				timeout: 30 * 1000,
			});

			await viewUpgradedPortalContent(page);
		}
	);
});
