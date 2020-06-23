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

package com.liferay.osb.loop.asset.entry.set.internal.search;

import com.liferay.osb.loop.asset.entry.set.constants.AssetEntrySetPortletKeys;
import com.liferay.osb.loop.asset.entry.set.model.AssetEntrySet;
import com.liferay.osb.loop.asset.entry.set.service.AssetEntrySetLocalService;
import com.liferay.osb.loop.asset.entry.set.util.AssetEntrySetParticipantInfo;
import com.liferay.osb.loop.asset.sharing.model.AssetSharingEntry;
import com.liferay.osb.loop.asset.sharing.service.AssetSharingEntryLocalService;
import com.liferay.portal.kernel.dao.orm.ActionableDynamicQuery;
import com.liferay.portal.kernel.dao.orm.IndexableActionableDynamicQuery;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.BaseIndexer;
import com.liferay.portal.kernel.search.BooleanClauseOccur;
import com.liferay.portal.kernel.search.BooleanQuery;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Hits;
import com.liferay.portal.kernel.search.IndexWriterHelper;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.search.Summary;
import com.liferay.portal.kernel.search.filter.BooleanFilter;
import com.liferay.portal.kernel.search.filter.TermsFilter;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.StringPool;

import java.io.Serializable;

import java.util.List;
import java.util.Locale;

import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * @author Sherry Yang
 * @author Timothy Bell
 */
@Component(immediate = true, service = Indexer.class)
public class AssetEntrySetIndexer extends BaseIndexer {

	public AssetEntrySetIndexer() {
		setFilterSearch(true);
		setPermissionAware(true);
	}

	@Override
	public String getClassName() {
		return AssetEntrySet.class.getName();
	}

	@Override
	public void postProcessContextBooleanFilter(
			BooleanFilter contextBooleanFilter, SearchContext searchContext)
		throws Exception {

		BooleanFilter booleanFilter = new BooleanFilter();

		addTermsFilter(booleanFilter, searchContext, "creators", "creator");
		addTermsFilter(
			booleanFilter, searchContext, "includeAssetEntrySetIds",
			"assetEntrySetId");
		addTermsFilter(booleanFilter, searchContext, "sharedTo", "sharedTo");

		if (booleanFilter.hasClauses()) {
			contextBooleanFilter.add(booleanFilter, BooleanClauseOccur.MUST);
		}

		contextBooleanFilter.addRequiredTerm("parentAssetEntrySetId", 0);

		addExcludeTermsFilter(
			contextBooleanFilter, searchContext, "excludeAssetEntrySetIds",
			"assetEntrySetId");
		addExcludeTermsFilter(
			contextBooleanFilter, searchContext, "excludeTypes", "type");
		addMembershipFilter(contextBooleanFilter, searchContext);
		addRequiredTermFilter(
			contextBooleanFilter, searchContext, "classNameId");
		addRequiredTermFilter(contextBooleanFilter, searchContext, "classPK");
		addRequiredTermFilter(
			contextBooleanFilter, searchContext, "entryClassPK");
		addRequiredTermFilter(
			contextBooleanFilter, searchContext, "privateAssetEntrySet");
		addRequiredTermFilter(contextBooleanFilter, searchContext, "type");
		addTimeFilter(
			contextBooleanFilter, searchContext, "createTime_sortable");
		addTimeFilter(
			contextBooleanFilter, searchContext, "modifiedTime_sortable");
		addTimeFilter(
			contextBooleanFilter, searchContext, "stickyTime_sortable");
	}

	@Override
	public void postProcessSearchQuery(
			BooleanQuery searchQuery, BooleanFilter fullQueryBooleanFilter,
			SearchContext searchContext)
		throws Exception {

		addSearchTerm(searchQuery, searchContext, "creatorName", true);
		addSearchTerm(searchQuery, searchContext, "message", true);
		addSearchTerm(searchQuery, searchContext, "title", true);
	}

	@Override
	public Hits search(SearchContext searchContext) throws SearchException {
		Hits hits = super.search(searchContext);

		if (searchContext.getStart() >= hits.getLength()) {
			hits.setDocs(new Document[0]);
			hits.setScores(new float[0]);
		}

		return hits;
	}

	protected void addExcludeTermsFilter(
			BooleanFilter contextBooleanFilter, SearchContext searchContext,
			String attributeName, String fieldName)
		throws Exception {

		BooleanFilter booleanFilter = new BooleanFilter();

		addTermsFilter(booleanFilter, searchContext, attributeName, fieldName);

		if (booleanFilter.hasClauses()) {
			contextBooleanFilter.add(
				booleanFilter, BooleanClauseOccur.MUST_NOT);
		}
	}

	protected void addMembershipFilter(
			BooleanFilter contextBooleanFilter, SearchContext searchContext)
		throws Exception {

		BooleanFilter booleanFilter = new BooleanFilter();

		String[] membershipSearchTerms =
			_assetEntrySetParticipantInfo.getMembershipSearchTerms(
				searchContext.getUserId());

		addTermsFilter(booleanFilter, membershipSearchTerms, "sharedTo");

		booleanFilter.addTerm("privateAssetEntrySet", StringPool.FALSE);

		contextBooleanFilter.add(booleanFilter, BooleanClauseOccur.MUST);
	}

	protected void addRequiredTermFilter(
		BooleanFilter contextBooleanFilter, SearchContext searchContext,
		String fieldName) {

		Serializable serializable = searchContext.getAttribute(fieldName);

		if (serializable != null) {
			contextBooleanFilter.addRequiredTerm(
				fieldName, String.valueOf(serializable));
		}
	}

	protected void addTermsFilter(
			BooleanFilter booleanFilter, SearchContext searchContext,
			String attributeName, String fieldName)
		throws Exception {

		String[] values = (String[])searchContext.getAttribute(attributeName);

		addTermsFilter(booleanFilter, values, fieldName);
	}

	protected void addTermsFilter(
			BooleanFilter booleanFilter, String[] values, String fieldName)
		throws Exception {

		if (ArrayUtil.isEmpty(values)) {
			return;
		}

		TermsFilter termsFilter = new TermsFilter(fieldName);

		termsFilter.addValues(values);

		booleanFilter.add(termsFilter);
	}

	protected void addTimeFilter(
		BooleanFilter contextBooleanFilter, SearchContext searchContext,
		String timeFieldName) {

		BooleanFilter booleanFilter = new BooleanFilter();

		long startValue = 0;
		long endValue = 0;

		long time = GetterUtil.getLong(
			searchContext.getAttribute(timeFieldName));

		if (time <= 0) {
			return;
		}

		String s = timeFieldName.substring(
			0, timeFieldName.indexOf(StringPool.UNDERLINE));

		String timeComparator = GetterUtil.getString(
			searchContext.getAttribute(s + "Comparator"));

		if (timeComparator.equals(StringPool.GREATER_THAN)) {
			startValue = time + 1;
			endValue = Long.MAX_VALUE;
		}
		else if (timeComparator.equals(StringPool.GREATER_THAN_OR_EQUAL)) {
			startValue = time;
			endValue = Long.MAX_VALUE;
		}
		else if (timeComparator.equals(StringPool.LESS_THAN)) {
			startValue = 0;
			endValue = time - 1;
		}
		else if (timeComparator.equals(StringPool.LESS_THAN_OR_EQUAL)) {
			startValue = 0;
			endValue = time;
		}

		booleanFilter.addRangeTerm(timeFieldName, startValue, endValue);

		contextBooleanFilter.add(booleanFilter, BooleanClauseOccur.MUST);
	}

	@Override
	protected void doDelete(Object obj) throws Exception {
		AssetEntrySet assetEntrySet = (AssetEntrySet)obj;

		deleteDocument(
			assetEntrySet.getCompanyId(), assetEntrySet.getAssetEntrySetId());
	}

	@Override
	protected Document doGetDocument(Object obj) throws Exception {
		AssetEntrySet assetEntrySet = (AssetEntrySet)obj;

		Document document = getBaseModelDocument(
			AssetEntrySetPortletKeys.ASSET_ENTRY_SET, assetEntrySet);

		document.addKeyword(Field.COMPANY_ID, assetEntrySet.getCompanyId());

		JSONObject payloadJSONObject = _jsonFactory.createJSONObject(
			assetEntrySet.getPayload());

		document.addText(
			Field.DESCRIPTION, payloadJSONObject.getString("message"));

		document.addText(Field.TITLE, assetEntrySet.getTitle());
		document.addKeyword(Field.TYPE, assetEntrySet.getType());

		document.addKeyword(
			"assetEntrySetId", assetEntrySet.getAssetEntrySetId());
		document.addNumber("createTime", assetEntrySet.getCreateTime());
		document.addText("creatorName", assetEntrySet.getCreatorName());
		document.addKeyword(
			"creatorName_sortable", assetEntrySet.getCreatorName());
		document.addKeyword(
			"creator",
			_assetEntrySetParticipantInfo.getSearchTerm(
				assetEntrySet.getCreatorClassNameId(),
				assetEntrySet.getCreatorClassPK()));
		document.addNumber("modifiedTime", assetEntrySet.getModifiedTime());
		document.addKeyword(
			"parentAssetEntrySetId", assetEntrySet.getParentAssetEntrySetId());
		document.addKeyword(
			"privateAssetEntrySet", assetEntrySet.isPrivateAssetEntrySet());
		document.addKeyword(
			"sharedTo", getSharedTo(assetEntrySet.getAssetEntrySetId()));
		document.addNumber("stickyTime", assetEntrySet.getStickyTime());

		return document;
	}

	@Override
	protected Summary doGetSummary(
			Document document, Locale locale, String snippet,
			PortletRequest portletRequest, PortletResponse portletResponse)
		throws Exception {

		Summary summary = createSummary(
			document, Field.TITLE, Field.DESCRIPTION);

		summary.setMaxContentLength(200);

		return summary;
	}

	@Override
	protected void doReindex(Object obj) throws Exception {
		AssetEntrySet assetEntrySet = (AssetEntrySet)obj;

		Document document = getDocument(assetEntrySet);

		_indexWriterHelper.updateDocument(
			getSearchEngineId(), assetEntrySet.getCompanyId(), document,
			isCommitImmediately());

		if (assetEntrySet.getParentAssetEntrySetId() > 0) {
			AssetEntrySet parentAssetEntrySet =
				_assetEntrySetLocalService.getAssetEntrySet(
					assetEntrySet.getParentAssetEntrySetId());

			doReindex(parentAssetEntrySet);
		}
	}

	@Override
	protected void doReindex(String className, long classPK) throws Exception {
		AssetEntrySet assetEntrySet =
			_assetEntrySetLocalService.getAssetEntrySet(classPK);

		doReindex(assetEntrySet);
	}

	@Override
	protected void doReindex(String[] ids) throws Exception {
		long companyId = GetterUtil.getLong(ids[0]);

		reindexAssetEntrySets(companyId);
	}

	protected String[] getSharedTo(long assetEntrySetId) throws Exception {
		List<AssetSharingEntry> assetSharingEntries =
			_assetSharingEntryLocalService.getAssetSharingEntries(
				_portal.getClassNameId(AssetEntrySet.class), assetEntrySetId);

		String[] sharedTo = new String[assetSharingEntries.size()];

		for (int i = 0; i < assetSharingEntries.size(); i++) {
			AssetSharingEntry assetSharingEntry = assetSharingEntries.get(i);

			sharedTo[i] = _assetEntrySetParticipantInfo.getSearchTerm(
				assetSharingEntry.getSharedToClassNameId(),
				assetSharingEntry.getSharedToClassPK());
		}

		return sharedTo;
	}

	protected void reindexAssetEntrySets(long companyId)
		throws PortalException {

		final IndexableActionableDynamicQuery indexableActionableDynamicQuery =
			_assetEntrySetLocalService.getIndexableActionableDynamicQuery();

		indexableActionableDynamicQuery.setCompanyId(companyId);
		indexableActionableDynamicQuery.setPerformActionMethod(
			new ActionableDynamicQuery.PerformActionMethod<AssetEntrySet>() {

				@Override
				public void performAction(AssetEntrySet assetEntrySet) {
					try {
						Document document = getDocument(assetEntrySet);

						indexableActionableDynamicQuery.addDocuments(document);
					}
					catch (PortalException pe) {
						if (_log.isWarnEnabled()) {
							_log.warn(
								"Unable to index asset entry set " +
									assetEntrySet.getAssetEntrySetId(),
								pe);
						}
					}
				}

			});
		indexableActionableDynamicQuery.setSearchEngineId(getSearchEngineId());

		indexableActionableDynamicQuery.performActions();
	}

	private static Log _log = LogFactoryUtil.getLog(AssetEntrySetIndexer.class);

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
	private IndexWriterHelper _indexWriterHelper;

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private Portal _portal;

}