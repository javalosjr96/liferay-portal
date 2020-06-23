/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Liferay Enterprise
 * Subscription License ("License"). You may not use this file except in
 * compliance with the License. You can obtain a copy of the License by
 * contacting Liferay, Inc. See the License for the specific language governing
 * permissions and limitations under the License, including but not limited to
 * distribution rights of the Software.
 *
 *
 *
 */

package com.liferay.osb.loop.web.internal.hook.security.permission;

import com.liferay.osb.loop.asset.entry.set.model.AssetEntrySet;
import com.liferay.osb.loop.asset.entry.set.service.AssetEntrySetLocalService;
import com.liferay.osb.loop.asset.entry.set.util.AssetEntrySetParticipantInfo;
import com.liferay.osb.loop.asset.sharing.model.AssetSharingEntry;
import com.liferay.osb.loop.asset.sharing.service.AssetSharingEntryLocalService;
import com.liferay.osb.loop.model.LoopPerson;
import com.liferay.osb.loop.service.LoopPersonLocalService;
import com.liferay.osb.loop.web.internal.util.LoopAssetEntrySetUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.BaseModelPermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.util.Portal;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * @author Timothy Bell
 */
@Component(
	property = "model.class.name=com.liferay.osb.loop.asset.entry.set.model.AssetEntrySet",
	service = BaseModelPermissionChecker.class
)
public class AssetEntrySetPermission implements BaseModelPermissionChecker {

	@Override
	public void checkBaseModel(
			PermissionChecker permissionChecker, long groupId, long primaryKey,
			String actionId)
		throws PortalException {

		if (!hasPermission(permissionChecker, primaryKey)) {
			throw new PrincipalException.MustHavePermission(
				permissionChecker, AssetEntrySet.class.getName(), primaryKey,
				actionId);
		}
	}

	protected boolean hasPermission(
			PermissionChecker permissionChecker, long primaryKey)
		throws PortalException {

		AssetEntrySet assetEntrySet =
			_assetEntrySetLocalService.getAssetEntrySet(primaryKey);

		if (assetEntrySet.isPrivateAssetEntrySet() &&
			!hasSharingPermission(
				permissionChecker.getUserId(),
				assetEntrySet.getAssetEntrySetId())) {

			return false;
		}

		return true;
	}

	protected boolean hasSharingPermission(long userId, long assetEntrySetId) {
		try {
			LoopPerson loopPerson =
				_loopPersonLocalService.getLoopPersonByPersonUserId(userId);

			List<AssetSharingEntry> assetSharingEntries =
				_assetSharingEntryLocalService.getAssetSharingEntries(
					_portal.getClassNameId(AssetEntrySet.class),
					LoopAssetEntrySetUtil.getRootAssetEntrySetId(
						assetEntrySetId));

			for (AssetSharingEntry assetSharingEntry : assetSharingEntries) {
				if (_assetEntrySetParticipantInfo.isMember(
						_portal.getClassNameId(LoopPerson.class),
						loopPerson.getLoopPersonId(),
						assetSharingEntry.getSharedToClassNameId(),
						assetSharingEntry.getSharedToClassPK())) {

					return true;
				}
			}
		}
		catch (Exception e) {
		}

		return false;
	}

	@Reference
	private AssetEntrySetLocalService _assetEntrySetLocalService;

	@Reference(
		policy = ReferencePolicy.DYNAMIC,
		policyOption = ReferencePolicyOption.GREEDY
	)
	private volatile AssetEntrySetParticipantInfo _assetEntrySetParticipantInfo;

	@Reference
	private AssetSharingEntryLocalService _assetSharingEntryLocalService;

	@Reference
	private LoopPersonLocalService _loopPersonLocalService;

	@Reference
	private Portal _portal;

}