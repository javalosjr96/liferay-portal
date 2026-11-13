/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

/* eslint-disable no-console */

import * as path from 'path';

import {ApiHelpers} from '../../helpers/ApiHelpers';
import {liferayConfig} from '../../liferay.config';
import {getUpgradeDataArchive} from './archives';
import {enableFeatureFlags} from './runner/featureFlags';
import {Manifest, writeManifest} from './runner/manifest';
import {createDataArchive} from './runner/zip';

const USAGE =
	'Usage: run.ts --type <type> --database-container <container> --database-name <name> [--database-type postgresql] [--database-user root] [--liferay-home <dir>] [--out <dir>]';

function getOptions(args: string[]) {
	const options: {[key: string]: string} = {
		'database-type': 'postgresql',
		'database-user': 'root',
		'liferay-home': process.env.LIFERAY_HOME,
		'out': '.',
	};

	for (let i = 0; i < args.length; i += 2) {
		const name = args[i];
		const value = args[i + 1];

		if (!name.startsWith('--') || value === undefined) {
			throw new Error(USAGE);
		}

		options[name.substring(2)] = value;
	}

	for (const name of [
		'database-container',
		'database-name',
		'liferay-home',
		'type',
	]) {
		if (!options[name]) {
			throw new Error(USAGE);
		}
	}

	return options;
}

async function main() {
	const options = getOptions(process.argv.slice(2));

	const upgradeDataArchive = getUpgradeDataArchive(options.type);

	const apiHelpers = await ApiHelpers.createWithoutBrowser();

	try {
		await enableFeatureFlags(
			apiHelpers.getAuthorizationHeader(),
			liferayConfig.environment.baseUrl,
			upgradeDataArchive.featureFlags
		);

		const portalBuildNumber = await apiHelpers.get(
			`${liferayConfig.environment.baseUrl}/api/jsonws/portal/get-build-number`,
			true
		);

		const manifest = new Manifest(
			upgradeDataArchive.name,
			portalBuildNumber
		);

		await upgradeDataArchive.populate(apiHelpers, manifest);

		const liferayHome = path.resolve(options['liferay-home']);

		console.log(`Wrote ${writeManifest(liferayHome, manifest)}`);

		const archiveFile = await createDataArchive(
			{
				container: options['database-container'],
				name: options['database-name'],
				type: options['database-type'],
				user: options['database-user'],
			},
			liferayHome,
			path.resolve(options.out),
			upgradeDataArchive.name
		);

		console.log(`Wrote ${archiveFile}`);
	}
	finally {
		await apiHelpers.dispose();
	}
}

main().catch((error) => {
	console.error(error);

	process.exit(1);
});
