/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Page, expect} from '@playwright/test';

/**
 * Asserts that a document restored by a legacy database upgrade opens on its
 * site page, reports the metadata the archive recorded for it, and answers its
 * download URL with 200 rather than the 404 a store miss produces. Pass
 * fileSize only for an archive whose expected display size is known.
 */
export async function viewUpgradedDocument({
	fileSize,
	page,
	title,
}: {
	fileSize?: string;
	page: Page;
	title: string;
}) {
	await page.goto('/web/site-name/document');

	await page.getByRole('link', {name: title}).click();

	await page.locator('a[href*=infoPanel]').click();

	await expect(page.locator('.sidebar-body .username')).toHaveText(
		'Test Test'
	);

	await expect(page.locator('.sidebar-header .label-item')).toHaveText(
		'Version 1.0'
	);

	await expect(page.locator('.sidebar-header .workflow-status')).toHaveText(
		'Approved'
	);

	const downloadLink = page
		.locator('.sidebar-section')
		.getByRole('link', {name: 'Download'});

	if (fileSize) {
		await expect(downloadLink).toHaveAttribute(
			'title',
			`File Size ${fileSize}`
		);
	}

	const downloadURL = await downloadLink.getAttribute('href');

	expect(downloadURL).not.toBeNull();

	const response = await page.request.get(downloadURL as string);

	expect(response.status()).toBe(200);
}
