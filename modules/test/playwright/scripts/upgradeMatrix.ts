/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {execFileSync} from 'child_process';
import path from 'path';

type TAnnotation = {
	description?: string;
	type: string;
};

type TSpec = {
	file: string;
	fullTitle?: string;
	tags?: string[];
	tests: {annotations?: TAnnotation[]}[];
	title: string;
};

type TSuite = {
	specs?: TSpec[];
	suites?: TSuite[];
	title?: string;
};

type TSlice = {
	dataArchiveType: string;
	databaseType: string;
	grep: string;
	portalVersion: string;
	titles: string[];
};

const PLAYWRIGHT_DIR = path.resolve(__dirname, '..');

function annotationValue(spec: TSpec, type: string) {
	for (const test of spec.tests) {
		for (const annotation of test.annotations || []) {
			if (annotation.type === type) {
				return annotation.description || '';
			}
		}
	}

	return '';
}

function collectSpecs(
	suite: TSuite,
	titlePath: string[] = [],
	specs: TSpec[] = []
) {
	for (const spec of suite.specs || []) {
		specs.push({...spec, fullTitle: [...titlePath, spec.title].join(' ')});
	}

	for (const child of suite.suites || []) {
		collectSpecs(
			child,
			child.title ? [...titlePath, child.title] : titlePath,
			specs
		);
	}

	return specs;
}

function escapeRegExp(value: string) {
	return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function listTests(projectName: string) {
	const stdout = execFileSync(
		process.execPath,
		[
			path.join(PLAYWRIGHT_DIR, 'node_modules', '.bin', 'playwright'),
			'test',
			'--list',
			'--reporter=json',
			`--project=${projectName}`,
		],
		{cwd: PLAYWRIGHT_DIR, encoding: 'utf-8', maxBuffer: 64 * 1024 * 1024}
	);

	return JSON.parse(stdout);
}

function buildSlices(specs: TSpec[], databaseType: string) {
	const slices = new Map<string, TSlice>();

	for (const spec of specs) {
		const dataArchiveType = annotationValue(spec, 'data.archive.type');
		const portalVersion = annotationValue(spec, 'portal.version');

		if (!dataArchiveType || !portalVersion) {
			continue;
		}

		const databaseTypes = annotationValue(spec, 'database.types').split(',');

		if (!databaseTypes.includes(databaseType)) {
			continue;
		}

		const key = `${portalVersion}|${dataArchiveType}`;

		let slice = slices.get(key);

		if (!slice) {
			slice = {
				dataArchiveType,
				databaseType,
				grep: '',
				portalVersion,
				titles: [],
			};

			slices.set(key, slice);
		}

		slice.titles.push(spec.fullTitle || spec.title);
	}

	for (const slice of slices.values()) {
		slice.titles.sort();

		slice.grep = [
			slice.portalVersion,
			slice.dataArchiveType,
			slice.databaseType,
		]
			.map((value) => `(?=.*@${escapeRegExp(value)}(?![\\w.-]))`)
			.join('');
	}

	return [...slices.values()].sort((a, b) =>
		`${a.portalVersion}${a.dataArchiveType}`.localeCompare(
			`${b.portalVersion}${b.dataArchiveType}`
		)
	);
}

function main() {
	const projectName = process.argv[2];

	const databaseTypeArgument = process.argv.find((argument) =>
		argument.startsWith('--database-type=')
	);

	const databaseType =
		(databaseTypeArgument &&
			databaseTypeArgument.slice('--database-type='.length)) ||
		process.env.DATABASE_TYPE;

	if (!projectName || !databaseType) {
		process.stderr.write(
			'Usage: upgradeMatrix.ts <playwrightProjectName> ' +
				'--database-type=<databaseType> [--json]\n'
		);

		process.exit(1);
	}

	const specs: TSpec[] = [];

	for (const suite of listTests(projectName).suites) {
		collectSpecs(suite, [], specs);
	}
	const slices = buildSlices(specs, databaseType);

	if (!slices.length) {
		process.stderr.write(
			`Unable to derive an upgrade matrix for "${projectName}" ` +
				`on "${databaseType}"\n`
		);

		process.exit(1);
	}

	if (process.argv.includes('--json')) {
		process.stdout.write(`${JSON.stringify(slices, null, 2)}\n`);

		return;
	}

	for (const slice of slices) {
		process.stdout.write(
			[
				`PORTAL_VERSION=${slice.portalVersion}`,
				`DATA_ARCHIVE_TYPE=${slice.dataArchiveType}`,
				`DATABASE_TYPE=${slice.databaseType}`,
				`PLAYWRIGHT_GREP=${JSON.stringify(slice.grep)}`,
				`TESTS=${slice.titles.length}`,
			].join(' ') + '\n'
		);
	}
}

main();
