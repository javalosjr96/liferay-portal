/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {existsSync, readFileSync} from 'fs';
import * as path from 'path';

export type UpgradeReportTableRows = {
	finalRows: number;
	initialRows: number;
	tableName: string;
};

/**
 * Reads the "Table Name / Initial Rows / Final Rows" section that the upgrade
 * client writes to reports/upgrade_report.txt. A table missing on one side is
 * printed as "-" and returned as -1. Returns null when the upgrade client wrote
 * no report.
 */
export function getUpgradeReportTableRows(
	liferayHome: string
): UpgradeReportTableRows[] | null {
	const upgradeReportFile = path.join(
		liferayHome,
		'tools',
		'portal-tools-db-upgrade-client',
		'reports',
		'upgrade_report.txt'
	);

	if (!existsSync(upgradeReportFile)) {
		return null;
	}

	const lines = readFileSync(upgradeReportFile, 'utf8').split('\n');

	const headerIndex = lines.findIndex((line) =>
		/^Table Name\s+Initial Rows\s+Final Rows\s*$/.test(line)
	);

	if (headerIndex < 0) {
		throw new Error('Unable to find the table rows in the upgrade report');
	}

	const tableRows: UpgradeReportTableRows[] = [];

	for (const line of lines.slice(headerIndex + 2)) {
		const match = line.match(/^(\S+)\s+(\d+|-)\s+(\d+|-)\s*$/);

		if (!match) {
			break;
		}

		tableRows.push({
			finalRows: match[3] === '-' ? -1 : Number(match[3]),
			initialRows: match[2] === '-' ? -1 : Number(match[2]),
			tableName: match[1],
		});
	}

	return tableRows;
}
