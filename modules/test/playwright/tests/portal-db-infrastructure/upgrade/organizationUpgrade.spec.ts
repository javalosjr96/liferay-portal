/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Locator, Page, expect, mergeTests} from '@playwright/test';

import {apiHelpersTest} from '../../../fixtures/apiHelpersTest';
import {loginTest} from '../../../fixtures/loginTest';
import {usersAndOrganizationsPagesTest} from '../../../fixtures/usersAndOrganizationsPagesTest';
import {ApiHelpers} from '../../../helpers/ApiHelpers';
import {DataTablePage} from '../../../pages/account-admin-web/DataTablePage';
import {AssignUsersPage} from '../../../pages/users-admin-web/AssignUsersPage';
import {EditOrganizationPage} from '../../../pages/users-admin-web/EditOrganizationPage';
import {OrganizationUsersPage} from '../../../pages/users-admin-web/OrganizationUsersPage';
import {UsersAndOrganizationsPage} from '../../../pages/users-admin-web/UsersAndOrganizationsPage';
import {getRandomInt} from '../../../utils/getRandomInt';
import {waitForAlert} from '../../../utils/waitForAlert';

const test = mergeTests(
	apiHelpersTest,
	loginTest(),
	usersAndOrganizationsPagesTest
);

const ALL_DATABASE_TYPES = 'db2,mariadb,mysql,oracle,postgresql,sqlserver';

const ALL_DATABASE_TYPE_TAGS = ALL_DATABASE_TYPES.split(',').map(
	(databaseType) => `@${databaseType}`
);

const ARCHIVE_ORGANIZATION_NAME = 'Organization1';
const ARCHIVE_SUBORGANIZATION_NAME = 'Sub-organization-Pre';
const ARCHIVE_USER_FULL_NAME = 'userfn userln';

const EDITED_ORGANIZATION_NAME = 'Organization1 Edit';
const EDITED_SUBORGANIZATION_NAME = 'Sub-organization1 Edit';

const ORGANIZATION_2_NAME = 'Organization2';
const SUBORGANIZATION_1_NAME = 'Sub-organization1';
const SUBORGANIZATION_2_NAME = 'Sub-organization2';

const USER_EMAIL_ADDRESS = 'user2@liferay.com';
const USER_FULL_NAME = 'user2 user2';
const USER_SCREEN_NAME = 'user2';

async function addOrganization(
	editOrganizationPage: EditOrganizationPage,
	usersAndOrganizationsPage: UsersAndOrganizationsPage
) {
	const organizationName = `Organization${getRandomInt()}`;

	await test.step('Add an organization after the upgrade', async () => {
		await usersAndOrganizationsPage.goToOrganizations();

		await usersAndOrganizationsPage.addOrganizationButton.click();

		await editOrganizationPage.addOrganization(organizationName);

		await expect(editOrganizationPage.backButton).toBeVisible();
	});

	await test.step('View the added organization', async () => {
		await usersAndOrganizationsPage.goToOrganizations();

		await usersAndOrganizationsPage.organizationsTable.search(
			organizationName
		);

		await expect(
			usersAndOrganizationsPage.organizationsTable.cell(organizationName)
		).toBeVisible();
	});
}

async function clickRowAction(
	menuItem: Locator,
	table: DataTablePage,
	value: string
) {
	await expect(async () => {
		const rowActions = await table.rowActions(value);

		await rowActions.click();

		await expect(menuItem).toBeVisible({timeout: 500});
	}).toPass({timeout: 5000});

	await menuItem.click();
}

async function editOrganizationName(
	editOrganizationPage: EditOrganizationPage,
	newName: string,
	page: Page,
	usersAndOrganizationsPage: UsersAndOrganizationsPage,
	value: string
) {
	await clickRowAction(
		usersAndOrganizationsPage.editOrganizationMenuItem,
		usersAndOrganizationsPage.organizationsTable,
		value
	);

	await editOrganizationPage.nameInput.fill(newName);

	await editOrganizationPage.saveButton.click();

	await waitForAlert(page);

	await expect(editOrganizationPage.nameInput).toHaveValue(newName);
}

async function gotoOrganization(
	name: string,
	usersAndOrganizationsPage: UsersAndOrganizationsPage
) {
	await usersAndOrganizationsPage.goToOrganizations();

	await usersAndOrganizationsPage.organizationsTable.search(name);

	await usersAndOrganizationsPage.organizationsTable.valueLink(name).click();

	await expect(
		usersAndOrganizationsPage.organizationsBreadcrumbLink(name)
	).toBeVisible();
}

async function addUserAndSuborganization(
	apiHelpers: ApiHelpers,
	assignUsersPage: AssignUsersPage,
	editOrganizationPage: EditOrganizationPage,
	organizationUsersPage: OrganizationUsersPage,
	page: Page,
	usersAndOrganizationsPage: UsersAndOrganizationsPage
) {
	page.on('dialog', (dialog) => dialog.accept());

	const archiveOrganization =
		await apiHelpers.headlessAdminUser.getOrganizationByName(
			ARCHIVE_ORGANIZATION_NAME
		);

	await test.step('Create the organizations and user the upgrade did not supply', async () => {
		const organization = await apiHelpers.headlessAdminUser.postOrganization(
			{name: ORGANIZATION_2_NAME}
		);

		await apiHelpers.headlessAdminUser.postOrganization({
			name: SUBORGANIZATION_2_NAME,
			parentOrganization: {id: organization.id},
		});

		await apiHelpers.headlessAdminUser.postOrganization({
			name: SUBORGANIZATION_1_NAME,
			parentOrganization: {id: String(archiveOrganization.id)},
		});

		await apiHelpers.headlessAdminUser.postUserAccount({
			alternateName: USER_SCREEN_NAME,
			emailAddress: USER_EMAIL_ADDRESS,
			familyName: USER_SCREEN_NAME,
			givenName: USER_SCREEN_NAME,
		});

		await apiHelpers.headlessAdminUser.assignUserToOrganizationByEmailAddress(
			String(archiveOrganization.id),
			USER_EMAIL_ADDRESS
		);
	});

	await test.step('Assign the user to the new suborganization', async () => {
		await gotoOrganization(
			ARCHIVE_ORGANIZATION_NAME,
			usersAndOrganizationsPage
		);

		await clickRowAction(
			usersAndOrganizationsPage.assignUsersMenuItem,
			usersAndOrganizationsPage.organizationsTable,
			SUBORGANIZATION_1_NAME
		);

		const rowCheckbox =
			await assignUsersPage.usersTableRowCheckbox(USER_FULL_NAME);

		await rowCheckbox.check();

		await assignUsersPage.doneButton.click();

		await waitForAlert(page);
	});

	await test.step('View the user the upgrade migrated', async () => {
		await gotoOrganization(
			ARCHIVE_ORGANIZATION_NAME,
			usersAndOrganizationsPage
		);

		const usersTableRowLink =
			await organizationUsersPage.usersTableRowLink(ARCHIVE_USER_FULL_NAME);

		await expect(usersTableRowLink).toBeVisible();
	});

	await test.step('View the suborganization the upgrade migrated', async () => {
		await gotoOrganization(
			ARCHIVE_ORGANIZATION_NAME,
			usersAndOrganizationsPage
		);

		await expect(
			usersAndOrganizationsPage.organizationsTable.cell(
				ARCHIVE_SUBORGANIZATION_NAME
			)
		).toBeVisible();

		await expect(
			usersAndOrganizationsPage.organizationsTable.cell('Organization')
		).toBeVisible();
	});

	await test.step('View the migrated user inside the migrated suborganization', async () => {
		await gotoOrganization(
			ARCHIVE_ORGANIZATION_NAME,
			usersAndOrganizationsPage
		);

		await usersAndOrganizationsPage.organizationsTable
			.valueLink(ARCHIVE_SUBORGANIZATION_NAME)
			.click();

		const usersTableRowLink =
			await organizationUsersPage.usersTableRowLink(ARCHIVE_USER_FULL_NAME);

		await expect(usersTableRowLink).toBeVisible();
	});

	await test.step('Edit the organization the upgrade migrated', async () => {
		await usersAndOrganizationsPage.goToOrganizations();

		await usersAndOrganizationsPage.organizationsTable.search(
			ARCHIVE_ORGANIZATION_NAME
		);

		await editOrganizationName(
			editOrganizationPage,
			EDITED_ORGANIZATION_NAME,
			page,
			usersAndOrganizationsPage,
			ARCHIVE_ORGANIZATION_NAME
		);
	});

	await test.step('Edit the new suborganization', async () => {
		await gotoOrganization(
			EDITED_ORGANIZATION_NAME,
			usersAndOrganizationsPage
		);

		await editOrganizationName(
			editOrganizationPage,
			EDITED_SUBORGANIZATION_NAME,
			page,
			usersAndOrganizationsPage,
			SUBORGANIZATION_1_NAME
		);
	});

	await test.step('Delete the new suborganization and organization', async () => {
		await gotoOrganization(ORGANIZATION_2_NAME, usersAndOrganizationsPage);

		await clickRowAction(
			usersAndOrganizationsPage.deleteOrganizationMenuItem,
			usersAndOrganizationsPage.organizationsTable,
			SUBORGANIZATION_2_NAME
		);

		await waitForAlert(page);

		await usersAndOrganizationsPage.goToOrganizations();

		await usersAndOrganizationsPage.organizationsTable.search(
			ORGANIZATION_2_NAME
		);

		await clickRowAction(
			usersAndOrganizationsPage.deleteOrganizationMenuItem,
			usersAndOrganizationsPage.organizationsTable,
			ORGANIZATION_2_NAME
		);

		await waitForAlert(page);
	});

	await test.step('Remove the user from the edited suborganization', async () => {
		await gotoOrganization(
			EDITED_ORGANIZATION_NAME,
			usersAndOrganizationsPage
		);

		await usersAndOrganizationsPage.organizationsTable
			.valueLink(EDITED_SUBORGANIZATION_NAME)
			.click();

		const usersTableRowActions =
			await organizationUsersPage.usersTableRowActions(USER_FULL_NAME);

		await usersTableRowActions.click();

		await organizationUsersPage.removeMenuItem.click();

		const screenName = await organizationUsersPage.screenName(USER_FULL_NAME);

		await expect(screenName).toHaveCount(0);
	});

	await test.step('Delete the edited suborganization', async () => {
		await gotoOrganization(
			EDITED_ORGANIZATION_NAME,
			usersAndOrganizationsPage
		);

		await clickRowAction(
			usersAndOrganizationsPage.deleteOrganizationMenuItem,
			usersAndOrganizationsPage.organizationsTable,
			EDITED_SUBORGANIZATION_NAME
		);

		await waitForAlert(page);
	});

	await test.step('View the edited organization', async () => {
		await usersAndOrganizationsPage.goToOrganizations();

		await usersAndOrganizationsPage.organizationsTable.search(
			EDITED_ORGANIZATION_NAME
		);

		await expect(
			usersAndOrganizationsPage.organizationsTable.cell(
				EDITED_ORGANIZATION_NAME
			)
		).toBeVisible();

		await expect(
			usersAndOrganizationsPage.organizationsTable.cell('Organization')
		).toBeVisible();
	});
}


test(
	'Can add an organization after upgrading from 6.2.5',
	{
		annotation: [
			{type: 'data.archive.type', description: 'data-archive-portal'},
			{
				type: 'database.types',
				description: 'mariadb,mysql,postgresql',
			},
			{type: 'portal.version', description: '6.2.5'},
		],
		tag: [
			'@6.2.5',
			'@data-archive-portal',
			'@mariadb',
			'@mysql',
			'@postgresql',
			'@LPD-82362',
		],
	},
	async ({editOrganizationPage, usersAndOrganizationsPage}) => {
		await addOrganization(
			editOrganizationPage,
			usersAndOrganizationsPage
		);
	}
);

test(
	'Can add an organization after upgrading from 6.2.10.21',
	{
		annotation: [
			{type: 'data.archive.type', description: 'data-archive-portal'},
			{
				type: 'database.types',
				description: 'db2,mariadb,mysql,oracle,postgresql',
			},
			{type: 'portal.version', description: '6.2.10.21'},
		],
		tag: [
			'@6.2.10.21',
			'@data-archive-portal',
			'@db2',
			'@mariadb',
			'@mysql',
			'@oracle',
			'@postgresql',
			'@LPD-82362',
		],
	},
	async ({editOrganizationPage, usersAndOrganizationsPage}) => {
		await addOrganization(
			editOrganizationPage,
			usersAndOrganizationsPage
		);
	}
);

test(
	'Can add an organization after upgrading from 7.0.10.6',
	{
		annotation: [
			{type: 'data.archive.type', description: 'data-archive-portal'},
			{type: 'database.types', description: ALL_DATABASE_TYPES},
			{type: 'portal.version', description: '7.0.10.6'},
		],
		tag: [
			'@7.0.10.6',
			'@data-archive-portal',
			...ALL_DATABASE_TYPE_TAGS,
			'@LPD-82362',
		],
	},
	async ({editOrganizationPage, usersAndOrganizationsPage}) => {
		await addOrganization(
			editOrganizationPage,
			usersAndOrganizationsPage
		);
	}
);

test(
	'Can add a user and suborganization after upgrading from 7.0.10.6',
	{
		annotation: [
			{
				type: 'data.archive.type',
				description: 'data-archive-admin-org-with-user',
			},
			{type: 'database.types', description: ALL_DATABASE_TYPES},
			{type: 'portal.version', description: '7.0.10.6'},
		],
		tag: [
			'@7.0.10.6',
			'@data-archive-admin-org-with-user',
			...ALL_DATABASE_TYPE_TAGS,
			'@LPD-82362',
		],
	},
	async ({
		apiHelpers,
		assignUsersPage,
		editOrganizationPage,
		organizationUsersPage,
		page,
		usersAndOrganizationsPage,
	}) => {
		await addUserAndSuborganization(
			apiHelpers,
			assignUsersPage,
			editOrganizationPage,
			organizationUsersPage,
			page,
			usersAndOrganizationsPage
		);
	}
);

test(
	'Can add a user and suborganization after upgrading from 7.4.13',
	{
		annotation: [
			{
				type: 'data.archive.type',
				description: 'data-archive-admin-org-with-user',
			},
			{type: 'database.types', description: ALL_DATABASE_TYPES},
			{type: 'portal.version', description: '7.4.13'},
		],
		tag: [
			'@7.4.13',
			'@data-archive-admin-org-with-user',
			...ALL_DATABASE_TYPE_TAGS,
			'@LPD-82362',
		],
	},
	async ({
		apiHelpers,
		assignUsersPage,
		editOrganizationPage,
		organizationUsersPage,
		page,
		usersAndOrganizationsPage,
	}) => {
		await addUserAndSuborganization(
			apiHelpers,
			assignUsersPage,
			editOrganizationPage,
			organizationUsersPage,
			page,
			usersAndOrganizationsPage
		);
	}
);
