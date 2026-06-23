/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import * as fs from 'fs';
import * as path from 'path';

export const STATE_FILE = path.join(
	__dirname,
	'../../test-results/db-partition-state.json'
);

export function readState(): {partitionCompanyId: number} {
	return JSON.parse(fs.readFileSync(STATE_FILE, 'utf-8'));
}
