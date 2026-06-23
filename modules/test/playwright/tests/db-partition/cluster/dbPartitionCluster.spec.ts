/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, mergeTests} from '@playwright/test';

import {apiHelpersTest} from '../../../fixtures/apiHelpersTest';
import {loginTest} from '../../../fixtures/loginTest';
import {liferayConfig} from '../../../liferay.config';
import {ServerAdministrationPage} from '../../../pages/server-admin-web/ServerAdministrationPage';
import {performLoginViaApi} from '../../../utils/performLogin';

const test = mergeTests(apiHelpersTest, loginTest());

const ABLE_HOST = 'www.able.com';

const GROOVY_SCRIPT = `
import com.liferay.portal.db.partition.util.DBPartitionUtil;
import java.util.concurrent.atomic.AtomicInteger;

AtomicInteger total = new AtomicInteger();

DBPartitionUtil.forEachCompanyId({companyId -> total.incrementAndGet()});

out.println(total.get());
`.trim();

test(
	'canAddCompanyWithCluster',
	{tag: '@LPD-91814'},
	async ({apiHelpers, browser}) => {
		const ableBaseUrl = liferayConfig.environment.baseUrl.replace(
			'localhost',
			ABLE_HOST
		);

		const instance =
			await apiHelpers.headlessPortalInstance.addVirtualInstance({
				domain: ABLE_HOST,
				portalInstanceId: ABLE_HOST,
				virtualHost: ABLE_HOST,
			});

		try {
			const instancePage = await browser.newPage({
				baseURL: ableBaseUrl,
			});

			try {
				await performLoginViaApi({
					domain: `@${ABLE_HOST}`,
					loginUrl: ableBaseUrl,
					page: instancePage,
					screenName: 'test',
				});

				await expect(instancePage).toHaveURL(new RegExp(ABLE_HOST));
			}
			finally {
				await instancePage.close();
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
	'canAddCompanyWithClusterSecondNode',
	{tag: '@LPD-91814'},
	async ({apiHelpers, browser}) => {
		const secondaryBaseUrl = liferayConfig.environment.baseUrl.replace(
			'8080',
			'9080'
		);

		const ableSecondaryUrl = secondaryBaseUrl.replace(
			'localhost',
			ABLE_HOST
		);

		const instance =
			await apiHelpers.headlessPortalInstance.addVirtualInstance({
				domain: ABLE_HOST,
				portalInstanceId: ABLE_HOST,
				virtualHost: ABLE_HOST,
			});

		try {
			const instancePage = await browser.newPage({
				baseURL: ableSecondaryUrl,
			});

			try {
				await performLoginViaApi({
					domain: `@${ABLE_HOST}`,
					loginUrl: ableSecondaryUrl,
					page: instancePage,
					screenName: 'test',
				});

				await expect(instancePage).toHaveURL(new RegExp(ABLE_HOST));
			}
			finally {
				await instancePage.close();
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
	'portalInstanceCreationDeletionUpdatesDBPartitionUtilWithCluster',
	{tag: '@LPD-91814'},
	async ({apiHelpers, browser}) => {
		const secondaryBaseUrl = liferayConfig.environment.baseUrl.replace(
			'8080',
			'9080'
		);

		const secondaryPage = await browser.newPage({
			baseURL: secondaryBaseUrl,
		});

		let instanceId: number | null = null;

		try {
			await performLoginViaApi({
				loginUrl: secondaryBaseUrl,
				page: secondaryPage,
				screenName: 'test',
			});

			const serverAdminPage = new ServerAdministrationPage(secondaryPage);

			await expect(async () => {
				await serverAdminPage.goto();
				await serverAdminPage.executeScript(GROOVY_SCRIPT);

				expect(await serverAdminPage.getScriptOutput()).toContain('1');
			}).toPass({timeout: 30000});

			const instance =
				await apiHelpers.headlessPortalInstance.addVirtualInstance({
					domain: ABLE_HOST,
					portalInstanceId: ABLE_HOST,
					virtualHost: ABLE_HOST,
				});

			instanceId = instance.id;

			await expect(async () => {
				await serverAdminPage.goto();
				await serverAdminPage.executeScript(GROOVY_SCRIPT);

				expect(await serverAdminPage.getScriptOutput()).toContain('2');
			}).toPass({timeout: 120000});

			await apiHelpers.headlessPortalInstance.deleteVirtualInstance(
				instance.id
			);

			instanceId = null;

			await expect(async () => {
				await serverAdminPage.goto();
				await serverAdminPage.executeScript(GROOVY_SCRIPT);

				expect(await serverAdminPage.getScriptOutput()).toContain('1');
			}).toPass({timeout: 120000});
		}
		finally {
			await secondaryPage.close();

			if (instanceId !== null) {
				await apiHelpers.headlessPortalInstance.deleteVirtualInstance(
					instanceId
				);
			}
		}
	}
);
