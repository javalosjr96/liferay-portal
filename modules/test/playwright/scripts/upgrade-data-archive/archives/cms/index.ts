/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, request} from '@playwright/test';

import {ApiHelpers} from '../../../../helpers/ApiHelpers';
import {liferayConfig} from '../../../../liferay.config';
import {
	Manifest,
	ManifestData,
	getManifestEntities,
} from '../../runner/manifest';
import {UpgradeDataArchive} from '../../runner/upgradeDataArchive';

const CONTENT_PAGE_EXTERNAL_REFERENCE_CODE = 'UPGRADE_DATA_ARCHIVE_CMS_PAGE';

const SITE_EXTERNAL_REFERENCE_CODE = 'L_GUEST';

function getPageSpecification(
	externalReferenceCode: string,
	status: string,
	draftExternalReferenceCode?: string
) {
	return {
		...(draftExternalReferenceCode
			? {
					draftContentPageSpecificationExternalReferenceCode:
						draftExternalReferenceCode,
				}
			: {}),
		externalReferenceCode,
		pageExperiences: [
			{
				externalReferenceCode: `${externalReferenceCode}_EXPERIENCE`,
				key: 'DEFAULT',
				name_i18n: {'en-US': 'Default'},
				pageElements: [
					{
						externalReferenceCode: `${externalReferenceCode}_CONTAINER`,
						pageElementDefinition: {type: 'Container'},
						position: 0,
					},
				],
			},
		],
		settings: {},
		status,
		type: 'ContentPageSpecification',
	};
}

async function populateContentPage(
	apiHelpers: ApiHelpers,
	manifest: Manifest
) {
	const draftExternalReferenceCode = `${CONTENT_PAGE_EXTERNAL_REFERENCE_CODE}_DRAFT`;
	const friendlyURLPath = '/upgrade-data-archive-cms';

	const pageSpecifications = [
		getPageSpecification(draftExternalReferenceCode, 'Draft'),
		getPageSpecification(
			CONTENT_PAGE_EXTERNAL_REFERENCE_CODE,
			'Approved',
			draftExternalReferenceCode
		),
	];

	await apiHelpers.headlessAdminSite.createPage(
		SITE_EXTERNAL_REFERENCE_CODE,
		{
			externalReferenceCode: CONTENT_PAGE_EXTERNAL_REFERENCE_CODE,
			friendlyUrlPath_i18n: {'en-US': friendlyURLPath},
			name_i18n: {'en-US': 'Upgrade Data Archive CMS'},
			pageSettings: {
				hiddenFromNavigation: false,
				type: 'ContentPageSettings',
			},
			pageSpecifications,
			type: 'ContentPage',
		}
	);

	manifest.add('contentPage', {
		externalReferenceCode: CONTENT_PAGE_EXTERNAL_REFERENCE_CODE,
		friendlyURLPath,
		pageSpecifications: pageSpecifications.map((pageSpecification) => ({
			externalReferenceCode: pageSpecification.externalReferenceCode,
			status: pageSpecification.status,
		})),
		siteExternalReferenceCode: SITE_EXTERNAL_REFERENCE_CODE,
	});
}

async function populateSpace(apiHelpers: ApiHelpers, manifest: Manifest) {
	const externalReferenceCode = 'UPGRADE_DATA_ARCHIVE_CMS_SPACE';
	const name = 'Upgrade Data Archive CMS';

	const space = await apiHelpers.headlessAssetLibrary.createSpace({
		externalReferenceCode,
		name,
	});

	if (space?.type !== 'Space') {
		throw new Error(
			`Unable to create space "${externalReferenceCode}": ${JSON.stringify(space)}`
		);
	}

	manifest.add('space', {externalReferenceCode, name});
}

async function verifyContentPages(
	apiHelpers: ApiHelpers,
	manifestData: ManifestData
) {
	const guestRequestContext = await request.newContext({
		baseURL: liferayConfig.environment.baseUrl,
	});

	try {
		for (const contentPage of getManifestEntities(
			manifestData,
			'contentPage'
		)) {
			const siteExternalReferenceCode =
				contentPage.siteExternalReferenceCode as string;

			const page = await apiHelpers.headlessAdminSite.getPage(
				siteExternalReferenceCode,
				contentPage.externalReferenceCode
			);

			expect(page.externalReferenceCode).toBe(
				contentPage.externalReferenceCode
			);
			expect(page.type).toBe('ContentPage');

			const pageSpecifications = (contentPage.pageSpecifications ||
				[]) as {externalReferenceCode: string; status: string}[];

			for (const pageSpecification of pageSpecifications) {
				const upgradedPageSpecification =
					await apiHelpers.headlessAdminSite.getPageSpecification(
						siteExternalReferenceCode,
						pageSpecification.externalReferenceCode
					);

				expect(upgradedPageSpecification.status).toBe(
					pageSpecification.status
				);
			}

			const response = await guestRequestContext.get(
				`/web/guest${contentPage.friendlyURLPath}`
			);

			expect(response.status()).toBe(200);
		}
	}
	finally {
		await guestRequestContext.dispose();
	}
}

async function verifySpaces(
	apiHelpers: ApiHelpers,
	manifestData: ManifestData
) {
	for (const space of getManifestEntities(manifestData, 'space')) {
		const assetLibrary =
			await apiHelpers.headlessAssetLibrary.getAssetLibrary(
				space.externalReferenceCode
			);

		expect(assetLibrary.name).toBe(space.name);
		expect(assetLibrary.type).toBe('Space');
	}
}

const upgradeDataArchive: UpgradeDataArchive = {
	featureFlags: [],
	name: 'cms',
	populate: async (apiHelpers, manifest) => {
		await populateSpace(apiHelpers, manifest);

		await populateContentPage(apiHelpers, manifest);
	},
	verify: async (apiHelpers, manifestData) => {
		await verifySpaces(apiHelpers, manifestData);

		await verifyContentPages(apiHelpers, manifestData);
	},
};

export default upgradeDataArchive;
