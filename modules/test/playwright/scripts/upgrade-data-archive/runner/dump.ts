/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {execFileSync} from 'child_process';
import * as fs from 'fs';
import * as path from 'path';

export type DatabaseOptions = {
	container: string;
	name: string;
	type: string;
	user: string;
};

export function dumpDatabase(databaseOptions: DatabaseOptions, dir: string) {
	if (databaseOptions.type !== 'postgresql') {
		throw new Error(
			`Unable to dump database type "${databaseOptions.type}"`
		);
	}

	const dumpFile = path.join(dir, 'postgresql.sql');

	const fileDescriptor = fs.openSync(dumpFile, 'w');

	try {
		execFileSync(
			'docker',
			[
				'exec',
				databaseOptions.container,
				'pg_dump',
				'-U',
				databaseOptions.user,
				databaseOptions.name,
			],
			{stdio: ['ignore', fileDescriptor, 'inherit']}
		);
	}
	finally {
		fs.closeSync(fileDescriptor);
	}

	return dumpFile;
}
