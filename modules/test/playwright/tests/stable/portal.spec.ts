/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, test} from '@playwright/test';

import {execSync} from 'child_process';

const currentDir = process.cwd();

test('title is Home - Liferay DXP', async ({page}) => {

	const scriptDir = currentDir + "/tests/stable/env/test.sh"

	console.log('Current working directory:', scriptDir);

	const output = execSync(scriptDir).toString();

	console.log('Function output:', output);

});

test('has homepage image', async ({page}) => {
	await page.goto('/');

	await expect(page.locator('#main-content img')).toBeVisible();
});
