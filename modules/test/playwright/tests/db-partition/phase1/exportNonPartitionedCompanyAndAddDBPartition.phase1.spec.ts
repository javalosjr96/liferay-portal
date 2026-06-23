/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, mergeTests} from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

import {apiHelpersTest} from '../../../fixtures/apiHelpersTest';
import {loginTest} from '../../../fixtures/loginTest';
import {ApiHelpers} from '../../../helpers/ApiHelpers';
import {liferayConfig} from '../../../liferay.config';
import {performLoginViaApi} from '../../../utils/performLogin';
import {STATE_FILE} from '../utils';

const test = mergeTests(apiHelpersTest, loginTest());

const VIRTUAL_HOST = 'www.able.com';

const VIRTUAL_HOST_BASE_URL = `http://${VIRTUAL_HOST}:${liferayConfig.environment.port}`;

const DOCUMENT_PATH = path.join(
	__dirname,
	'../../../../../../portal-web/test/functional/com/liferay/portalweb/dependencies/Document_1.doc'
);

test.describe
	.serial('ExportNonPartitionedCompanyAndAddDBPartition — Phase 1', () => {
	test(
		'Add www.able.com portal instance',
		{tag: '@LPD-91814'},
		async ({apiHelpers}) => {
			await apiHelpers.headlessPortalInstance.addVirtualInstance({
				domain: VIRTUAL_HOST,
				portalInstanceId: VIRTUAL_HOST,
				virtualHost: VIRTUAL_HOST,
			});
		}
	);

	test(
		'Upload DM document to www.able.com and verify',
		{tag: '@LPD-91814'},
		async ({apiHelpers, browser}) => {
			const company =
				await apiHelpers.jsonWebServicesCompany.getCompanyByWebId(
					VIRTUAL_HOST
				);

			const companyGroup =
				await apiHelpers.jsonWebServicesGroup.getCompanyGroup(
					company.companyId
				);

			const virtualInstancePage = await browser.newPage({
				baseURL: VIRTUAL_HOST_BASE_URL,
			});

			await performLoginViaApi({
				domain: `@${VIRTUAL_HOST}`,
				loginUrl: VIRTUAL_HOST_BASE_URL,
				page: virtualInstancePage,
				screenName: 'test',
			});

			const virtualInstanceApiHelpers = new ApiHelpers(
				virtualInstancePage,
				VIRTUAL_HOST_BASE_URL
			);

			await virtualInstanceApiHelpers.headlessDelivery.postDocument(
				companyGroup.groupId,
				fs.createReadStream(DOCUMENT_PATH),
				{
					description: 'DM Document Description',
					fileName: 'Document_1.doc',
					title: 'DM Document Title',
				}
			);

			await virtualInstancePage.goto(
				'/~/control_panel/manage?p_p_id=com_liferay_document_library_web_portlet_DLAdminPortlet'
			);

			await expect(
				virtualInstancePage.getByRole('link', {
					name: 'DM Document Title',
				})
			).toBeVisible();

			fs.mkdirSync(path.dirname(STATE_FILE), {recursive: true});

			fs.writeFileSync(
				STATE_FILE,
				JSON.stringify({partitionCompanyId: company.companyId}, null, 2)
			);

			await virtualInstancePage.close();
		}
	);
});
