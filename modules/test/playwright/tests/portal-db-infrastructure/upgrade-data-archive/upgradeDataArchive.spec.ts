/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, mergeTests} from '@playwright/test';
import {existsSync, readFileSync} from 'fs';
import * as path from 'path';

import {apiHelpersTest} from '../../../fixtures/apiHelpersTest';
import {loginTest} from '../../../fixtures/loginTest';
import {getUpgradeDataArchive} from '../../../scripts/upgrade-data-archive/archives';
import {readManifests} from '../../../scripts/upgrade-data-archive/runner/manifest';
import {getUpgradeReportTableRows} from '../utils/getUpgradeReportTableRows';

const test = mergeTests(apiHelpersTest, loginTest());

const ROW_LOSS_ALLOWED_TABLE_NAMES = new Set<string>([]);

function getLiferayHome() {
	const liferayHome = process.env.LIFERAY_HOME;

	if (!liferayHome) {
		throw new Error('Set the environment variable LIFERAY_HOME');
	}

	return liferayHome;
}

test(
	'Every table keeps its rows through the upgrade',
	{tag: '@LPD-107280'},
	async () => {
		const liferayHome = getLiferayHome();

		const tableRows = getUpgradeReportTableRows(liferayHome);

		if (!tableRows) {
			const upgradeLogFile = path.join(
				liferayHome,
				'tools',
				'portal-tools-db-upgrade-client',
				'logs',
				'upgrade.log'
			);

			test.skip(
				existsSync(upgradeLogFile) &&
					readFileSync(upgradeLogFile, 'utf8').includes(
					'Upgrade report was not generated because no upgrade processes were executed'
				),
				'The archive needed no upgrade processes, so there is no report to compare'
			);

			throw new Error('Unable to find the upgrade report');
		}

		expect(tableRows.length).toBeGreaterThan(0);

		const lostRowsTableNames = tableRows
			.filter(
				({finalRows, initialRows, tableName}) =>
					initialRows > 0 &&
					finalRows < initialRows &&
					!ROW_LOSS_ALLOWED_TABLE_NAMES.has(tableName)
			)
			.map(
				({finalRows, initialRows, tableName}) =>
					`${tableName} ${initialRows} -> ${finalRows}`
			);

		expect(lostRowsTableNames).toEqual([]);
	}
);

test(
	'Every entity in the archive manifest survives the upgrade',
	{tag: '@LPD-107280'},
	async ({apiHelpers}) => {
		const manifests = readManifests(getLiferayHome());

		expect(manifests.length).toBeGreaterThan(0);

		for (const manifestData of manifests) {
			await test.step(`Verify the ${manifestData.type} archive`, async () => {
				const upgradeDataArchive = getUpgradeDataArchive(
					manifestData.type
				);

				await upgradeDataArchive.verify(apiHelpers, manifestData);
			});
		}
	}
);
