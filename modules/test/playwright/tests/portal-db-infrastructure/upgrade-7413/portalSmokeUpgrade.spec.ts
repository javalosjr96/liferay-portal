/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {mergeTests} from '@playwright/test';

import {loginTest} from '../../../fixtures/loginTest';
import {viewUpgradedPortalContent} from '../utils/viewUpgradedPortalContent';

const test = mergeTests(loginTest());

const ARCHIVE_ANNOTATIONS = [
	{type: 'data.archive.type', description: 'data-archive-portal'},
	{type: 'database.types', description: 'postgresql'},
	{type: 'portal.version', description: '7.4.13'},
];

test(
	'Can view upgraded portal content as admin',
	{annotation: ARCHIVE_ANNOTATIONS, tag: '@LPD-103549'},
	async ({page}) => {
		await viewUpgradedPortalContent(page);
	}
);
