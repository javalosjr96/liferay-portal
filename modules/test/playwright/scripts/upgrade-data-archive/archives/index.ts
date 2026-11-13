/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {UpgradeDataArchive} from '../runner/upgradeDataArchive';
import cms from './cms';

const upgradeDataArchives: UpgradeDataArchive[] = [cms];

export function getUpgradeDataArchive(name: string): UpgradeDataArchive {
	const upgradeDataArchive = upgradeDataArchives.find(
		(curUpgradeDataArchive) => curUpgradeDataArchive.name === name
	);

	if (!upgradeDataArchive) {
		throw new Error(`Unable to find upgrade data archive "${name}"`);
	}

	return upgradeDataArchive;
}
