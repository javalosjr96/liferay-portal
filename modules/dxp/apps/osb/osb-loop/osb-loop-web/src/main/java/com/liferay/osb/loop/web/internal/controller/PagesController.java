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

package com.liferay.osb.loop.web.internal.controller;

import com.liferay.alloy.mvc.AlloyException;
import com.liferay.alloy.mvc.json.web.service.JSONWebServiceMethod;
import com.liferay.osb.loop.asset.entry.set.model.AssetEntrySet;
import com.liferay.osb.loop.asset.entry.set.service.AssetEntrySetLocalServiceUtil;
import com.liferay.osb.loop.constants.LoopConstants;
import com.liferay.osb.loop.model.LoopDivision;
import com.liferay.osb.loop.model.LoopPerson;
import com.liferay.osb.loop.service.LoopDivisionLocalServiceUtil;
import com.liferay.osb.loop.service.LoopPersonLocalServiceUtil;
import com.liferay.osb.loop.web.internal.constants.LoopAssetEntrySetConstants;
import com.liferay.osb.loop.web.internal.notifications.LoopUserNotificationEventUtil;
import com.liferay.osb.loop.web.internal.util.AssetEntrySetUtil;
import com.liferay.osb.loop.web.internal.util.LoopNotificationEventHelper;
import com.liferay.osb.loop.web.internal.util.LoopUserNotificationRecordUtil;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.BaseModel;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import java.io.Serializable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.portlet.PortletRequest;

/**
 * @author Timothy Bell
 */
public class PagesController extends LoopAlloyControllerImpl {

	public PagesController() {
		setAlloyNotificationEventHelper(new LoopNotificationEventHelper());
		setAlloyServiceInvokerClass(AssetEntrySet.class);
		setPermissioned(true);
	}

	public void add() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		_validateAdd();

		long classNameId = ParamUtil.getLong(request, "classNameId");
		long classPK = ParamUtil.getLong(request, "classPK");

		LoopPerson loopPerson =
			LoopPersonLocalServiceUtil.getLoopPersonByPersonUserId(
				themeDisplay.getUserId());

		String payload = ParamUtil.getString(request, "payload");
		String title = ParamUtil.getString(request, "title");

		AssetEntrySet assetEntrySet = AssetEntrySetUtil.addAssetEntrySet(
			request, themeDisplay, 0, classNameId, classPK,
			PortalUtil.getClassNameId(LoopPerson.class),
			loopPerson.getLoopPersonId(), payload, false, 0, title,
			LoopAssetEntrySetConstants.TYPE_PAGE,
			WorkflowConstants.STATUS_APPROVED);

		pollBaseModel = assetEntrySet;

		pollHitsLength = 1;

		alloyNotificationEventHelperPayloadJSONObject =
			JSONFactoryUtil.createJSONObject();

		alloyNotificationEventHelperPayloadJSONObject.put(
			"assetEntrySetId", assetEntrySet.getAssetEntrySetId());

		respondWith(
			AssetEntrySetUtil.getAssetEntrySetJSONObject(
				themeDisplay, assetEntrySet, 0, 0,
				LoopConstants.ASSET_ENTRY_SET_LIKED_PARTICIPANTS_LIMIT));
	}

	public void delete() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		AssetEntrySet assetEntrySet = _fetchAssetEntrySet();

		_validateDelete(assetEntrySet);

		AssetEntrySetUtil.sendActivityLogEmail(
			themeDisplay.getUser(), "delete",
			AssetEntrySetUtil.getAssetEntrySetJSONObject(
				themeDisplay, assetEntrySet, assetEntrySet.getAssetEntrySetId(),
				0, 0));

		AssetEntrySetUtil.deleteAssetEntrySet(
			assetEntrySet.getAssetEntrySetId());

		LoopUserNotificationEventUtil.deleteLoopUserNotificationEvents(
			new Object[] {
				"classNameId", PortalUtil.getClassNameId(AssetEntrySet.class),
				"classPK", assetEntrySet.getAssetEntrySetId()
			});

		LoopUserNotificationRecordUtil.deleteLoopUserNotificationRecords(
			PortalUtil.getClassNameId(AssetEntrySet.class),
			assetEntrySet.getAssetEntrySetId());

		pollBaseModel = assetEntrySet;

		pollHitsLength = 0;

		respondWith("The page was successfully deleted.");
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.RENDER_PHASE,
		parameterNames = {
			"classNameId", "classPK", "end", "reverseSort", "sortFieldName",
			"start"
		},
		parameterTypes = {
			Long.class, Long.class, Integer.class, Boolean.class, String.class,
			Integer.class
		}
	)
	public void index() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		Map<String, Serializable> attributes = new HashMap<>();

		long classNameId = ParamUtil.getLong(request, "classNameId");

		attributes.put(Field.CLASS_NAME_ID, classNameId);

		long classPK = ParamUtil.getLong(request, "classPK");

		attributes.put(Field.CLASS_PK, classPK);

		attributes.put(Field.TYPE, LoopAssetEntrySetConstants.TYPE_PAGE);

		String sortFieldName = ParamUtil.getString(request, "sortFieldName");

		int sortType = Sort.STRING_TYPE;

		if (Objects.equals(sortFieldName, "modifiedTime_sortable")) {
			sortType = Sort.LONG_TYPE;
		}

		boolean reverseSort = ParamUtil.getBoolean(request, "reverseSort");

		Sort[] sorts = {new Sort(sortFieldName, sortType, reverseSort)};

		respondWith(
			doSearch(
				_assetEntrySetIndexer, alloyServiceInvoker, attributes, sorts));
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.RENDER_PHASE,
		parameterNames = {"end", "keywords", "start"},
		parameterTypes = {Integer.class, String.class, Integer.class}
	)
	public void search() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		Map<String, Serializable> attributes = new HashMap<>();

		attributes.put(
			"excludeTypes",
			new String[] {
				String.valueOf(LoopAssetEntrySetConstants.TYPE_ANNOUNCEMENT),
				String.valueOf(LoopAssetEntrySetConstants.TYPE_POST)
			});

		respondWith(
			doSearch(
				_assetEntrySetIndexer, alloyServiceInvoker, attributes, null));
	}

	public void update() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		AssetEntrySet assetEntrySet = _fetchAssetEntrySet();

		_validateUpdate(assetEntrySet);

		JSONObject oldAssetEntrySetCompositeJSONObject =
			AssetEntrySetUtil.getAssetEntrySetJSONObject(
				themeDisplay, assetEntrySet, assetEntrySet.getAssetEntrySetId(),
				0, 0);

		String payload = ParamUtil.getString(request, "payload");
		String title = ParamUtil.getString(request, "title");

		assetEntrySet = AssetEntrySetUtil.updateAssetEntrySet(
			request, themeDisplay, assetEntrySet.getAssetEntrySetId(), payload,
			assetEntrySet.isPrivateAssetEntrySet(), 0, title,
			assetEntrySet.getType(), assetEntrySet.getStatus());

		JSONObject newAssetEntrySetCompositeJSONObject =
			AssetEntrySetUtil.getAssetEntrySetJSONObject(
				themeDisplay, assetEntrySet, assetEntrySet.getAssetEntrySetId(),
				0, 0);

		AssetEntrySetUtil.sendActivityLogEmail(
			themeDisplay.getUser(), "update",
			newAssetEntrySetCompositeJSONObject,
			oldAssetEntrySetCompositeJSONObject);

		respondWith(newAssetEntrySetCompositeJSONObject);
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.RENDER_PHASE, parameterNames = "id",
		parameterTypes = Long.class
	)
	public void view() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		AssetEntrySet assetEntrySet = _fetchAssetEntrySet();

		_validateView(assetEntrySet);

		respondWith(
			AssetEntrySetUtil.getAssetEntrySetJSONObject(
				themeDisplay, assetEntrySet, assetEntrySet.getAssetEntrySetId(),
				0, 0));
	}

	@Override
	protected BaseModel<?> fetchAttachedBaseModel() {
		BaseModel<?> baseModel = null;

		long classNameId = ParamUtil.getLong(request, "classNameId");

		if (classNameId == PortalUtil.getClassNameId(LoopDivision.class)) {
			long classPK = ParamUtil.getLong(request, "classPK");

			baseModel = LoopDivisionLocalServiceUtil.fetchLoopDivision(classPK);
		}

		return baseModel;
	}

	private AssetEntrySet _fetchAssetEntrySet() {
		long assetEntrySetId = ParamUtil.getLong(request, "id");

		return AssetEntrySetLocalServiceUtil.fetchAssetEntrySet(
			assetEntrySetId);
	}

	private void _validateAdd() throws Exception {
		_validateAttachedBaseModel();
		_validateTitle(null);
	}

	private void _validateAttachedBaseModel() throws Exception {
		BaseModel<?> baseModel = fetchAttachedBaseModel();

		if (baseModel == null) {
			throw new AlloyException("the-model-does-not-exist");
		}
	}

	private void _validateDelete(AssetEntrySet assetEntrySet) throws Exception {
		_validatePage(assetEntrySet);
	}

	private void _validatePage(AssetEntrySet assetEntrySet) throws Exception {
		if ((assetEntrySet == null) ||
			(assetEntrySet.getType() != LoopAssetEntrySetConstants.TYPE_PAGE)) {

			throw new AlloyException("the-page-does-not-exist");
		}
	}

	private void _validateTitle(AssetEntrySet assetEntrySet) throws Exception {
		String title = ParamUtil.getString(request, "title");

		if (Validator.isNull(title)) {
			throw new AlloyException("the-title-is-not-set");
		}

		long classNameId = ParamUtil.getLong(request, "classNameId");
		long classPK = ParamUtil.getLong(request, "classPK");

		if (assetEntrySet != null) {
			classNameId = assetEntrySet.getClassNameId();
			classPK = assetEntrySet.getClassPK();
		}

		AssetEntrySet duplicateAssetEntrySet =
			AssetEntrySetLocalServiceUtil.fetchAssetEntrySet(
				classNameId, classPK, title);

		if ((duplicateAssetEntrySet != null) &&
			((assetEntrySet == null) ||
			 (duplicateAssetEntrySet.getAssetEntrySetId() !=
				 assetEntrySet.getAssetEntrySetId()))) {

			throw new AlloyException("the-page-title-already-exists");
		}
	}

	private void _validateUpdate(AssetEntrySet assetEntrySet) throws Exception {
		_validatePage(assetEntrySet);
		_validateTitle(assetEntrySet);
	}

	private void _validateView(AssetEntrySet assetEntrySet) throws Exception {
		_validatePage(assetEntrySet);
	}

	private static final Indexer _assetEntrySetIndexer =
		IndexerRegistryUtil.nullSafeGetIndexer(AssetEntrySet.class);

}