/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, mergeTests} from '@playwright/test';

import {apiHelpersTest} from '../../../fixtures/apiHelpersTest';
import {loginTest} from '../../../fixtures/loginTest';
import {EActions} from '../../../helpers/ServerAdministrationHelper';
import {liferayConfig} from '../../../liferay.config';
import {ServerAdministrationPage} from '../../../pages/server-admin-web/ServerAdministrationPage';
import {SiteSettingsPage} from '../../../pages/users-admin-web/site-admin-web/SiteSettingsPage';
import {performLoginViaApi} from '../../../utils/performLogin';

const test = mergeTests(apiHelpersTest, loginTest());

test(
	'canAddCompanyWithHeadlessAPI',
	{tag: '@LPD-91814'},
	async ({apiHelpers, browser}) => {
		const VIRTUAL_HOST = 'www.baker.com';
		const VIRTUAL_HOST_BASE_URL = `http://${VIRTUAL_HOST}:${liferayConfig.environment.port}`;

		const instance =
			await apiHelpers.headlessPortalInstance.addVirtualInstance({
				domain: VIRTUAL_HOST,
				portalInstanceId: VIRTUAL_HOST,
				virtualHost: VIRTUAL_HOST,
			});

		try {
			const virtualInstancePage = await browser.newPage({
				baseURL: VIRTUAL_HOST_BASE_URL,
			});

			try {
				await performLoginViaApi({
					domain: `@${VIRTUAL_HOST}`,
					loginUrl: VIRTUAL_HOST_BASE_URL,
					page: virtualInstancePage,
					screenName: 'test',
				});

				await expect(virtualInstancePage).toHaveURL(
					new RegExp(VIRTUAL_HOST)
				);
			}
			finally {
				await virtualInstancePage.close();
			}
		}
		finally {
			await apiHelpers.headlessPortalInstance.deleteVirtualInstance(
				instance.id
			);
		}
	}
);

test(
	'canSetVirtualHostViaPages',
	{tag: '@LPD-91814'},
	async ({apiHelpers, browser, page}) => {
		const ABLE_HOST = 'www.able.com';
		const BAKER_HOST = 'www.baker.com';
		const BAKER_BASE_URL = `http://${BAKER_HOST}:${liferayConfig.environment.port}`;

		const instance =
			await apiHelpers.headlessPortalInstance.addVirtualInstance({
				domain: ABLE_HOST,
				portalInstanceId: ABLE_HOST,
				virtualHost: ABLE_HOST,
			});

		try {
			const ABLE_BASE_URL = `http://${ABLE_HOST}:${liferayConfig.environment.port}`;

			const virtualPage = await browser.newPage({
				baseURL: ABLE_BASE_URL,
			});

			try {
				await performLoginViaApi({
					domain: `@${ABLE_HOST}`,
					loginUrl: ABLE_BASE_URL,
					page: virtualPage,
					screenName: 'test',
				});

				const siteSettingsPage = new SiteSettingsPage(virtualPage);

				await siteSettingsPage.setVirtualHost('guest', BAKER_HOST);

				await virtualPage.goto(`${BAKER_BASE_URL}/web/guest/home`);

				await expect(virtualPage).toHaveURL(
					`${BAKER_BASE_URL}/web/guest/home`
				);
			}
			finally {
				await virtualPage.close();
			}

			const serverAdminPage = new ServerAdministrationPage(page);

			await serverAdminPage.goto();
			await serverAdminPage.executeAction(EActions.CLEAR_DATABASE_CACHE);

			const bakerPage = await browser.newPage({
				baseURL: BAKER_BASE_URL,
			});

			try {
				await bakerPage.goto('/web/guest/home');

				await expect(bakerPage).toHaveURL(
					`${BAKER_BASE_URL}/web/guest/home`
				);
			}
			finally {
				await bakerPage.close();
			}
		}
		finally {
			await apiHelpers.headlessPortalInstance.deleteVirtualInstance(
				instance.id
			);
		}
	}
);

test('canDeleteCompany', {tag: '@LPD-91814'}, async ({apiHelpers, page}) => {
	const VIRTUAL_HOST = 'www.able.com';

	const instance = await apiHelpers.headlessPortalInstance.addVirtualInstance(
		{
			domain: VIRTUAL_HOST,
			portalInstanceId: VIRTUAL_HOST,
			virtualHost: VIRTUAL_HOST,
		}
	);

	let deleted = false;

	try {
		await page.goto(
			'/~/control_panel/manage?p_p_id=com_liferay_portal_instances_web_portlet_PortalInstancesPortlet'
		);

		await page
			.getByRole('row', {name: VIRTUAL_HOST})
			.getByRole('button', {name: 'Actions'})
			.click();

		await page.getByRole('menuitem', {name: 'Delete'}).click();

		await page.getByRole('button', {name: 'OK'}).click();

		await expect(
			page.getByRole('cell', {exact: true, name: VIRTUAL_HOST})
		).not.toBeVisible();

		deleted = true;
	}
	finally {
		if (!deleted) {
			await apiHelpers.headlessPortalInstance.deleteVirtualInstance(
				instance.id
			);
		}
	}
});
