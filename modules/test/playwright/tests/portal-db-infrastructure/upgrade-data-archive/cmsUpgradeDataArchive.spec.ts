/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Page, expect, mergeTests} from '@playwright/test';
import {statSync} from 'fs';

import {apiHelpersTest} from '../../../fixtures/apiHelpersTest';
import {loginTest} from '../../../fixtures/loginTest';
import {ApiHelpers} from '../../../helpers/ApiHelpers';
import {getSpaceEntryURL} from '../../../scripts/upgrade-data-archive/archives/cms';
import {
	ManifestEntity,
	getLiferayHome,
	getManifestEntities,
	readManifests,
} from '../../../scripts/upgrade-data-archive/runner/manifest';

const test = mergeTests(apiHelpersTest, loginTest());

function getEntities(kind: string) {
	const manifests = readManifests(getLiferayHome());

	const manifestData = manifests.find(({type}) => type === 'cms');

	test.skip(!manifestData, 'The restored archive has no CMS manifest');

	const entities = getManifestEntities(manifestData!, kind);

	expect(entities.length).toBeGreaterThan(0);

	return entities;
}

async function openInCMSViewer({
	apiHelpers,
	entity,
	page,
}: {
	apiHelpers: ApiHelpers;
	entity: ManifestEntity;
	page: Page;
}) {
	const objectEntry = await apiHelpers.get(
		getSpaceEntryURL(
			apiHelpers,
			entity.path as string,
			entity.externalReferenceCode
		),
		true
	);

	await page.goto(`/web/cms/view-asset?objectEntryId=${objectEntry.id}`);
}

test.describe('View CMS upgrade data archive', () => {
	test(
		'Every archived basic content opens in the CMS viewer',
		{tag: '@LPD-107280'},
		async ({apiHelpers, page}) => {
			for (const entity of getEntities('basicWebContent')) {
				await test.step(entity.externalReferenceCode, async () => {

					// Open the entry in the CMS viewer

					await openInCMSViewer({apiHelpers, entity, page});

					// Check the title

					await expect(
						page
							.frameLocator('iframe')
							.getByLabel('Title (Read Only)')
					).toHaveValue(entity.title as string);
				});
			}
		}
	);

	test(
		'Every archived basic document downloads from the CMS viewer',
		{tag: '@LPD-107280'},
		async ({apiHelpers, page}) => {
			for (const entity of getEntities('basicDocument')) {
				await test.step(entity.externalReferenceCode, async () => {

					// Open the entry in the CMS viewer

					await openInCMSViewer({apiHelpers, entity, page});

					// Check the title

					await expect(
						page.getByText(entity.title as string, {exact: true})
					).toBeVisible();

					// Download the file

					const downloadPromise = page.waitForEvent('download');

					await page.getByRole('link', {name: 'Download'}).click();

					const download = await downloadPromise;

					expect(download.suggestedFilename()).toBe(entity.fileName);

					const downloadPath = await download.path();

					expect(statSync(downloadPath).size).toBeGreaterThan(0);
				});
			}
		}
	);
});
