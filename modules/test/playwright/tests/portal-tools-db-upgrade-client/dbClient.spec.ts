/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {test} from '@playwright/test';
import {spawn} from 'child_process';

const antDir = __dirname.toString() + '/ant/';

async function runAntTask(buildFile, taskName) {
	return new Promise((resolve, reject) => {
		const antProcess = spawn('ant', ['-f', antDir + buildFile, taskName], {
			cwd: process.cwd(),
			env: process.env,
		});

		antProcess.stdout.on('data', (data) => {
			console.log(`${data.toString()}`);
		});

		antProcess.stderr.on('data', (data) => {
			console.error(`${data.toString()}`);
		});

		antProcess.on('close', (code) => {
			if (code === 0) {
				resolve(`Ant Task "${taskName}" completed successfully.`);
			}
			else {
				reject(
					new Error(
						`Ant Task "${taskName}" failed with exit code: ${code}`
					)
				);
			}
		});
	});
}

test('CheckUpgradeClientAdditionalSettings', async () => {
	await runAntTask(
		'build-test-db-upgrade-client-playwright.xml',
		'check-upgrade-client-additional-settings'
	);
});

test('CheckUpgradeClientCustomLog', async () => {
	await runAntTask(
		'build-test-db-upgrade-client-playwright.xml',
		'check-upgrade-client-custom-log'
	);
});

test('CheckUpgradeClientGogoShell', async () => {
	await runAntTask(
		'build-test-db-upgrade-client-playwright.xml',
		'execute-upgrade-client-gogoshell'
	);
});

test('CheckUpgradeClientGogoShellCommandOutput', async () => {
	await runAntTask(
		'build-test-db-upgrade-client-playwright.xml',
		'check-upgrade-client-gogoshell-command-output'
	);
});

test('CheckUpgradeClientGogoShellHelpOutput', async () => {
	await runAntTask(
		'build-test-db-upgrade-client-playwright.xml',
		'check-upgrade-client-gogoshell-help-output'
	);
});

test('CheckUpgradeClientHelp', async () => {
	await runAntTask(
		'build-test-db-upgrade-client-playwright.xml',
		'check-upgrade-client-help'
	);
});

test('CheckUpgradeClientSecondProcess', async () => {
	await runAntTask(
		'build-test-db-upgrade-client-playwright.xml',
		'check-upgrade-client-second-process'
	);
});

test('CheckUpgradeClientShDisconnect', async () => {
	await runAntTask(
		'build-test-db-upgrade-client-playwright.xml',
		'check-upgrade-client-sh-disconnect'
	);
});

test('CheckUpgradeClientZipContents', async () => {
	await runAntTask(
		'build-test-db-upgrade-client-playwright.xml',
		'check-upgrade-client-zip-content'
	);
});

test('CheckUpgradeDebugOptions', async () => {
	await runAntTask(
		'build-test-db-upgrade-client-playwright.xml',
		'check-upgrade-debug-options'
	);
});

test('CheckUpgradePropertiesAppDBSet', async () => {
	await runAntTask(
		'build-test-db-upgrade-client-playwright.xml',
		'check-upgrade-properties-app-db-set'
	);
});

test('CheckUpgradePropertiesAppExtSet', async () => {
	await runAntTask(
		'build-test-db-upgrade-client-playwright.xml',
		'check-upgrade-properties-app-ext-set'
	);
});

test('CheckUpgradePropertiesDBExtSet', async () => {
	await runAntTask(
		'build-test-db-upgrade-client-playwright.xml',
		'check-upgrade-properties-db-ext-set'
	);
});
test.properties
test('CheckUpgradePropertiesNoneSet', async () => {
	await runAntTask(
		'build-test-db-upgrade-client-playwright.xml',
		'check-upgrade-properties-none-set'
	);
});

test('HomeExtPropertiesReturnsError', async () => {
	await runAntTask(
		'build-test-db-upgrade-client-playwright.xml',
		'check-upgrade-client-home-ext-properties'
	);
});
