/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import * as fs from 'fs';
import * as path from 'path';

import {expect, mergeTests} from '@playwright/test';

import {apiHelpersTest} from '../../../fixtures/apiHelpersTest';
import {loginTest} from '../../../fixtures/loginTest';
import {liferayConfig} from '../../../liferay.config';
import {performLoginViaApi} from '../../../utils/performLogin';

const test = mergeTests(apiHelpersTest, loginTest());

const VIRTUAL_HOST = 'www.able.com';

const VIRTUAL_HOST_BASE_URL = `http://${VIRTUAL_HOST}:${liferayConfig.environment.port}`;

const STATE_FILE = path.join(
	__dirname,
	'../../../test-results/db-partition-state.json'
);

function readState(): {partitionCompanyId: string} {
	return JSON.parse(fs.readFileSync(STATE_FILE, 'utf-8'));
}

test.describe.serial('ExportAndAddDBPartition — Phase 2', () => {
	test(
		'Default portal instance is accessible after DB rebuild',
		{tag: '@LPD-91814'},
		async ({page}) => {
			await page.goto('/');

			await expect(
				page.getByText('Welcome to Liferay')
			).toBeVisible();
		}
	);

	test(
		'www.able.com is accessible and DM document is visible after partition import',
		{tag: '@LPD-91814'},
		async ({browser}) => {
			const {partitionCompanyId} = readState();

			expect(partitionCompanyId).toBeTruthy();

			const virtualInstancePage = await browser.newPage({
				baseURL: VIRTUAL_HOST_BASE_URL,
			});

			await performLoginViaApi({
				domain: `@${VIRTUAL_HOST}`,
				loginUrl: VIRTUAL_HOST_BASE_URL,
				page: virtualInstancePage,
				screenName: 'test',
			});

			await virtualInstancePage.goto(
				'/~/control_panel/manage?p_p_id=com_liferay_document_library_web_portlet_DLAdminPortlet'
			);

			await expect(
				virtualInstancePage.getByRole('link', {
					name: 'DM Document Title',
				})
			).toBeVisible();

			await virtualInstancePage.close();
		}
	);
});
