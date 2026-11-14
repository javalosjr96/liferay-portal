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

type CustomMetaTag = {
	key: string;
	value: string;
};

type PageSpecification = {
	externalReferenceCode: string;
	heading?: string;
	status: string;
};

type Widget = {
	widgetConfig: Record<string, string>;
	widgetName: string;
};

function getContentPage(
	customMetaTags: CustomMetaTag[],
	externalReferenceCode: string,
	friendlyURLPath: string,
	name: string,
	pageSpecifications: PageSpecification[]
) {
	const [draftPageSpecification, approvedPageSpecification] =
		pageSpecifications;

	return {
		externalReferenceCode,
		friendlyUrlPath_i18n: {'en-US': friendlyURLPath},
		name_i18n: {'en-US': name},
		pageSettings: {
			customMetaTags: customMetaTags.map(({key, value}) => ({
				key,
				value_i18n: {'en-US': value},
			})),
			hiddenFromNavigation: false,
			type: 'ContentPageSettings',
		},
		pageSpecifications: [
			getContentPageSpecification(draftPageSpecification),
			getContentPageSpecification(
				approvedPageSpecification,
				draftPageSpecification.externalReferenceCode
			),
		],
		type: 'ContentPage',
	};
}

function getContentPageSpecification(
	pageSpecification: PageSpecification,
	draftExternalReferenceCode?: string
) {
	const {externalReferenceCode, heading, status} = pageSpecification;

	const containerExternalReferenceCode = `${externalReferenceCode}_CONTAINER`;

	return {
		draftContentPageSpecificationExternalReferenceCode:
			draftExternalReferenceCode,
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

function getContentPageSpecifications(
	externalReferenceCode: string,
	heading: string
): PageSpecification[] {
	return [
		{
			externalReferenceCode: `${externalReferenceCode}_DRAFT`,
			heading: `${heading} Draft`,
			status: 'Draft',
		},
		{externalReferenceCode, heading, status: 'Approved'},
	];
}

function getFragmentEditableElements(pageElements: any[]): any[] {
	return (pageElements || []).flatMap((pageElement: any) => [
		...(pageElement.pageElementDefinition.fragmentInstance
			?.fragmentEditableElements || []),
		...getFragmentEditableElements(pageElement.pageElements),
	]);
}

function getHeadings(pageSpecification: any) {
	const fragmentEditableElements = pageSpecification.pageExperiences.flatMap(
		(pageExperience: any) =>
			getFragmentEditableElements(pageExperience.pageElements)
	);

	return fragmentEditableElements.map(
		(fragmentEditableElement: any) =>
			fragmentEditableElement.fragmentEditableElementValue
				.fragmentLinkTextValue.textFragmentValue.fragmentInlineValue
				.value_i18n['en-US']
	);
}

async function populateContentPage(
	apiHelpers: ApiHelpers,
	kind: string,
	label: string,
	manifest: Manifest,
	privateLayout: boolean
) {
	const customMetaTags = [
		{key: 'upgrade-data-archive', value: `Archived ${label} Meta Tag`},
	];
	const externalReferenceCode = `${SITE_EXTERNAL_REFERENCE_CODE}_${label.toUpperCase()}_PAGE`;
	const friendlyURLPath = `/${label.toLowerCase()}-page`;
	const pageSpecifications = getContentPageSpecifications(
		externalReferenceCode,
		`Archived ${label} Heading`
	);

	const page = getContentPage(
		customMetaTags,
		externalReferenceCode,
		friendlyURLPath,
		`${label} Page`,
		pageSpecifications
	);

	if (privateLayout) {
		await apiHelpers.headlessAdminSite.createPrivatePage(
			SITE_EXTERNAL_REFERENCE_CODE,
			page
		);
	}
	else {
		await apiHelpers.headlessAdminSite.createPage(
			SITE_EXTERNAL_REFERENCE_CODE,
			page
		);
	}

	manifest.add(kind, {
		customMetaTags,
		externalReferenceCode,
		friendlyURLPath,
		pageSpecifications,
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
	const widget: Widget = {
		widgetConfig: {showCurrentGroup: 'false'},
		widgetName:
			'com_liferay_site_navigation_breadcrumb_web_portlet_SiteNavigationBreadcrumbPortlet',
	};

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
								...widget,
								externalReferenceCode: `${externalReferenceCode}_BREADCRUMB`,
								parentSectionId: 'column-1',
								position: 0,
								type: 'BasicWidgetPageWidgetInstance',
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
		pageSpecifications: [{externalReferenceCode, status: 'Approved'}],
		siteExternalReferenceCode: SITE_EXTERNAL_REFERENCE_CODE,
		type: page.type,
		widget,
	});
}

async function verifyContentPages(
	apiHelpers: ApiHelpers,
	manifestData: ManifestData,
	kind: string
) {
	for (const recordedPage of getManifestEntities(manifestData, kind)) {
		const {page} = await verifyPage(apiHelpers, recordedPage);

		for (const {key, value} of recordedPage.customMetaTags as CustomMetaTag[]) {
			expect(page.pageSettings.customMetaTags).toContainEqual(
				expect.objectContaining({
					key,
					value_i18n: expect.objectContaining({'en-US': value}),
				})
			);
		}
	}
}

async function verifyPage(apiHelpers: ApiHelpers, recordedPage: ManifestEntity) {
	const siteExternalReferenceCode =
		recordedPage.siteExternalReferenceCode as string;

	const page = await apiHelpers.headlessAdminSite.getPage(
		siteExternalReferenceCode,
		recordedPage.externalReferenceCode
	);

	expect(page.externalReferenceCode).toBe(recordedPage.externalReferenceCode);
	expect(page.type).toBe(recordedPage.type);

	const upgradedPageSpecifications = [];

	for (const pageSpecification of recordedPage.pageSpecifications as PageSpecification[]) {
		const upgradedPageSpecification =
			await apiHelpers.headlessAdminSite.getPageSpecification(
				siteExternalReferenceCode,
				pageSpecification.externalReferenceCode
			);

		expect(upgradedPageSpecification.status).toBe(
			pageSpecification.status
		);

		if (pageSpecification.heading) {
			expect(getHeadings(upgradedPageSpecification)).toContain(
				pageSpecification.heading
			);
		}

		upgradedPageSpecifications.push(upgradedPageSpecification);
	}

	return {page, pageSpecifications: upgradedPageSpecifications};
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

async function verifyWidgetPages(
	apiHelpers: ApiHelpers,
	manifestData: ManifestData
) {
	for (const recordedPage of getManifestEntities(
		manifestData,
		'widgetPage'
	)) {
		const {pageSpecifications} = await verifyPage(apiHelpers, recordedPage);

		const {widgetConfig, widgetName} = recordedPage.widget as Widget;

		for (const pageSpecification of pageSpecifications) {
			const widgetPageWidgetInstances =
				pageSpecification.widgetPageSections.flatMap(
					(widgetPageSection: any) =>
						widgetPageSection.widgetPageWidgetInstances
				);

			expect(widgetPageWidgetInstances).toContainEqual(
				expect.objectContaining({
					widgetConfig: expect.objectContaining(widgetConfig),
					widgetName,
				})
			);
		}
	}
}

const upgradeDataArchive: UpgradeDataArchive = {
	featureFlags: ['LPD-38869', 'LPD-76864'],
	name: 'content-pages',
	populate: async (apiHelpers, manifest) => {
		await populateSite(apiHelpers, manifest);

		await populateContentPage(
			apiHelpers,
			'contentPage',
			'Content',
			manifest,
			false
		);

		await populateWidgetPage(apiHelpers, manifest);

		await populateContentPage(
			apiHelpers,
			'privatePage',
			'Private',
			manifest,
			true
		);
	},
	verify: async (apiHelpers, manifestData) => {
		await verifySites(apiHelpers, manifestData);

		await verifyContentPages(apiHelpers, manifestData, 'contentPage');

		await verifyWidgetPages(apiHelpers, manifestData);

		await verifyContentPages(apiHelpers, manifestData, 'privatePage');
	},
};

export default upgradeDataArchive;
