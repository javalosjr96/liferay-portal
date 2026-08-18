/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import fs from 'fs';
import path from 'path';

const ANNOTATED_PROPERTY_NAMES = [
	'data.archive.type',
	'database.types',
	'portal.version',
];

const POSHI_UPGRADE_DIR = path.resolve(
	__dirname,
	'../../../../portal-web/test/functional/com/liferay/portalweb/tests',
	'coreinfrastructure/upgrades'
);

type TPoshiTest = {
	ignored: boolean;
	name: string;
	properties: Record<string, string>;
	testcase: string;
};

function parseProperties(block: string) {
	const properties: Record<string, string> = {};

	const pattern = /property\s+([\w.]+)\s*=\s*"([^"]*)"/g;

	let match = pattern.exec(block);

	while (match) {
		properties[match[1]] = match[2];

		match = pattern.exec(block);
	}

	return properties;
}

function parseTestcase(filePath: string) {
	const contents = fs.readFileSync(filePath, 'utf-8');

	const testcase = path.basename(filePath, '.testcase');

	const definitionEnd = contents.search(/^\t(?:@|test\s)/m);

	const definitionProperties = parseProperties(
		definitionEnd > 0 ? contents.slice(0, definitionEnd) : ''
	);

	const tests: TPoshiTest[] = [];

	const pattern = /^\ttest\s+(\w+)\s*\{/gm;

	const starts: {index: number; name: string}[] = [];

	let match = pattern.exec(contents);

	while (match) {
		starts.push({index: match.index, name: match[1]});

		match = pattern.exec(contents);
	}

	for (let i = 0; i < starts.length; i++) {
		const start = starts[i];

		const end = i + 1 < starts.length ? starts[i + 1].index : contents.length;

		const block = contents.slice(start.index, end);

		const preamble = contents.slice(
			i === 0 ? 0 : starts[i - 1].index,
			start.index
		);

		tests.push({
			ignored: /@ignore\s*=\s*"true"/.test(preamble),
			name: start.name,
			properties: {...definitionProperties, ...parseProperties(block)},
			testcase,
		});
	}

	return tests;
}

function toAnnotationBlock(poshiTest: TPoshiTest) {
	const annotations = ANNOTATED_PROPERTY_NAMES.filter(
		(name) => poshiTest.properties[name]
	).map(
		(name) =>
			`\t\t\t{type: '${name}', description: '${poshiTest.properties[name]}'},`
	);

	const databaseTypes = (poshiTest.properties['database.types'] || '').split(
		','
	);

	const tags = [
		`'@${poshiTest.properties['portal.version']}'`,
		`'@${poshiTest.properties['data.archive.type']}'`,
		...databaseTypes.filter(Boolean).map((type) => `'@${type}'`),
	];

	return [
		'\t{',
		'\t\tannotation: [',
		...annotations,
		'\t\t],',
		`\t\ttag: [${tags.join(', ')}],`,
		'\t},',
	].join('\n');
}

function main() {
	const filter = process.argv[2];

	const files = fs
		.readdirSync(POSHI_UPGRADE_DIR)
		.filter((file) => file.endsWith('.testcase'));

	let converted = 0;
	let ignored = 0;
	let skipped = 0;

	for (const file of files) {
		for (const poshiTest of parseTestcase(
			path.join(POSHI_UPGRADE_DIR, file)
		)) {
			if (!poshiTest.properties['portal.version']) {
				skipped++;

				continue;
			}

			if (poshiTest.ignored) {
				ignored++;

				continue;
			}

			converted++;

			if (filter && !poshiTest.name.includes(filter)) {
				continue;
			}

			process.stdout.write(
				`// ${poshiTest.testcase}.${poshiTest.name}\n${toAnnotationBlock(poshiTest)}\n`
			);
		}
	}

	process.stderr.write(
		`converted=${converted} ignored=${ignored} noVersion=${skipped}\n`
	);
}

main();
