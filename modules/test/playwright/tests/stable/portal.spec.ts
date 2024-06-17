/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, test} from '@playwright/test';

import {execSync, spawn} from 'child_process';

import fs from 'fs';

const scriptDir = process.cwd()+ "/tests/stable/env/test.sh";

const antDir = process.cwd()+ "/tests/stable/env/ant/";

const liferayRoot = "/home/jorgeavalos/Liferay/liferay-portal-ce-1/liferay-portal/";

test('title is Home - Liferay DXP', async ({page}) => {

	const properties = {
		'app.server.types' : 'tomcat',
		'ci.retries.disabled' : 'true',
		'data.archive.type' : 'data-archive-portal',
		'database.types' : 'postgresql',
		'database.upgrade.enabled' : 'true',
		'portal.release' : 'true',
		'portal.upstream' : 'true',
		'portal.version' : '6.2.10.21',
		'skip.start.app.server' : 'true',
		'test.liferay.virtual.instance' : 'false',
		'test.run.type' : 'single',
		'testcase.url' : 'http://www.example.com'
	}

	const propertiesContent = Object.entries(properties).map(([key, value]) => `${key}=${value}`).join('\n');

	const { spawn } = require('child_process');

	const scriptProcess = spawn(scriptDir, ['clean-database-upgrade-client']);

	scriptProcess.stdout.on('data', (data) => {
		console.log(`Script Output: ${data}`);
	});

	scriptProcess.stderr.on('data', (data) => {
		console.error(`Script Error: ${data}`);
	});

	await new Promise(resolve => scriptProcess.on('close', resolve));

	const antProcess = spawn('ant', ['-f', antDir + 'build-test-db-upgrade-client.xml', 'check-upgrade-client-additional-settings']);


	antProcess.stdout.on('data', (data) => {
		console.log(`${data.toString()}`);
	});

	antProcess.stderr.on('data', (data) => {
		console.error(`${data.toString()}`);
	});

	await new Promise(resolve => antProcess.on('close', resolve));

	console.log('Playwright, script, and Ant build completed!');

});

test('title is Home 2 - Liferay DXP', async ({page}) => {

	const properties = {
		'app.server.types' : 'tomcat',
		'ci.retries.disabled' : 'true',
		'data.archive.type' : 'data-archive-portal',
		'database.type' : 'mysql',
		'database.upgrade.enabled' : 'true',
		'portal.release' : 'true',
		'portal.upstream' : 'true',
		'portal.version' : '6.2.10.21',
		'skip.start.app.server' : 'true',
		'test.liferay.virtual.instance' : 'false',
		'test.run.type' : 'single',
		'testcase.url' : 'http://www.example.com',
		'database.docker.image' : 'mysql:5.7.25'
	};

	const propertiesContent = Object.entries(properties).map(([key, value]) => `${key}=${value}`).join('\n');

	Object.entries(properties).forEach(([key, value]) => process.env[key] = value);

	const antProcess = spawn('ant', ['-f', liferayRoot + 'build-test.xml', 'rebuild-database']);

	antProcess.stdout.on('data', (data) => {
		console.log(`${data.toString()}`);
	});

	antProcess.stderr.on('data', (data) => {
		console.error(`${data.toString()}`);
	});

	await new Promise(resolve => antProcess.on('close', resolve));

	console.log('Done');

});
