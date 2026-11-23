/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect} from '@playwright/test';

import {ApiHelpers} from '../../../../helpers/ApiHelpers';
import {ObjectEntryFolderExternalReferenceCode} from '../../../../utils/objectEntryFolderConstants';
import {
	Manifest,
	ManifestData,
	ManifestEntity,
	getManifestEntities,
} from '../../runner/manifest';
import {createPortalSession} from '../../runner/portalSession';
import {UpgradeDataArchive} from '../../runner/upgradeDataArchive';

const BASIC_DOCUMENTS_PATH = 'cms/basic-documents';

const BASIC_WEB_CONTENTS_PATH = 'cms/basic-web-contents';

const DEFAULT_PERMISSION_KEYS = [
	ObjectEntryFolderExternalReferenceCode.CONTENTS,
	ObjectEntryFolderExternalReferenceCode.FILES,
	'OBJECT_ENTRY_FOLDERS',
];

const FOLDER_CHILD_EXTERNAL_REFERENCE_CODE =
	'UPGRADE_DATA_ARCHIVE_CMS_FOLDER_CHILD';

const FOLDER_PERMISSIONS_EXTERNAL_REFERENCE_CODE =
	'UPGRADE_DATA_ARCHIVE_CMS_FOLDER_PERMISSIONS';

const ROLE_NAME = 'Upgrade Data Archive CMS Role';

const SPACE_EXTERNAL_REFERENCE_CODE = 'UPGRADE_DATA_ARCHIVE_CMS_SPACE';

const OBJECT_DEFINITIONS_PATH = 'object-admin/v1.0/object-definitions';

const STRUCTURE_A_EXTERNAL_REFERENCE_CODE =
	'UPGRADE_DATA_ARCHIVE_CMS_STRUCTURE_A';

const STRUCTURE_ENTRY_EXTERNAL_REFERENCE_CODE =
	'UPGRADE_DATA_ARCHIVE_CMS_STRUCTURE_ENTRY';

const STRUCTURE_ENTRY_NESTED_FIELDS =
	'nestedFields=cmsReferencedB,cmsRepeatableGroup,cmsGroupB&nestedFieldsDepth=2';

const STRUCTURE_B_EXTERNAL_REFERENCE_CODE =
	'UPGRADE_DATA_ARCHIVE_CMS_STRUCTURE_B';

type Entry = {
	externalReferenceCode: string;
	file?: {name: string};
	keywords?: string[];
	status: {code: number};
	taxonomyCategoryBriefs?: {taxonomyCategoryName: string}[];
	title: string;
};

function getObjectDefinition({
	externalReferenceCode,
	name,
	objectFolderExternalReferenceCode,
	objectRelationships = [],
	spaceExternalReferenceCode,
}: {
	externalReferenceCode: string;
	name: string;
	objectFolderExternalReferenceCode: string;
	objectRelationships?: {[key: string]: unknown}[];
	spaceExternalReferenceCode?: string;
}) {
	return {
		enableComments: true,
		enableFriendlyURLCustomization: true,
		enableIndexSearch: !!spaceExternalReferenceCode,
		enableLocalization: true,
		enableObjectEntryDraft: true,
		enableObjectEntryHistory: true,
		enableObjectEntrySchedule: true,
		enableObjectEntryVersioning: true,
		externalReferenceCode,
		label: {en_US: name},
		name,
		objectDefinitionSettings: spaceExternalReferenceCode
			? [
					{
						name: 'acceptedGroupExternalReferenceCodes',
						value: spaceExternalReferenceCode,
					},
				]
			: [{name: 'acceptAllGroups', value: 'true'}],
		objectFields: [
			{
				DBType: 'String',
				businessType: 'Text',
				label: {en_US: 'Title'},
				name: 'title',
				required: false,
			},
		],
		objectFolderExternalReferenceCode,
		objectRelationships,
		pluralLabel: {en_US: name},
		scope: 'depot',
		status: {code: 0},
		titleObjectFieldName: 'title',
	};
}

function getObjectRelationship({
	deletionType,
	edge = false,
	name,
	objectDefinitionExternalReferenceCode1,
	objectDefinitionExternalReferenceCode2,
	type,
}: {
	deletionType: 'cascade' | 'disassociate';
	edge?: boolean;
	name: string;
	objectDefinitionExternalReferenceCode1: string;
	objectDefinitionExternalReferenceCode2: string;
	type: 'manyToMany' | 'oneToMany';
}) {
	return {
		deletionType,
		edge,
		externalReferenceCode: `UPGRADE_DATA_ARCHIVE_CMS_${name.toUpperCase()}`,
		label: {en_US: name},
		name,
		objectDefinitionExternalReferenceCode1,
		objectDefinitionExternalReferenceCode2,
		type,
	};
}

function getStructureEntryChildren(structureEntry: {[key: string]: unknown}) {
	const getExternalReferenceCodes = (entries: unknown) =>
		((entries || []) as ManifestEntity[]).map(
			({externalReferenceCode}) => externalReferenceCode
		);

	return {
		referencedExternalReferenceCodes: getExternalReferenceCodes(
			structureEntry.cmsReferencedB
		),
		repeatableGroupExternalReferenceCodes: (
			(structureEntry.cmsRepeatableGroup || []) as {
				[key: string]: unknown;
			}[]
		).map((row) => getExternalReferenceCodes(row.cmsGroupB)),
	};
}

async function getFolder(
	apiHelpers: ApiHelpers,
	kind: string,
	externalReferenceCode: string
) {
	return getOrFail(
		apiHelpers,
		kind,
		externalReferenceCode,
		`${apiHelpers.baseUrl}headless-object/v1.0/scopes/${SPACE_EXTERNAL_REFERENCE_CODE}/object-entry-folder/by-external-reference-code/${externalReferenceCode}`
	);
}

async function getOrFail(
	apiHelpers: ApiHelpers,
	kind: string,
	externalReferenceCode: string,
	url: string
) {
	const response = await apiHelpers.getResponse(url);

	if (!response.ok()) {
		throw new Error(
			`Unable to find ${kind} "${externalReferenceCode}": ${response.status()}`
		);
	}

	return response.json();
}

export function getSpaceEntryURL(
	apiHelpers: ApiHelpers,
	path: string,
	externalReferenceCode: string
) {
	return `${apiHelpers.baseUrl}${path}/scopes/${SPACE_EXTERNAL_REFERENCE_CODE}/by-external-reference-code/${externalReferenceCode}`;
}

async function addEntry(
	apiHelpers: ApiHelpers,
	manifest: Manifest,
	kind: string,
	path: string,
	data: {[key: string]: unknown},
	draftData?: {[key: string]: unknown}
) {
	const externalReferenceCode = data.externalReferenceCode as string;

	await apiHelpers.objectEntry.postObjectEntry(
		data,
		path,
		SPACE_EXTERNAL_REFERENCE_CODE
	);

	const url = getSpaceEntryURL(apiHelpers, path, externalReferenceCode);

	if (draftData) {
		await apiHelpers.put(url, {
			data: {...data, ...draftData, status: {code: 2}},
			failOnStatusCode: true,
		});
	}

	const entry: Entry = await getOrFail(
		apiHelpers,
		kind,
		externalReferenceCode,
		url
	);

	if (
		(entry.keywords ?? []).length !==
			((data.keywords as string[]) ?? []).length ||
		(entry.taxonomyCategoryBriefs ?? []).length !==
			((data.taxonomyCategoryIds as number[]) ?? []).length
	) {
		throw new Error(
			`Unable to apply the keywords or categories of ${kind} "${externalReferenceCode}"`
		);
	}

	manifest.add(kind, {
		externalReferenceCode,
		fileName: entry.file?.name,
		keywords: entry.keywords ?? [],
		path,
		statusCode: entry.status.code,
		taxonomyCategoryNames: (entry.taxonomyCategoryBriefs ?? []).map(
			(taxonomyCategoryBrief) =>
				taxonomyCategoryBrief.taxonomyCategoryName
		),
		title: entry.title,
	});
}

async function addFolder(
	apiHelpers: ApiHelpers,
	manifest: Manifest,
	externalReferenceCode: string,
	parentObjectEntryFolderExternalReferenceCode: string,
	title: string
) {
	const folder = await apiHelpers.post(
		`${apiHelpers.baseUrl}headless-object/v1.0/scopes/${SPACE_EXTERNAL_REFERENCE_CODE}/object-entry-folders`,
		{
			data: {
				externalReferenceCode,
				parentObjectEntryFolderExternalReferenceCode,
				title,
			},
		}
	);

	manifest.add('folder', {
		externalReferenceCode,
		parentObjectEntryFolderExternalReferenceCode,
		title,
	});

	return folder;
}

async function populateComment(apiHelpers: ApiHelpers, manifest: Manifest) {
	const entryExternalReferenceCode = 'UPGRADE_DATA_ARCHIVE_CMS_CONTENT';
	const externalReferenceCode = 'UPGRADE_DATA_ARCHIVE_CMS_COMMENT';
	const text = '<p>Upgrade data archive comment</p>';

	await apiHelpers.post(
		`${getSpaceEntryURL(apiHelpers, BASIC_WEB_CONTENTS_PATH, entryExternalReferenceCode)}/comments`,
		{data: {externalReferenceCode, text}}
	);

	manifest.add('comment', {
		entryExternalReferenceCode,
		externalReferenceCode,
		text,
	});
}

async function populateEntries(
	apiHelpers: ApiHelpers,
	manifest: Manifest,
	keywordName: string,
	taxonomyCategoryId: number
) {
	await addEntry(
		apiHelpers,
		manifest,
		'basicWebContent',
		BASIC_WEB_CONTENTS_PATH,
		{
			content: '<p>Upgrade data archive content</p>',
			externalReferenceCode: 'UPGRADE_DATA_ARCHIVE_CMS_CONTENT',
			keywords: [keywordName],
			objectEntryFolderExternalReferenceCode:
				ObjectEntryFolderExternalReferenceCode.CONTENTS,
			taxonomyCategoryIds: [taxonomyCategoryId],
			title: 'Upgrade Data Archive CMS Content',
		}
	);

	await addEntry(
		apiHelpers,
		manifest,
		'basicWebContent',
		BASIC_WEB_CONTENTS_PATH,
		{
			content: '<p>Upgrade data archive content in a folder</p>',
			externalReferenceCode: 'UPGRADE_DATA_ARCHIVE_CMS_CONTENT_IN_FOLDER',
			keywords: [keywordName],
			objectEntryFolderExternalReferenceCode:
				FOLDER_CHILD_EXTERNAL_REFERENCE_CODE,
			taxonomyCategoryIds: [taxonomyCategoryId],
			title: 'Upgrade Data Archive CMS Content In Folder',
		}
	);

	await addEntry(
		apiHelpers,
		manifest,
		'basicWebContent',
		BASIC_WEB_CONTENTS_PATH,
		{
			content: '<p>Upgrade data archive published content</p>',
			externalReferenceCode: 'UPGRADE_DATA_ARCHIVE_CMS_CONTENT_DRAFT',
			objectEntryFolderExternalReferenceCode:
				ObjectEntryFolderExternalReferenceCode.CONTENTS,
			title: 'Upgrade Data Archive CMS Published Content',
		},
		{title: 'Upgrade Data Archive CMS Draft Content'}
	);

	await addEntry(
		apiHelpers,
		manifest,
		'basicWebContent',
		BASIC_WEB_CONTENTS_PATH,
		{
			content: '<p>Upgrade data archive draft only content</p>',
			externalReferenceCode:
				'UPGRADE_DATA_ARCHIVE_CMS_CONTENT_DRAFT_ONLY',
			objectEntryFolderExternalReferenceCode:
				ObjectEntryFolderExternalReferenceCode.CONTENTS,
			status: {code: 2},
			title: 'Upgrade Data Archive CMS Draft Only Content',
		}
	);

	await addEntry(
		apiHelpers,
		manifest,
		'basicDocument',
		BASIC_DOCUMENTS_PATH,
		{
			externalReferenceCode: 'UPGRADE_DATA_ARCHIVE_CMS_DOCUMENT',
			file: {
				fileBase64: Buffer.from(
					'Upgrade data archive document'
				).toString('base64'),
				name: 'upgrade-data-archive-cms.txt',
			},
			friendlyUrlPath: 'upgrade-data-archive-cms-document',
			keywords: [keywordName],
			objectEntryFolderExternalReferenceCode:
				FOLDER_PERMISSIONS_EXTERNAL_REFERENCE_CODE,
			taxonomyCategoryIds: [taxonomyCategoryId],
			title: 'Upgrade Data Archive CMS Document',
		}
	);
}

async function getDefaultDisplayPageTemplate(
	apiHelpers: ApiHelpers,
	className: string
) {
	const displayPageTemplates = await apiHelpers.get(
		`${apiHelpers.baseUrl}headless-admin-site/v1.0/sites/L_CMS/display-page-templates?pageSize=-1`,
		true
	);

	return (
		displayPageTemplates.items as {
			contentTypeReference: {className: string};
			externalReferenceCode: string;
			markedAsDefault: boolean;
			name: string;
		}[]
	).find(
		(displayPageTemplate) =>
			displayPageTemplate.markedAsDefault &&
			displayPageTemplate.contentTypeReference.className === className
	);
}

async function populateDisplayPageTemplates(
	apiHelpers: ApiHelpers,
	manifest: Manifest
) {
	const structureA = await getOrFail(
		apiHelpers,
		'structure',
		STRUCTURE_A_EXTERNAL_REFERENCE_CODE,
		`${apiHelpers.baseUrl}${OBJECT_DEFINITIONS_PATH}/by-external-reference-code/${STRUCTURE_A_EXTERNAL_REFERENCE_CODE}`
	);

	const structureEntry = await getOrFail(
		apiHelpers,
		'structureEntry',
		STRUCTURE_ENTRY_EXTERNAL_REFERENCE_CODE,
		`${getSpaceEntryURL(
			apiHelpers,
			(structureA.restContextPath as string).replace(/^\/o\//, ''),
			STRUCTURE_ENTRY_EXTERNAL_REFERENCE_CODE
		)}?${STRUCTURE_ENTRY_NESTED_FIELDS}`
	);

	const portalSession = await createPortalSession();

	try {
		for (const [structureExternalReferenceCode, objectEntryId] of [
			[STRUCTURE_A_EXTERNAL_REFERENCE_CODE, structureEntry.id],
			[
				STRUCTURE_B_EXTERNAL_REFERENCE_CODE,
				structureEntry.cmsReferencedB[0].id,
			],
		] as [string, number][]) {
			const response = await portalSession.get(
				`/c/cms/edit_content_item?objectEntryId=${objectEntryId}`,
				{maxRedirects: 0}
			);

			const location = response.headers()['location'] ?? '';

			if (response.status() !== 302 || !location.includes('/e/')) {
				throw new Error(
					`Unable to open the entry of structure "${structureExternalReferenceCode}" in the CMS admin: HTTP ${response.status()} ${location}`
				);
			}

			const objectDefinition = await getOrFail(
				apiHelpers,
				'structure',
				structureExternalReferenceCode,
				`${apiHelpers.baseUrl}${OBJECT_DEFINITIONS_PATH}/by-external-reference-code/${structureExternalReferenceCode}`
			);

			const displayPageTemplate = await getDefaultDisplayPageTemplate(
				apiHelpers,
				objectDefinition.className
			);

			if (!displayPageTemplate) {
				throw new Error(
					`Unable to create the display page template of structure "${structureExternalReferenceCode}"`
				);
			}

			manifest.add('displayPageTemplate', {
				className: objectDefinition.className,
				externalReferenceCode: displayPageTemplate.externalReferenceCode,
				name: displayPageTemplate.name,
				structureExternalReferenceCode,
			});
		}
	}
	finally {
		await portalSession.dispose();
	}
}

async function populateFolders(apiHelpers: ApiHelpers, manifest: Manifest) {
	const parentExternalReferenceCode =
		'UPGRADE_DATA_ARCHIVE_CMS_FOLDER_PARENT';

	await addFolder(
		apiHelpers,
		manifest,
		parentExternalReferenceCode,
		ObjectEntryFolderExternalReferenceCode.CONTENTS,
		'Upgrade Data Archive CMS Parent'
	);

	await addFolder(
		apiHelpers,
		manifest,
		FOLDER_CHILD_EXTERNAL_REFERENCE_CODE,
		parentExternalReferenceCode,
		'Upgrade Data Archive CMS Child'
	);

	const cmsDefaultPermission = await getCMSDefaultPermission(
		apiHelpers,
		FOLDER_CHILD_EXTERNAL_REFERENCE_CODE
	);

	const defaultPermissions = JSON.parse(
		cmsDefaultPermission.defaultPermissions || '{}'
	);

	for (const key of DEFAULT_PERMISSION_KEYS) {
		defaultPermissions[key] = {
			...(defaultPermissions[key] || {}),
			[ROLE_NAME]: ['VIEW'],
		};
	}

	await apiHelpers.patchRequestOptions(
		`${apiHelpers.baseUrl}cms/default-permissions/by-external-reference-code/${cmsDefaultPermission.externalReferenceCode}`,
		{
			data: {defaultPermissions: JSON.stringify(defaultPermissions)},
			failOnStatusCode: true,
		}
	);

	manifest.add('folderDefaultPermissions', {
		actionIds: ['VIEW'],
		externalReferenceCode: FOLDER_CHILD_EXTERNAL_REFERENCE_CODE,
		keys: DEFAULT_PERMISSION_KEYS,
		roleName: ROLE_NAME,
	});

	const folder = await addFolder(
		apiHelpers,
		manifest,
		FOLDER_PERMISSIONS_EXTERNAL_REFERENCE_CODE,
		ObjectEntryFolderExternalReferenceCode.FILES,
		'Upgrade Data Archive CMS Permissions'
	);

	await apiHelpers.put(
		`${apiHelpers.baseUrl}headless-object/v1.0/object-entry-folders/${folder.id}/permissions`,
		{
			data: [{actionIds: ['VIEW'], roleName: ROLE_NAME}],
			failOnStatusCode: true,
		}
	);

	manifest.add('folderPermissions', {
		actionIds: ['VIEW'],
		externalReferenceCode: FOLDER_PERMISSIONS_EXTERNAL_REFERENCE_CODE,
		roleName: ROLE_NAME,
	});
}

async function getCMSDefaultPermission(
	apiHelpers: ApiHelpers,
	folderExternalReferenceCode: string
) {
	const response =
		await apiHelpers.objectEntry.getObjectDefinitionObjectEntries(
			'cms/default-permissions',
			new URLSearchParams({
				filter: `(classExternalReferenceCode eq '${folderExternalReferenceCode}') and (className eq 'com.liferay.object.model.ObjectEntryFolder')`,
			})
		);

	const cmsDefaultPermission = response?.items?.[0];

	if (!cmsDefaultPermission) {
		throw new Error(
			`Unable to find the default permissions of folder "${folderExternalReferenceCode}"`
		);
	}

	return cmsDefaultPermission;
}

async function populateSpace(apiHelpers: ApiHelpers, manifest: Manifest) {
	const name = 'Upgrade Data Archive CMS';

	const space = await apiHelpers.headlessAssetLibrary.createSpace({
		externalReferenceCode: SPACE_EXTERNAL_REFERENCE_CODE,
		name,
	});

	if (space?.type !== 'Space') {
		throw new Error(
			`Unable to create space "${SPACE_EXTERNAL_REFERENCE_CODE}": ${JSON.stringify(space)}`
		);
	}

	manifest.add('space', {
		externalReferenceCode: SPACE_EXTERNAL_REFERENCE_CODE,
		name,
	});

	return space;
}

async function populateSpaceMember(apiHelpers: ApiHelpers, manifest: Manifest) {
	const externalReferenceCode = 'UPGRADE_DATA_ARCHIVE_CMS_USER';

	await apiHelpers.headlessAdminUser.postRole({
		externalReferenceCode: 'UPGRADE_DATA_ARCHIVE_CMS_ROLE',
		name: ROLE_NAME,
		roleType: 'depot',
	});

	await apiHelpers.headlessAdminUser.postUserAccount({
		alternateName: 'upgradedataarchivecms',
		emailAddress: 'upgradedataarchivecms@liferay.com',
		externalReferenceCode,
		familyName: 'CMS',
		givenName: 'Upgrade',
	});

	const userAccountURL = `${apiHelpers.baseUrl}headless-asset-library/v1.0/asset-libraries/${SPACE_EXTERNAL_REFERENCE_CODE}/user-accounts/${externalReferenceCode}`;

	await apiHelpers.put(userAccountURL, {failOnStatusCode: true});

	await apiHelpers.put(`${userAccountURL}/roles`, {
		data: [{name: ROLE_NAME}],
		failOnStatusCode: true,
	});

	manifest.add('spaceMember', {
		externalReferenceCode,
		roleNames: [ROLE_NAME],
	});
}

async function populateStructures(apiHelpers: ApiHelpers, manifest: Manifest) {
	const objectDefinitionsURL = `${apiHelpers.baseUrl}${OBJECT_DEFINITIONS_PATH}`;

	await apiHelpers.post(objectDefinitionsURL, {
		data: getObjectDefinition({
			externalReferenceCode: STRUCTURE_B_EXTERNAL_REFERENCE_CODE,
			name: 'UpgradeDataArchiveCMSStructureB',
			objectFolderExternalReferenceCode: 'L_CMS_CONTENT_STRUCTURES',
			spaceExternalReferenceCode: SPACE_EXTERNAL_REFERENCE_CODE,
		}),
	});

	const groupExternalReferenceCode = 'UPGRADE_DATA_ARCHIVE_CMS_GROUP';

	await apiHelpers.put(
		`${objectDefinitionsURL}/by-external-reference-code/${groupExternalReferenceCode}`,
		{
			data: getObjectDefinition({
				externalReferenceCode: groupExternalReferenceCode,
				name: 'UpgradeDataArchiveCMSGroup',
				objectFolderExternalReferenceCode:
					'L_CMS_STRUCTURE_REPEATABLE_GROUPS',
				objectRelationships: [
					getObjectRelationship({
						deletionType: 'cascade',
						edge: true,
						name: 'cmsGroupB',
						objectDefinitionExternalReferenceCode1:
							groupExternalReferenceCode,
						objectDefinitionExternalReferenceCode2:
							STRUCTURE_B_EXTERNAL_REFERENCE_CODE,
						type: 'oneToMany',
					}),
				],
			}),
			failOnStatusCode: true,
		}
	);

	const objectRelationships = [
		getObjectRelationship({
			deletionType: 'cascade',
			edge: true,
			name: 'cmsReferencedB',
			objectDefinitionExternalReferenceCode1:
				STRUCTURE_A_EXTERNAL_REFERENCE_CODE,
			objectDefinitionExternalReferenceCode2:
				STRUCTURE_B_EXTERNAL_REFERENCE_CODE,
			type: 'oneToMany',
		}),
		getObjectRelationship({
			deletionType: 'cascade',
			edge: true,
			name: 'cmsRepeatableGroup',
			objectDefinitionExternalReferenceCode1:
				STRUCTURE_A_EXTERNAL_REFERENCE_CODE,
			objectDefinitionExternalReferenceCode2: groupExternalReferenceCode,
			type: 'oneToMany',
		}),
		getObjectRelationship({
			deletionType: 'disassociate',
			name: 'cmsRelatedBs',
			objectDefinitionExternalReferenceCode1:
				STRUCTURE_A_EXTERNAL_REFERENCE_CODE,
			objectDefinitionExternalReferenceCode2:
				STRUCTURE_B_EXTERNAL_REFERENCE_CODE,
			type: 'manyToMany',
		}),
	];

	const structureA = await apiHelpers.post(objectDefinitionsURL, {
		data: getObjectDefinition({
			externalReferenceCode: STRUCTURE_A_EXTERNAL_REFERENCE_CODE,
			name: 'UpgradeDataArchiveCMSStructureA',
			objectFolderExternalReferenceCode: 'L_CMS_CONTENT_STRUCTURES',
			objectRelationships,
			spaceExternalReferenceCode: SPACE_EXTERNAL_REFERENCE_CODE,
		}),
	});

	const relatedBObjectRelationship = getObjectRelationship({
		deletionType: 'disassociate',
		name: 'cmsRelatedB',
		objectDefinitionExternalReferenceCode1:
			STRUCTURE_B_EXTERNAL_REFERENCE_CODE,
		objectDefinitionExternalReferenceCode2: STRUCTURE_A_EXTERNAL_REFERENCE_CODE,
		type: 'oneToMany',
	});

	await apiHelpers.post(
		`${objectDefinitionsURL}/by-external-reference-code/${STRUCTURE_B_EXTERNAL_REFERENCE_CODE}/object-relationships`,
		{data: relatedBObjectRelationship}
	);

	for (const [externalReferenceCode, objectRelationshipNames] of [
		[
			STRUCTURE_A_EXTERNAL_REFERENCE_CODE,
			objectRelationships.map(({name}) => name),
		],
		[
			STRUCTURE_B_EXTERNAL_REFERENCE_CODE,
			[relatedBObjectRelationship.name],
		],
	] as [string, string[]][]) {
		const objectDefinition = await getOrFail(
			apiHelpers,
			'structure',
			externalReferenceCode,
			`${objectDefinitionsURL}/by-external-reference-code/${externalReferenceCode}`
		);

		const upgradedObjectRelationshipNames = (
			objectDefinition.objectRelationships as {name: string}[]
		).map(({name}) => name);

		for (const objectRelationshipName of objectRelationshipNames) {
			if (
				!upgradedObjectRelationshipNames.includes(
					objectRelationshipName
				)
			) {
				throw new Error(
					`Unable to create relationship "${objectRelationshipName}" in structure "${externalReferenceCode}"`
				);
			}
		}

		manifest.add('structure', {
			externalReferenceCode,
			objectRelationshipNames,
		});
	}

	const path = (structureA.restContextPath as string).replace(/^\/o\//, '');

	await apiHelpers.objectEntry.postObjectEntry(
		{
			cmsReferencedB: [
				{
					externalReferenceCode:
						'UPGRADE_DATA_ARCHIVE_CMS_STRUCTURE_ENTRY_B',
					title: 'Upgrade Data Archive CMS Referenced Entry',
				},
			],
			cmsRepeatableGroup: [1, 2].map((row) => ({
				cmsGroupB: [
					{
						externalReferenceCode: `UPGRADE_DATA_ARCHIVE_CMS_STRUCTURE_ENTRY_GROUP_B_${row}`,
						title: `Upgrade Data Archive CMS Group Row ${row} Entry`,
					},
				],
				title: `Upgrade Data Archive CMS Group Row ${row}`,
			})),
			externalReferenceCode: STRUCTURE_ENTRY_EXTERNAL_REFERENCE_CODE,
			title: 'Upgrade Data Archive CMS Structure Entry',
		},
		path,
		SPACE_EXTERNAL_REFERENCE_CODE
	);

	const structureEntry = await getOrFail(
		apiHelpers,
		'structureEntry',
		STRUCTURE_ENTRY_EXTERNAL_REFERENCE_CODE,
		`${getSpaceEntryURL(apiHelpers, path, STRUCTURE_ENTRY_EXTERNAL_REFERENCE_CODE)}?${STRUCTURE_ENTRY_NESTED_FIELDS}`
	);

	const {
		referencedExternalReferenceCodes,
		repeatableGroupExternalReferenceCodes,
	} = getStructureEntryChildren(structureEntry);
	const repeatableGroupRows = repeatableGroupExternalReferenceCodes.length;

	if (
		referencedExternalReferenceCodes.length !== 1 ||
		repeatableGroupRows !== 2 ||
		repeatableGroupExternalReferenceCodes.some(
			(externalReferenceCodes) => externalReferenceCodes.length !== 1
		)
	) {
		throw new Error(
			`Unable to create the nested entries of structure entry "${STRUCTURE_ENTRY_EXTERNAL_REFERENCE_CODE}": ${JSON.stringify(structureEntry)}`
		);
	}

	manifest.add('structureEntry', {
		externalReferenceCode: STRUCTURE_ENTRY_EXTERNAL_REFERENCE_CODE,
		path,
		referencedExternalReferenceCodes,
		repeatableGroupExternalReferenceCodes,
	});
}

async function populateTaxonomy(
	apiHelpers: ApiHelpers,
	manifest: Manifest,
	spaceId: number
) {
	const taxonomyVocabulary =
		await apiHelpers.headlessAdminTaxonomy.postAssetLibraryTaxonomyVocabulary(
			{
				assetLibraryId: spaceId,
				name: 'Upgrade Data Archive CMS Vocabulary',
			}
		);

	const taxonomyCategory =
		await apiHelpers.headlessAdminTaxonomy.postTaxonomyVocabularyTaxonomyCategory(
			{
				name: 'Upgrade Data Archive CMS Category',
				vocabularyId: taxonomyVocabulary.id,
			}
		);

	const keywordName = 'upgrade data archive cms';

	await apiHelpers.headlessAdminTaxonomy.postAssetLibraryKeyword({
		depotEntryId: String(spaceId),
		name: keywordName,
	});

	manifest.add('taxonomyCategory', {
		externalReferenceCode: taxonomyCategory.externalReferenceCode,
		name: taxonomyCategory.name,
	});

	return {
		keywordName,
		taxonomyCategoryId: taxonomyCategory.id,
	};
}

async function verifyComments(
	apiHelpers: ApiHelpers,
	manifestData: ManifestData
) {
	for (const comment of getManifestEntities(manifestData, 'comment')) {
		const upgradedComment = await getOrFail(
			apiHelpers,
			'comment',
			comment.externalReferenceCode,
			`${getSpaceEntryURL(apiHelpers, BASIC_WEB_CONTENTS_PATH, comment.entryExternalReferenceCode as string)}/comments/${comment.externalReferenceCode}`
		);

		expect(
			upgradedComment.text,
			`comment "${comment.externalReferenceCode}" text`
		).toBe(comment.text);
	}
}

async function verifyDisplayPageTemplates(
	apiHelpers: ApiHelpers,
	manifestData: ManifestData
) {
	for (const displayPageTemplate of getManifestEntities(
		manifestData,
		'displayPageTemplate'
	)) {
		const upgradedDisplayPageTemplate = await getOrFail(
			apiHelpers,
			'displayPageTemplate',
			displayPageTemplate.externalReferenceCode,
			`${apiHelpers.baseUrl}headless-admin-site/v1.0/sites/L_CMS/display-page-templates/${displayPageTemplate.externalReferenceCode}`
		);

		const message = `display page template of structure "${displayPageTemplate.structureExternalReferenceCode}"`;

		expect(
			upgradedDisplayPageTemplate.contentTypeReference.className,
			`${message} content type`
		).toBe(displayPageTemplate.className);
		expect(
			upgradedDisplayPageTemplate.markedAsDefault,
			`${message} default`
		).toBe(true);
		expect(upgradedDisplayPageTemplate.name, `${message} name`).toBe(
			displayPageTemplate.name
		);
	}
}

async function verifyEntries(
	apiHelpers: ApiHelpers,
	manifestData: ManifestData,
	kind: string
) {
	for (const entry of getManifestEntities(manifestData, kind)) {
		const upgradedEntry: Entry = await getOrFail(
			apiHelpers,
			kind,
			entry.externalReferenceCode,
			getSpaceEntryURL(
				apiHelpers,
				entry.path as string,
				entry.externalReferenceCode
			)
		);

		const message = `${kind} "${entry.externalReferenceCode}"`;

		expect(upgradedEntry.title, `${message} title`).toBe(entry.title);
		expect(upgradedEntry.status.code, `${message} status`).toBe(
			entry.statusCode
		);
		expect(upgradedEntry.file?.name, `${message} file name`).toBe(
			entry.fileName
		);
		expect(upgradedEntry.keywords ?? [], `${message} keywords`).toEqual(
			entry.keywords
		);
		expect(
			(upgradedEntry.taxonomyCategoryBriefs ?? []).map(
				(taxonomyCategoryBrief) =>
					taxonomyCategoryBrief.taxonomyCategoryName
			),
			`${message} categories`
		).toEqual(entry.taxonomyCategoryNames);
	}
}

async function verifyFolderPermissions(
	apiHelpers: ApiHelpers,
	manifestData: ManifestData
) {
	for (const folderPermissions of getManifestEntities(
		manifestData,
		'folderPermissions'
	)) {
		const folder = await getFolder(
			apiHelpers,
			'folderPermissions',
			folderPermissions.externalReferenceCode
		);

		const response = await getOrFail(
			apiHelpers,
			'folderPermissions',
			folderPermissions.externalReferenceCode,
			`${apiHelpers.baseUrl}headless-object/v1.0/object-entry-folders/${folder.id}/permissions?roleNames=${encodeURIComponent(folderPermissions.roleName as string)}`
		);

		const permission = (
			response.items as {actionIds: string[]; roleName: string}[]
		)?.find(({roleName}) => roleName === folderPermissions.roleName);

		expect(
			permission?.actionIds,
			`folderPermissions "${folderPermissions.externalReferenceCode}" actionIds`
		).toEqual(folderPermissions.actionIds);
	}

	for (const folderDefaultPermissions of getManifestEntities(
		manifestData,
		'folderDefaultPermissions'
	)) {
		const cmsDefaultPermission = await getCMSDefaultPermission(
			apiHelpers,
			folderDefaultPermissions.externalReferenceCode
		);

		const defaultPermissions = JSON.parse(
			cmsDefaultPermission.defaultPermissions
		);

		for (const key of folderDefaultPermissions.keys as string[]) {
			expect(
				defaultPermissions[key]?.[
					folderDefaultPermissions.roleName as string
				],
				`folderDefaultPermissions "${folderDefaultPermissions.externalReferenceCode}" ${key}`
			).toEqual(folderDefaultPermissions.actionIds);
		}
	}
}

async function verifyFolders(
	apiHelpers: ApiHelpers,
	manifestData: ManifestData
) {
	for (const folder of getManifestEntities(manifestData, 'folder')) {
		const upgradedFolder = await getFolder(
			apiHelpers,
			'folder',
			folder.externalReferenceCode
		);

		const message = `folder "${folder.externalReferenceCode}"`;

		expect(upgradedFolder.title, `${message} title`).toBe(folder.title);
		expect(
			upgradedFolder.parentObjectEntryFolderExternalReferenceCode,
			`${message} parent`
		).toBe(folder.parentObjectEntryFolderExternalReferenceCode);
	}
}

async function verifySpaceMembers(
	apiHelpers: ApiHelpers,
	manifestData: ManifestData
) {
	for (const spaceMember of getManifestEntities(
		manifestData,
		'spaceMember'
	)) {
		const response = await getOrFail(
			apiHelpers,
			'spaceMember',
			spaceMember.externalReferenceCode,
			`${apiHelpers.baseUrl}headless-asset-library/v1.0/asset-libraries/${SPACE_EXTERNAL_REFERENCE_CODE}/user-accounts/${spaceMember.externalReferenceCode}/roles`
		);

		const roleNames = (response.items as {name: string}[]).map(
			({name}) => name
		);

		for (const roleName of spaceMember.roleNames as string[]) {
			expect(
				roleNames,
				`spaceMember "${spaceMember.externalReferenceCode}" roles`
			).toContain(roleName);
		}
	}
}

async function verifySpaces(
	apiHelpers: ApiHelpers,
	manifestData: ManifestData
) {
	for (const space of getManifestEntities(manifestData, 'space')) {
		const assetLibrary = await getOrFail(
			apiHelpers,
			'space',
			space.externalReferenceCode,
			`${apiHelpers.baseUrl}headless-asset-library/v1.0/asset-libraries/${space.externalReferenceCode}`
		);

		const message = `space "${space.externalReferenceCode}"`;

		expect(assetLibrary.name, `${message} name`).toBe(space.name);
		expect(assetLibrary.type, `${message} type`).toBe('Space');
	}
}

async function verifyStructureEntries(
	apiHelpers: ApiHelpers,
	manifestData: ManifestData
) {
	for (const structureEntry of getManifestEntities(
		manifestData,
		'structureEntry'
	)) {
		const upgradedStructureEntry = await getOrFail(
			apiHelpers,
			'structureEntry',
			structureEntry.externalReferenceCode,
			`${getSpaceEntryURL(apiHelpers, structureEntry.path as string, structureEntry.externalReferenceCode)}?${STRUCTURE_ENTRY_NESTED_FIELDS}`
		);

		const message = `structureEntry "${structureEntry.externalReferenceCode}"`;

		const {
			referencedExternalReferenceCodes,
			repeatableGroupExternalReferenceCodes,
		} = getStructureEntryChildren(upgradedStructureEntry);

		expect(
			referencedExternalReferenceCodes,
			`${message} referenced entries`
		).toEqual(structureEntry.referencedExternalReferenceCodes);
		expect(
			repeatableGroupExternalReferenceCodes,
			`${message} repeatable group rows`
		).toEqual(structureEntry.repeatableGroupExternalReferenceCodes);
	}
}

async function verifyStructures(
	apiHelpers: ApiHelpers,
	manifestData: ManifestData
) {
	for (const structure of getManifestEntities(manifestData, 'structure')) {
		const objectDefinition = await getOrFail(
			apiHelpers,
			'structure',
			structure.externalReferenceCode,
			`${apiHelpers.baseUrl}${OBJECT_DEFINITIONS_PATH}/by-external-reference-code/${structure.externalReferenceCode}`
		);

		const message = `structure "${structure.externalReferenceCode}"`;

		expect(objectDefinition.status.code, `${message} status`).toBe(0);

		const objectRelationshipNames = (
			objectDefinition.objectRelationships as {name: string}[]
		).map(({name}) => name);

		for (const objectRelationshipName of structure.objectRelationshipNames as string[]) {
			expect(
				objectRelationshipNames,
				`${message} relationships`
			).toContain(objectRelationshipName);
		}
	}
}

async function verifyTaxonomyCategories(
	apiHelpers: ApiHelpers,
	manifestData: ManifestData
) {
	const space = await apiHelpers.headlessAssetLibrary.getAssetLibrary(
		SPACE_EXTERNAL_REFERENCE_CODE
	);

	for (const taxonomyCategory of getManifestEntities(
		manifestData,
		'taxonomyCategory'
	)) {
		const upgradedTaxonomyCategory = await getOrFail(
			apiHelpers,
			'taxonomyCategory',
			taxonomyCategory.externalReferenceCode,
			`${apiHelpers.baseUrl}headless-admin-taxonomy/v1.0/asset-libraries/${space.id}/taxonomy-categories/by-external-reference-code/${taxonomyCategory.externalReferenceCode}`
		);

		expect(
			upgradedTaxonomyCategory.name,
			`taxonomyCategory "${taxonomyCategory.externalReferenceCode}" name`
		).toBe(taxonomyCategory.name);
	}
}

const upgradeDataArchive: UpgradeDataArchive = {
	featureFlags: ['LPD-43996'],
	name: 'cms',
	populate: async (apiHelpers, manifest) => {
		const space = await populateSpace(apiHelpers, manifest);

		await populateSpaceMember(apiHelpers, manifest);

		await populateFolders(apiHelpers, manifest);

		const {keywordName, taxonomyCategoryId} = await populateTaxonomy(
			apiHelpers,
			manifest,
			space.id
		);

		await populateStructures(apiHelpers, manifest);

		await populateDisplayPageTemplates(apiHelpers, manifest);

		await populateEntries(
			apiHelpers,
			manifest,
			keywordName,
			taxonomyCategoryId
		);

		await populateComment(apiHelpers, manifest);
	},
	verify: async (apiHelpers, manifestData) => {
		for (const kind of [
			'basicDocument',
			'basicWebContent',
			'comment',
			'displayPageTemplate',
			'folder',
			'folderDefaultPermissions',
			'folderPermissions',
			'space',
			'spaceMember',
			'structure',
			'structureEntry',
			'taxonomyCategory',
		]) {
			if (!getManifestEntities(manifestData, kind).length) {
				throw new Error(
					`Unable to find any "${kind}" in the manifest of archive "${manifestData.type}"`
				);
			}
		}

		await verifySpaces(apiHelpers, manifestData);

		await verifySpaceMembers(apiHelpers, manifestData);

		await verifyFolders(apiHelpers, manifestData);

		await verifyFolderPermissions(apiHelpers, manifestData);

		await verifyTaxonomyCategories(apiHelpers, manifestData);

		await verifyStructures(apiHelpers, manifestData);

		await verifyStructureEntries(apiHelpers, manifestData);

		await verifyDisplayPageTemplates(apiHelpers, manifestData);

		await verifyEntries(apiHelpers, manifestData, 'basicDocument');

		await verifyEntries(apiHelpers, manifestData, 'basicWebContent');

		await verifyComments(apiHelpers, manifestData);
	},
};

export default upgradeDataArchive;
