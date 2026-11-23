/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.upgrade.internal.registry;

import com.liferay.petra.lang.HashUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.upgrade.UpgradeStep;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Miguel Pastor
 * @author Carlos Sierra Andrés
 */
public class UpgradeInfo {

	public UpgradeInfo(
		String fromSchemaVersionString, String toSchemaVersionString,
		List<UpgradeStep> upgradeSteps) {

		_fromSchemaVersionString = fromSchemaVersionString;
		_toSchemaVersionString = toSchemaVersionString;
		_upgradeSteps = Collections.unmodifiableList(upgradeSteps);
	}

	public UpgradeInfo(
		String fromSchemaVersionString, String toSchemaVersionString,
		UpgradeStep upgradeStep) {

		this(
			fromSchemaVersionString, toSchemaVersionString,
			Collections.singletonList(upgradeStep));
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!(object instanceof UpgradeInfo)) {
			return false;
		}

		UpgradeInfo upgradeInfo = (UpgradeInfo)object;

		if (Objects.equals(
				_fromSchemaVersionString,
				upgradeInfo._fromSchemaVersionString) &&
			Objects.equals(
				_toSchemaVersionString, upgradeInfo._toSchemaVersionString) &&
			Objects.equals(_upgradeSteps, upgradeInfo._upgradeSteps)) {

			return true;
		}

		return false;
	}

	public String getFromSchemaVersionString() {
		return _fromSchemaVersionString;
	}

	public String getToSchemaVersionString() {
		return _toSchemaVersionString;
	}

	public UpgradeStep getUpgradeStep() {
		if (_upgradeSteps.size() == 1) {
			return _upgradeSteps.get(0);
		}

		return () -> {
			for (UpgradeStep upgradeStep : _upgradeSteps) {
				upgradeStep.upgrade();
			}
		};
	}

	public List<UpgradeStep> getUpgradeSteps() {
		return _upgradeSteps;
	}

	@Override
	public int hashCode() {
		int hash = HashUtil.hash(0, _fromSchemaVersionString);

		hash = HashUtil.hash(hash, _toSchemaVersionString);
		hash = HashUtil.hash(hash, _upgradeSteps);

		return hash;
	}

	@Override
	public String toString() {
		return StringBundler.concat(
			"{fromSchemaVersionString=", _fromSchemaVersionString,
			", toSchemaVersionString=", _toSchemaVersionString,
			", upgradeSteps=", _upgradeSteps, "}");
	}

	private final String _fromSchemaVersionString;
	private final String _toSchemaVersionString;
	private final List<UpgradeStep> _upgradeSteps;

}