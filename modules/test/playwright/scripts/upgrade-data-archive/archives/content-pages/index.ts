/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect} from '@playwright/test';

import {ApiHelpers} from '../../../../helpers/ApiHelpers';
import {
	Manifest,
	ManifestData,
	ManifestEntity,
	getManifestEntities,
} from '../../runner/manifest';
import {UpgradeDataArchive} from '../../runner/upgradeDataArchive';

const SITE_EXTERNAL_REFERENCE_CODE = 'UPGRADE_DATA_ARCHIVE_CONTENT_PAGES';

type PageSpecification = {
	externalReferenceCode: string;
	status: string;
};

async function createPrivatePage(
	apiHelpers: ApiHelpers,
	siteExternalReferenceCode: string,
	page: object
) {
	return apiHelpers.post(
		`${apiHelpers.baseUrl}headless-admin-site/v1.0/sites/${siteExternalReferenceCode}/site-pages?privateLayout=true`,
		{data: page, failOnStatusCode: true}
	);
}

function getContentPage(
	externalReferenceCode: string,
	friendlyURLPath: string,
	heading: string,
	name: string
) {
	const draftExternalReferenceCode = `${externalReferenceCode}_DRAFT`;

	return {
		externalReferenceCode,
		friendlyUrlPath_i18n: {'en-US': friendlyURLPath},
		name_i18n: {'en-US': name},
		pageSettings: {
			hiddenFromNavigation: false,
			type: 'ContentPageSettings',
		},
		pageSpecifications: [
			getContentPageSpecification(
				draftExternalReferenceCode,
				heading,
				'Draft'
			),
			getContentPageSpecification(
				externalReferenceCode,
				heading,
				'Approved',
				draftExternalReferenceCode
			),
		],
		type: 'ContentPage',
	};
}

function getContentPageSpecification(
	externalReferenceCode: string,
	heading: string,
	status: string,
	draftExternalReferenceCode?: string
) {
	const containerExternalReferenceCode = `${externalReferenceCode}_CONTAINER`;

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
						externalReferenceCode: containerExternalReferenceCode,
						pageElementDefinition: {type: 'Container'},
						position: 0,
					},
					{
						externalReferenceCode: `${externalReferenceCode}_HEADING`,
						pageElementDefinition: {
							fragmentInstance: {
								fragmentEditableElements: [
									{
										fragmentEditableElementValue: {
											fragmentLinkTextValue: {
												textFragmentValue: {
													fragmentInlineValue: {
														value_i18n: {
															'en-US': heading,
														},
													},
													type: 'Inline',
												},
											},
											type: 'Text',
										},
										id: 'element-text',
									},
								],
								fragmentReference: {
									defaultFragmentKey:
										'BASIC_COMPONENT-heading',
									fragmentReferenceType:
										'DefaultFragmentReference',
								},
							},
							type: 'BasicFragment',
						},
						parentExternalReferenceCode:
							containerExternalReferenceCode,
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

function getPageSpecifications(page: {
	pageSpecifications: PageSpecification[];
}) {
	return page.pageSpecifications.map(({externalReferenceCode, status}) => ({
		externalReferenceCode,
		status,
	}));
}

async function populateContentPage(
	apiHelpers: ApiHelpers,
	manifest: Manifest
) {
	const externalReferenceCode = `${SITE_EXTERNAL_REFERENCE_CODE}_CONTENT_PAGE`;
	const friendlyURLPath = '/content-page';
	const heading = 'Archived Content Heading';

	const page = getContentPage(
		externalReferenceCode,
		friendlyURLPath,
		heading,
		'Content Page'
	);

	await apiHelpers.headlessAdminSite.createPage(
		SITE_EXTERNAL_REFERENCE_CODE,
		page
	);

	manifest.add('contentPage', {
		externalReferenceCode,
		friendlyURLPath,
		heading,
		pageSpecifications: getPageSpecifications(page),
		siteExternalReferenceCode: SITE_EXTERNAL_REFERENCE_CODE,
		type: page.type,
	});
}

async function populatePrivatePage(
	apiHelpers: ApiHelpers,
	manifest: Manifest
) {
	const externalReferenceCode = `${SITE_EXTERNAL_REFERENCE_CODE}_PRIVATE_PAGE`;
	const friendlyURLPath = '/private-page';
	const heading = 'Archived Private Heading';

	const page = getContentPage(
		externalReferenceCode,
		friendlyURLPath,
		heading,
		'Private Page'
	);

	await createPrivatePage(apiHelpers, SITE_EXTERNAL_REFERENCE_CODE, page);

	manifest.add('privatePage', {
		externalReferenceCode,
		friendlyURLPath,
		heading,
		pageSpecifications: getPageSpecifications(page),
		siteExternalReferenceCode: SITE_EXTERNAL_REFERENCE_CODE,
		type: page.type,
	});
}

async function populateSite(apiHelpers: ApiHelpers, manifest: Manifest) {
	const friendlyURLPath = '/upgrade-data-archive-content-pages';
	const name = 'Upgrade Data Archive Content Pages';

	await apiHelpers.headlessAdminSite.postSite({
		externalReferenceCode: SITE_EXTERNAL_REFERENCE_CODE,
		friendlyUrlPath: friendlyURLPath,
		name,
	});

	manifest.add('site', {
		externalReferenceCode: SITE_EXTERNAL_REFERENCE_CODE,
		friendlyURLPath,
		name,
	});
}

async function populateWidgetPage(apiHelpers: ApiHelpers, manifest: Manifest) {
	const externalReferenceCode = `${SITE_EXTERNAL_REFERENCE_CODE}_WIDGET_PAGE`;
	const friendlyURLPath = '/widget-page';
	const name = 'Widget Page';

	const page = {
		externalReferenceCode,
		friendlyUrlPath_i18n: {'en-US': friendlyURLPath},
		name_i18n: {'en-US': name},
		pageSettings: {
			hiddenFromNavigation: false,
			layoutTemplateId: '1_column',
			type: 'WidgetPageSettings',
		},
		pageSpecifications: [
			{
				externalReferenceCode,
				settings: {},
				status: 'Approved',
				type: 'WidgetPageSpecification',
				widgetPageSections: [
					{
						id: 'column-1',
						widgetPageWidgetInstances: [
							{
								externalReferenceCode: `${externalReferenceCode}_BREADCRUMB`,
								parentSectionId: 'column-1',
								position: 0,
								type: 'BasicWidgetPageWidgetInstance',
								widgetConfig: {
									showCurrentGroup: 'false',
								},
								widgetName:
									'com_liferay_site_navigation_breadcrumb_web_portlet_SiteNavigationBreadcrumbPortlet',
							},
						],
					},
				],
			},
		],
		type: 'WidgetPage',
	};

	await apiHelpers.headlessAdminSite.createPage(
		SITE_EXTERNAL_REFERENCE_CODE,
		page
	);

	manifest.add('widgetPage', {
		breadcrumb: name,
		externalReferenceCode,
		friendlyURLPath,
		pageSpecifications: getPageSpecifications(page),
		siteExternalReferenceCode: SITE_EXTERNAL_REFERENCE_CODE,
		type: page.type,
	});
}

async function verifyPages(
	apiHelpers: ApiHelpers,
	manifestData: ManifestData,
	kind: string
) {
	for (const recordedPage of getManifestEntities(manifestData, kind)) {
		const siteExternalReferenceCode =
			recordedPage.siteExternalReferenceCode as string;

		const page = await apiHelpers.headlessAdminSite.getPage(
			siteExternalReferenceCode,
			recordedPage.externalReferenceCode
		);

		expect(page.externalReferenceCode).toBe(
			recordedPage.externalReferenceCode
		);
		expect(page.type).toBe(recordedPage.type);

		await verifyPageSpecifications(
			apiHelpers,
			recordedPage,
			siteExternalReferenceCode
		);
	}
}

async function verifyPageSpecifications(
	apiHelpers: ApiHelpers,
	recordedPage: ManifestEntity,
	siteExternalReferenceCode: string
) {
	const pageSpecifications = (recordedPage.pageSpecifications ||
		[]) as PageSpecification[];

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
}

async function verifySites(
	apiHelpers: ApiHelpers,
	manifestData: ManifestData
) {
	for (const site of getManifestEntities(manifestData, 'site')) {
		const upgradedSite = await apiHelpers.headlessAdminSite.getSite(
			site.externalReferenceCode
		);

		expect(upgradedSite.name).toBe(site.name);
	}
}

const upgradeDataArchive: UpgradeDataArchive = {
	featureFlags: ['LPD-38869', 'LPD-76864'],
	name: 'content-pages',
	populate: async (apiHelpers, manifest) => {
		await populateSite(apiHelpers, manifest);

		await populateContentPage(apiHelpers, manifest);

		await populateWidgetPage(apiHelpers, manifest);

		await populatePrivatePage(apiHelpers, manifest);
	},
	verify: async (apiHelpers, manifestData) => {
		await verifySites(apiHelpers, manifestData);

		await verifyPages(apiHelpers, manifestData, 'contentPage');

		await verifyPages(apiHelpers, manifestData, 'privatePage');

		await verifyPages(apiHelpers, manifestData, 'widgetPage');
	},
};

export default upgradeDataArchive;
