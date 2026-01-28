/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.store.db;

import com.liferay.document.library.content.exception.NoSuchContentException;
import com.liferay.document.library.content.model.DLContent;
import com.liferay.document.library.content.service.DLContentLocalService;
import com.liferay.document.library.kernel.exception.NoSuchFileException;
import com.liferay.document.library.kernel.store.Store;
import com.liferay.document.library.kernel.util.DLUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.dao.orm.ActionableDynamicQuery;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.instance.PortalInstancePool;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.PropsValues;

import java.io.InputStream;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Shuyang Zhou
 * @author Tina Tian
 */
@Component(
	property = {
		"ct.aware=true", "store.type=com.liferay.portal.store.db.DBStore"
	},
	service = Store.class
)
public class DBStore implements Store {

	@Override
	public void addFile(
		long companyId, long repositoryId, String fileName, String versionLabel,
		InputStream inputStream) {

		_dlContentLocalService.addContent(
			companyId, repositoryId, fileName, versionLabel, inputStream);
	}

	@Override
	public void deleteDirectory(long companyId) throws PortalException {
		if (PropsValues.DATABASE_PARTITION_ENABLED &&
			!ArrayUtil.contains(
				PortalInstancePool.getCompanyIds(), companyId)) {

			return;
		}

		ActionableDynamicQuery actionableDynamicQuery =
			_dlContentLocalService.getActionableDynamicQuery();

		actionableDynamicQuery.setAddCriteriaMethod(
			dynamicQuery -> dynamicQuery.add(
				RestrictionsFactoryUtil.eq("companyId", companyId)));

		actionableDynamicQuery.setPerformActionMethod(
			(DLContent dlContent) -> _dlContentLocalService.deleteDLContent(
				dlContent));

		actionableDynamicQuery.performActions();
	}

	@Override
	public void deleteDirectory(
		long companyId, long repositoryId, String dirName) {

		_dlContentLocalService.deleteContentsByDirectory(
			companyId, repositoryId, dirName);
	}

	@Override
	public void deleteFile(
		long companyId, long repositoryId, String fileName,
		String versionLabel) {

		_dlContentLocalService.deleteContent(
			companyId, repositoryId, fileName, versionLabel);
	}

	@Override
	public long[] getCompanyIds() throws PortalException {
		Set<Long> companyIdsSet = new HashSet<>();

		try (Connection connection = DataAccess.getConnection();
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(
				"select distinct companyId from DLContent where companyId > " +
					"0")) {

			while (resultSet.next()) {
				companyIdsSet.add(resultSet.getLong("companyId"));
			}
		}
		catch (SQLException sqlException) {
			throw new PortalException(sqlException);
		}

		long[] companyIds = new long[companyIdsSet.size()];
		int index = 0;

		for (Long id : companyIdsSet) {
			companyIds[index++] = id;
		}

		Arrays.sort(companyIds);

		return companyIds;
	}

	@Override
	public InputStream getFileAsStream(
			long companyId, long repositoryId, String fileName,
			String versionLabel)
		throws NoSuchFileException {

		try {
			DLContent dlContent = _dlContentLocalService.getContent(
				companyId, repositoryId, fileName, versionLabel);

			return _dlContentLocalService.openDataInputStream(
				dlContent.getContentId());
		}
		catch (NoSuchContentException noSuchContentException) {
			throw new NoSuchFileException(
				companyId, repositoryId, fileName, versionLabel,
				noSuchContentException);
		}
	}

	@Override
	public String[] getFileNames(
		long companyId, long repositoryId, String dirName) {

		List<DLContent> dlContents =
			_dlContentLocalService.getContentsByDirectory(
				companyId, repositoryId, dirName);

		String[] fileNames = new String[dlContents.size()];

		for (int i = 0; i < dlContents.size(); i++) {
			DLContent dlContent = dlContents.get(i);

			fileNames[i] = dlContent.getPath();
		}

		return fileNames;
	}

	@Override
	public long getFileSize(
			long companyId, long repositoryId, String fileName,
			String versionLabel)
		throws NoSuchFileException {

		DLContent dlContent = null;

		try {
			dlContent = _dlContentLocalService.getContent(
				companyId, repositoryId, fileName, versionLabel);
		}
		catch (NoSuchContentException noSuchContentException) {
			throw new NoSuchFileException(
				companyId, repositoryId, fileName, noSuchContentException);
		}

		return dlContent.getSize();
	}

	@Override
	public String[] getFileVersions(
		long companyId, long repositoryId, String fileName) {

		List<DLContent> dlContents = _dlContentLocalService.getContents(
			companyId, repositoryId, fileName);

		if (dlContents.isEmpty()) {
			return StringPool.EMPTY_ARRAY;
		}

		String[] versions = new String[dlContents.size()];

		for (int i = 0; i < dlContents.size(); i++) {
			DLContent dlContent = dlContents.get(i);

			versions[i] = dlContent.getVersion();
		}

		Arrays.sort(versions, DLUtil::compareVersions);

		return versions;
	}

	@Override
	public boolean hasFile(
		long companyId, long repositoryId, String fileName,
		String versionLabel) {

		return _dlContentLocalService.hasContent(
			companyId, repositoryId, fileName, versionLabel);
	}

	@Reference
	private DLContentLocalService _dlContentLocalService;

}