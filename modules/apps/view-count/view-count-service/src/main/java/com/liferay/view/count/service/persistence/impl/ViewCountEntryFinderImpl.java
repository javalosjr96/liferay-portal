/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.view.count.service.persistence.impl;

import com.liferay.document.library.kernel.service.DLAppHelperLocalService;
import com.liferay.portal.kernel.bean.BeanReference;
import com.liferay.portal.kernel.dao.orm.EntityCache;
import com.liferay.portal.kernel.dao.orm.LockMode;
import com.liferay.portal.kernel.dao.orm.Session;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.repository.Repository;
import com.liferay.portal.kernel.repository.RepositoryProvider;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.view.count.model.ViewCountEntry;
import com.liferay.view.count.model.impl.ViewCountEntryImpl;
import com.liferay.view.count.service.persistence.ViewCountEntryFinder;
import com.liferay.view.count.service.persistence.ViewCountEntryPK;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Preston Crary
 */
@Component(service = ViewCountEntryFinder.class)
public class ViewCountEntryFinderImpl
	extends ViewCountEntryFinderBaseImpl implements ViewCountEntryFinder {

	@Override
	public void incrementViewCount(
		long companyId, long classNameId, long classPK, int increment) {

		Session session = null;

		try {
			session = openSession();

			ViewCountEntryPK viewCountEntryPK = new ViewCountEntryPK(
				companyId, classNameId, classPK);

			ViewCountEntry viewCountEntry = (ViewCountEntry)session.get(
				ViewCountEntryImpl.class, viewCountEntryPK, LockMode.UPGRADE);

			if (viewCountEntry == null) {
				viewCountEntry = new ViewCountEntryImpl();

				viewCountEntry.setPrimaryKey(viewCountEntryPK);

				viewCountEntry.setViewCount(increment);

				session.save(viewCountEntry);

				session.flush();
			}
			else {
				viewCountEntry.setViewCount(
					viewCountEntry.getViewCount() + increment);

				session.saveOrUpdate(viewCountEntry);
			}

			_entityCache.putResult(
				ViewCountEntryImpl.class, viewCountEntryPK, viewCountEntry);
		}
		finally {
			closeSession(session);
		}

		Repository repo = null;

		PermissionChecker permissionChecker;

		try {

			User user = _userLocalService.getUserById(20127);

			permissionChecker = PermissionCheckerFactoryUtil.create(user);

			 repo = repositoryProvider.getFileEntryRepository(
				 classPK);

			PermissionThreadLocal.setPermissionChecker(permissionChecker);

			FileEntry fileEntry = null;

			try {
				fileEntry = repo.getFileEntry(classPK);

				FileVersion fileVersion = fileEntry.getLatestFileVersion();

				_dlAppHelperLocalService.updateAsset(
					user.getUserId(), fileEntry, fileVersion,
					fileVersion.getFileVersionId());
			}
			catch(Exception e){
				return;
			}


		}
		catch (PortalException e) {

		}

	}

	@Reference
	private EntityCache _entityCache;

	@Reference
	private DLAppHelperLocalService _dlAppHelperLocalService;

	@Reference
	protected RepositoryProvider repositoryProvider;

	@Reference
	private UserLocalService _userLocalService;

}