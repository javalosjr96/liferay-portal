/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, test} from '@playwright/test';
import path from 'path';

import {MANIFEST_FORMAT_VERSION} from '../../../scripts/upgrade-data-archive/runner/manifest';
import {getZipEntryNames, readFileFromZip} from '../../../utils/zip';

test(
	'generates an upgrade data archive with the database dump and the manifest',
	{tag: '@LPD-113900'},
	async () => {
		const type = process.env.UPGRADE_DATA_ARCHIVE_TYPE || 'cms';

		const upgradeDataArchiveFile = path.resolve(
			__dirname,
			'../../../../../../build/upgrade-data-archive',
			process.env.UPGRADE_DATA_ARCHIVE_PORTAL_VERSION,
			`data-archive-${type}-postgresql.zip`
		);

		const entryNames = await getZipEntryNames(upgradeDataArchiveFile);

		expect(entryNames).toContain('postgresql.sql');

		const manifestFileName = `data/upgrade-data-archive/${type}.json`;

		expect(entryNames).toContain(manifestFileName);

		const manifest = JSON.parse(
			await readFileFromZip(manifestFileName, upgradeDataArchiveFile)
		);

		expect(manifest.formatVersion).toBe(MANIFEST_FORMAT_VERSION);
		expect(Object.keys(manifest.entities)).not.toHaveLength(0);
	}
);
