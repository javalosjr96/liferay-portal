/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, mergeTests, test} from '@playwright/test';

import {loginTest} from '../../../fixtures/loginTest';
import {
	ManifestData,
	ManifestEntity,
	getManifestEntities,
	readManifests,
} from '../../../scripts/upgrade-data-archive/runner/manifest';

const loggedInTest = mergeTests(loginTest());

function getApprovedHeading(recordedPage: ManifestEntity) {
	const pageSpecifications = recordedPage.pageSpecifications as {
		heading: string;
		status: string;
	}[];

	const approvedPageSpecification = pageSpecifications.find(
		({status}) => status === 'Approved'
	);

	return approvedPageSpecification.heading;
}

function getManifestData(): ManifestData {
	const liferayHome = process.env.LIFERAY_HOME;

	if (!liferayHome) {
		throw new Error('Set the environment variable LIFERAY_HOME');
	}

	const manifestData = readManifests(liferayHome).find(
		({type}) => type === 'content-pages'
	);

	if (!manifestData) {
		throw new Error('Unable to find the content pages archive manifest');
	}

	return manifestData;
}

function getPageURLs(kind: string, prefix: string) {
	const manifestData = getManifestData();

	const [site] = getManifestEntities(manifestData, 'site');

	return getManifestEntities(manifestData, kind).map((recordedPage) => ({
		recordedPage,
		url: `${prefix}${site.friendlyURLPath}${recordedPage.friendlyURLPath}`,
	}));
}

test(
	'Content page renders for guests',
	{tag: '@LPD-113899'},
	async ({page}) => {
		for (const {recordedPage, url} of getPageURLs('contentPage', '/web')) {
			await page.goto(url);

			await expect(
				page.getByRole('heading', {
					exact: true,
					name: getApprovedHeading(recordedPage),
				})
			).toBeVisible();
		}
	}
);

test('Widget page renders for guests', {tag: '@LPD-113899'}, async ({page}) => {
	for (const {recordedPage, url} of getPageURLs('widgetPage', '/web')) {
		await page.goto(url);

		await expect(
			page.getByRole('navigation', {exact: true, name: 'Breadcrumb'})
		).toHaveText(recordedPage.breadcrumb as string);
	}
});

loggedInTest(
	'Private page renders for signed-in users',
	{tag: '@LPD-113899'},
	async ({page}) => {
		for (const {recordedPage, url} of getPageURLs('privatePage', '/group')) {
			await page.goto(url);

			await expect(
				page.getByRole('heading', {
					exact: true,
					name: getApprovedHeading(recordedPage),
				})
			).toBeVisible();
		}
	}
);
