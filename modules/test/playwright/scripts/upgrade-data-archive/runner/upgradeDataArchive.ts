/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ApiHelpers} from '../../../helpers/ApiHelpers';
import {Manifest, ManifestData} from './manifest';

export type UpgradeDataArchive = {
	featureFlags: string[];
	name: string;
	populate: (apiHelpers: ApiHelpers, manifest: Manifest) => Promise<void>;
	verify: (apiHelpers: ApiHelpers, manifestData: ManifestData) => Promise<void>;
};
