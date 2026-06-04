/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.journal.content.web.internal.upgrade.v1_1_1;

import com.liferay.dynamic.data.mapping.model.DDMTemplate;
import com.liferay.dynamic.data.mapping.service.DDMTemplateLocalService;
import com.liferay.journal.constants.JournalContentPortletKeys;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.service.JournalArticleLocalService;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.portlet.PortletPreferencesFactoryUtil;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.upgrade.BasePortletPreferencesUpgradeProcess;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;

import jakarta.portlet.PortletPreferences;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Mikel Lorza
 */
public class UpgradePortletPreferences
	extends BasePortletPreferencesUpgradeProcess {

	public UpgradePortletPreferences(
		long ddmStructureClassNameId,
		DDMTemplateLocalService ddmTemplateLocalService,
		GroupLocalService groupLocalService,
		JournalArticleLocalService journalArticleLocalService,
		LayoutLocalService layoutLocalService, Portal portal) {

		_ddmStructureClassNameId = ddmStructureClassNameId;
		_ddmTemplateLocalService = ddmTemplateLocalService;
		_groupLocalService = groupLocalService;
		_journalArticleLocalService = journalArticleLocalService;
		_layoutLocalService = layoutLocalService;
		_portal = portal;
	}

	@Override
	protected String[] getPortletIds() {
		return new String[] {
			JournalContentPortletKeys.JOURNAL_CONTENT + "_INSTANCE_%"
		};
	}

	@Override
	protected String upgradePreferences(
			long companyId, long ownerId, int ownerType, long plid,
			String portletId, String xml)
		throws Exception {

		PortletPreferences portletPreferences =
			PortletPreferencesFactoryUtil.fromXML(
				companyId, ownerId, ownerType, plid, portletId, xml);

		String articleId = portletPreferences.getValue("articleId", null);
		long groupId = GetterUtil.getLong(
			portletPreferences.getValue("groupId", null));

		if (Validator.isNull(articleId) || (groupId == 0)) {
			return PortletPreferencesFactoryUtil.toXML(portletPreferences);
		}

		GroupInfo groupInfo = _groups.computeIfAbsent(
			groupId,
			key -> {
				Group group = _groupLocalService.fetchGroup(key);

				if (group == null) {
					return GroupInfo._MISSING;
				}

				return new GroupInfo(
					group.isCompany(), group.getExternalReferenceCode());
			});

		if (groupInfo == GroupInfo._MISSING) {
			return PortletPreferencesFactoryUtil.toXML(portletPreferences);
		}

		String journalArticleCacheKey = groupId + StringPool.POUND + articleId;

		JournalArticleInfo journalArticleInfo =
			_journalArticles.computeIfAbsent(
				journalArticleCacheKey,
				key -> {
					JournalArticle journalArticle =
						_journalArticleLocalService.fetchArticle(
							groupId, articleId);

					if (journalArticle == null) {
						return JournalArticleInfo._MISSING;
					}

					return new JournalArticleInfo(
						journalArticle.getExternalReferenceCode(),
						journalArticle.getGroupId());
				});

		if (journalArticleInfo == JournalArticleInfo._MISSING) {
			return PortletPreferencesFactoryUtil.toXML(portletPreferences);
		}

		portletPreferences.reset("articleId");
		portletPreferences.reset("groupId");
		portletPreferences.setValue(
			"articleExternalReferenceCode",
			journalArticleInfo._getExternalReferenceCode());
		portletPreferences.setValue(
			"groupExternalReferenceCode",
			groupInfo._getExternalReferenceCode());

		String ddmTemplateKey = portletPreferences.getValue(
			"ddmTemplateKey", null);

		if (Validator.isNull(ddmTemplateKey)) {
			return PortletPreferencesFactoryUtil.toXML(portletPreferences);
		}

		long ddmTemplateGroupId = _getDDMTemplateGroupId(
			groupInfo, journalArticleInfo, plid);

		String ddmTemplateCacheKey =
			ddmTemplateGroupId + StringPool.POUND + ddmTemplateKey;

		DDMTemplateInfo ddmTemplateInfo = _ddmTemplates.computeIfAbsent(
			ddmTemplateCacheKey,
			key -> {
				DDMTemplate ddmTemplate =
					_ddmTemplateLocalService.fetchTemplate(
						ddmTemplateGroupId, _ddmStructureClassNameId,
						ddmTemplateKey, true);

				if (ddmTemplate == null) {
					return DDMTemplateInfo._MISSING;
				}

				return new DDMTemplateInfo(
					ddmTemplate.getExternalReferenceCode());
			});

		if (ddmTemplateInfo == DDMTemplateInfo._MISSING) {
			return PortletPreferencesFactoryUtil.toXML(portletPreferences);
		}

		portletPreferences.reset("ddmTemplateKey");
		portletPreferences.setValue(
			"ddmTemplateExternalReferenceCode",
			ddmTemplateInfo._getExternalReferenceCode());

		return PortletPreferencesFactoryUtil.toXML(portletPreferences);
	}

	private long _getDDMTemplateGroupId(
			GroupInfo groupInfo, JournalArticleInfo journalArticleInfo,
			long plid)
		throws Exception {

		if (!groupInfo._isCompany()) {
			return _portal.getSiteGroupId(journalArticleInfo._getGroupId());
		}

		Layout layout = _layoutLocalService.fetchLayout(plid);

		if (layout != null) {
			return layout.getGroupId();
		}

		return _portal.getSiteGroupId(journalArticleInfo._getGroupId());
	}

	private final long _ddmStructureClassNameId;
	private final DDMTemplateLocalService _ddmTemplateLocalService;
	private final Map<String, DDMTemplateInfo> _ddmTemplates =
		new ConcurrentHashMap<>();
	private final GroupLocalService _groupLocalService;
	private final Map<Long, GroupInfo> _groups = new ConcurrentHashMap<>();
	private final JournalArticleLocalService _journalArticleLocalService;
	private final Map<String, JournalArticleInfo> _journalArticles =
		new ConcurrentHashMap<>();
	private final LayoutLocalService _layoutLocalService;
	private final Portal _portal;

	private static class DDMTemplateInfo {

		private DDMTemplateInfo(String externalReferenceCode) {
			_externalReferenceCode = externalReferenceCode;
		}

		private String _getExternalReferenceCode() {
			return _externalReferenceCode;
		}

		private static final DDMTemplateInfo _MISSING = new DDMTemplateInfo(
			null);

		private final String _externalReferenceCode;

	}

	private static class GroupInfo {

		private GroupInfo(boolean company, String externalReferenceCode) {
			_company = company;
			_externalReferenceCode = externalReferenceCode;
		}

		private String _getExternalReferenceCode() {
			return _externalReferenceCode;
		}

		private boolean _isCompany() {
			return _company;
		}

		private static final GroupInfo _MISSING = new GroupInfo(false, null);

		private final boolean _company;
		private final String _externalReferenceCode;

	}

	private static class JournalArticleInfo {

		private JournalArticleInfo(String externalReferenceCode, long groupId) {
			_externalReferenceCode = externalReferenceCode;
			_groupId = groupId;
		}

		private String _getExternalReferenceCode() {
			return _externalReferenceCode;
		}

		private long _getGroupId() {
			return _groupId;
		}

		private static final JournalArticleInfo _MISSING =
			new JournalArticleInfo(null, 0);

		private final String _externalReferenceCode;
		private final long _groupId;

	}

}