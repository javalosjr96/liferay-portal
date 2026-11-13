/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import * as fs from 'fs';
import * as path from 'path';

export const MANIFEST_FORMAT_VERSION = 1;

export type ManifestEntity = {
	[key: string]: unknown;
	externalReferenceCode: string;
};

export type ManifestData = {
	entities: {[kind: string]: ManifestEntity[]};
	formatVersion: number;
	generatedAt: string;
	portalBuildNumber: number;
	type: string;
};

export class Manifest {
	private _data: ManifestData;

	constructor(type: string, portalBuildNumber: number) {
		this._data = {
			entities: {},
			formatVersion: MANIFEST_FORMAT_VERSION,
			generatedAt: new Date().toISOString(),
			portalBuildNumber,
			type,
		};
	}

	add(kind: string, entity: ManifestEntity) {
		if (!entity.externalReferenceCode) {
			throw new Error(
				`Unable to record a ${kind} without an external reference code`
			);
		}

		const entities = this._data.entities[kind] || [];

		entities.push(entity);

		this._data.entities[kind] = entities;
	}

	getData(): ManifestData {
		return this._data;
	}
}

export function getManifestDir(liferayHome: string) {
	return path.join(liferayHome, 'data', 'upgrade-data-archive');
}

export function getManifestEntities(
	manifestData: ManifestData,
	kind: string
): ManifestEntity[] {
	return manifestData.entities[kind] || [];
}

export function readManifests(liferayHome: string): ManifestData[] {
	const manifestDir = getManifestDir(liferayHome);

	if (!fs.existsSync(manifestDir)) {
		return [];
	}

	return fs
		.readdirSync(manifestDir)
		.filter((fileName) => fileName.endsWith('.json'))
		.sort()
		.map((fileName) => {
			const manifestData: ManifestData = JSON.parse(
				fs.readFileSync(path.join(manifestDir, fileName), 'utf8')
			);

			if (
				!Number.isInteger(manifestData.formatVersion) ||
				manifestData.formatVersion > MANIFEST_FORMAT_VERSION
			) {
				throw new Error(
					`Unable to read manifest "${fileName}" with format version ${manifestData.formatVersion}`
				);
			}

			return manifestData;
		});
}

export function writeManifest(liferayHome: string, manifest: Manifest) {
	const manifestData = manifest.getData();

	const manifestDir = getManifestDir(liferayHome);

	fs.mkdirSync(manifestDir, {recursive: true});

	const manifestFile = path.join(manifestDir, `${manifestData.type}.json`);

	fs.writeFileSync(manifestFile, JSON.stringify(manifestData, null, '\t'));

	return manifestFile;
}
