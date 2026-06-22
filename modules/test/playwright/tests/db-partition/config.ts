/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

export const dbPartitionPhase1Config = {
	name: 'db-partition.phase1',
	testDir: 'tests/db-partition/phase1',
	use: {
		testIdAttribute: 'data-qa-id',
	},
};

export const dbPartitionPhase2Config = {
	name: 'db-partition.phase2',
	testDir: 'tests/db-partition/phase2',
	use: {
		testIdAttribute: 'data-qa-id',
	},
};
