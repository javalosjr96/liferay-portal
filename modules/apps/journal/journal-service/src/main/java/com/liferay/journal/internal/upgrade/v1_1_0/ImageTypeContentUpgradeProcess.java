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

package com.liferay.journal.internal.upgrade.v1_1_0;

import com.liferay.journal.constants.JournalConstants;
import com.liferay.journal.internal.upgrade.helper.JournalArticleImageUpgradeHelper;
import com.liferay.journal.model.JournalArticle;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Image;
import com.liferay.portal.kernel.portletfilerepository.PortletFileRepository;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.service.ImageLocalService;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.LoggingTimer;
import com.liferay.portal.kernel.util.MimeTypesUtil;
import com.liferay.portal.kernel.util.PortalUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author Eudaldo Alonso
 */
public class ImageTypeContentUpgradeProcess extends UpgradeProcess {

	public ImageTypeContentUpgradeProcess(
		ImageLocalService imageLocalService,
		JournalArticleImageUpgradeHelper journalArticleImageUpgradeHelper,
		PortletFileRepository portletFileRepository) {

		_imageLocalService = imageLocalService;
		_journalArticleImageUpgradeHelper = journalArticleImageUpgradeHelper;
		_portletFileRepository = portletFileRepository;
	}

	@Override
	protected void doUpgrade() throws Exception {
		_copyJournalArticleImagesToJournalRepository();
		_dropJournalArticleImageTable();
	}

	private void _copyJournalArticleImagesToJournalRepository()
		throws Exception {

		try (LoggingTimer loggingTimer = new LoggingTimer()) {
			try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select JournalArticleImage.articleImageId, ",
					"JournalArticleImage.groupId, ",
					"JournalArticleImage.companyId, ",
					"JournalArticle.resourcePrimKey, JournalArticle.userId ",
					"from JournalArticleImage inner join JournalArticle on ",
					"(JournalArticle.groupId = JournalArticleImage.groupId ",
					"and JournalArticle.articleId = ",
					"JournalArticleImage.articleId and JournalArticle.version ",
					"= JournalArticleImage.version)"))) {
				try (ResultSet resultSet = preparedStatement.executeQuery()) {
					System.out.println("JournalArticleImage Entries: "+ resultSet.getFetchSize());
					while (resultSet.next()) {
						long articleImageId =  resultSet.getLong("articleImageId");
						long groupId = resultSet.getLong("groupId");
						long companyId =  resultSet.getLong("companyId");
						long resourcePrimKey =  resultSet.getLong("resourcePrimKey");
						long userId = PortalUtil.getValidUserId(
							companyId,  resultSet.getLong("userId"));
						long folderId =
							_journalArticleImageUpgradeHelper.getFolderId(
								userId, groupId, resourcePrimKey);

						String fileName = String.valueOf(articleImageId);

						FileEntry fileEntry =
							_portletFileRepository.fetchPortletFileEntry(
								groupId, folderId, fileName);

						if (fileEntry != null) {
							return;
						}

						try {
							Image image = _imageLocalService.getImage(
								articleImageId);

							if (image == null) {
								return;
							}

							String mimeType = MimeTypesUtil.getContentType(
								fileName + StringPool.PERIOD + image.getType());

							_portletFileRepository.addPortletFileEntry(
								groupId, userId, JournalArticle.class.getName(),
								resourcePrimKey, JournalConstants.SERVICE_NAME,
								folderId, image.getTextObj(), fileName, mimeType,
								false);

							_imageLocalService.deleteImage(image.getImageId());
						}

						catch (Exception exception) {
							_log.error(
								"Unable to add the journal article image " +
								fileName + " into the file repository",
								exception);

							throw exception;
						}
					}
				}
			}
		}
	}

	private void _dropJournalArticleImageTable() throws Exception {
		runSQL(connection, "drop table JournalArticleImage");

		if (_log.isInfoEnabled()) {
			_log.info("Deleted table JournalArticleImage");
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ImageTypeContentUpgradeProcess.class);

	private final ImageLocalService _imageLocalService;
	private final JournalArticleImageUpgradeHelper
		_journalArticleImageUpgradeHelper;
	private final PortletFileRepository _portletFileRepository;

}