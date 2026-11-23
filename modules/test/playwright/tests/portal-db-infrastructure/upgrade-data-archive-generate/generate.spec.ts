/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, test} from '@playwright/test';
import {existsSync, readdirSync} from 'fs';
import path from 'path';

import {getZipEntryNames, readFileFromZip} from '../../../utils/zip';

const UPGRADE_DATA_ARCHIVE_DIR = path.resolve(
	__dirname,
	'../../../../../../build/upgrade-data-archive'
);

function getUpgradeDataArchiveFiles() {
	if (!existsSync(UPGRADE_DATA_ARCHIVE_DIR)) {
		return [];
	}

	return readdirSync(UPGRADE_DATA_ARCHIVE_DIR, {withFileTypes: true})
		.filter((dirent) => dirent.isDirectory())
		.flatMap((dirent) => {
			const versionDir = path.join(UPGRADE_DATA_ARCHIVE_DIR, dirent.name);

			return readdirSync(versionDir)
				.filter((fileName) =>
					/^data-archive-.+-postgresql\.zip$/.test(fileName)
				)
				.map((fileName) => path.join(versionDir, fileName));
		});
}

test(
	'generates an upgrade data archive with the database dump and the manifest',
	{tag: '@LPD-113900'},
	async () => {
		const upgradeDataArchiveFiles = getUpgradeDataArchiveFiles();

		test.skip(
			!upgradeDataArchiveFiles.length,
			'No upgrade data archive was generated'
		);

		for (const upgradeDataArchiveFile of upgradeDataArchiveFiles) {
			const [, type] = path
				.basename(upgradeDataArchiveFile)
				.match(/^data-archive-(.+)-postgresql\.zip$/);

			const entryNames = await getZipEntryNames(upgradeDataArchiveFile);

			expect(entryNames).toContain('postgresql.sql');

			const manifestFileName = `data/upgrade-data-archive/${type}.json`;

			expect(entryNames).toContain(manifestFileName);

			const manifest = JSON.parse(
				await readFileFromZip(manifestFileName, upgradeDataArchiveFile)
			);

			expect(manifest.type).toBe(type);
		}
	}
);
