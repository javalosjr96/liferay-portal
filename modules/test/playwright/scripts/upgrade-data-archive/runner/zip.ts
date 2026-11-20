/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import {zip} from 'zip-a-folder';

import {DatabaseOptions, dumpDatabase} from './dump';

export async function createDataArchive(
	databaseOptions: DatabaseOptions,
	liferayHome: string,
	outDir: string,
	type: string
) {
	const stagingDir = fs.mkdtempSync(
		path.join(os.tmpdir(), 'upgrade-data-archive-')
	);

	try {
		dumpDatabase(databaseOptions, stagingDir);

		const dataDir = path.join(liferayHome, 'data');

		fs.cpSync(dataDir, path.join(stagingDir, 'data'), {
			filter: (source) =>
				!path
					.relative(dataDir, source)
					.split(path.sep)[0]
					.startsWith('elasticsearch'),
			recursive: true,
		});

		fs.mkdirSync(outDir, {recursive: true});

		const archiveFile = path.join(
			outDir,
			`data-archive-${type}-${databaseOptions.type}.zip`
		);

		await zip(stagingDir, archiveFile);

		return archiveFile;
	}
	finally {
		fs.rmSync(stagingDir, {force: true, recursive: true});
	}
}
