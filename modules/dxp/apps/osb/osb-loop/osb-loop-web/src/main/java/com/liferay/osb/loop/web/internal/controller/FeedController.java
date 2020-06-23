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
import com.liferay.alloy.mvc.AlloyServiceInvoker;
import com.liferay.alloy.mvc.json.web.service.JSONWebServiceMethod;
import com.liferay.osb.loop.asset.entry.set.model.AssetEntrySet;
import com.liferay.osb.loop.asset.entry.set.model.AssetEntrySetLike;
import com.liferay.osb.loop.asset.entry.set.service.AssetEntrySetLikeLocalServiceUtil;
import com.liferay.osb.loop.asset.entry.set.service.AssetEntrySetLocalServiceUtil;
import com.liferay.osb.loop.asset.entry.set.util.AssetEntrySetParticipantInfoUtil;
import com.liferay.osb.loop.asset.sharing.model.AssetSharingEntry;
import com.liferay.osb.loop.asset.sharing.service.AssetSharingEntryLocalServiceUtil;
import com.liferay.osb.loop.constants.LoopConstants;
import com.liferay.osb.loop.model.LoopDivision;
import com.liferay.osb.loop.model.LoopExternalReferenceRel;
import com.liferay.osb.loop.model.LoopPerson;
import com.liferay.osb.loop.model.LoopStreamEntry;
import com.liferay.osb.loop.model.LoopTopic;
import com.liferay.osb.loop.model.LoopUserNotificationEvent;
import com.liferay.osb.loop.service.LoopPersonLocalServiceUtil;
import com.liferay.osb.loop.web.internal.composite.LoopPersonComposite;
import com.liferay.osb.loop.web.internal.configuration.LoopWebConfigurationKeys;
import com.liferay.osb.loop.web.internal.configuration.LoopWebConfigurationUtil;
import com.liferay.osb.loop.web.internal.configuration.LoopWebConfigurationValues;
import com.liferay.osb.loop.web.internal.constants.LoopAssetEntrySetConstants;
import com.liferay.osb.loop.web.internal.constants.LoopStreamConstants;
import com.liferay.osb.loop.web.internal.constants.LoopStreamEntryConstants;
import com.liferay.osb.loop.web.internal.constants.LoopUserNotificationConstants;
import com.liferay.osb.loop.web.internal.notifications.LoopUserNotificationEventUtil;
import com.liferay.osb.loop.web.internal.util.AssetEntrySetUtil;
import com.liferay.osb.loop.web.internal.util.LoopCompositeUtil;
import com.liferay.osb.loop.web.internal.util.LoopExternalReferenceRelUtil;
import com.liferay.osb.loop.web.internal.util.LoopHTMLTruncator;
import com.liferay.osb.loop.web.internal.util.LoopHttpServletRequestUtil;
import com.liferay.osb.loop.web.internal.util.LoopMarkdownUtil;
import com.liferay.osb.loop.web.internal.util.LoopNotificationEventHelper;
import com.liferay.osb.loop.web.internal.util.LoopParticipantAssignmentUtil;
import com.liferay.osb.loop.web.internal.util.LoopPersonUtil;
import com.liferay.osb.loop.web.internal.util.LoopStatsEntryUtil;
import com.liferay.osb.loop.web.internal.util.LoopStreamEntryUtil;
import com.liferay.osb.loop.web.internal.util.LoopUserNotificationRecordUtil;
import com.liferay.osb.loop.web.internal.util.LoopUserNotificationSubscriptionUtil;
import com.liferay.osb.loop.web.internal.util.LoopUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.configuration.Filter;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.BaseModel;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.servlet.HttpHeaders;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import java.io.File;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.portlet.PortletRequest;

/**
 * @author Timothy Bell
 */
public class FeedController extends LoopAlloyControllerImpl {

	public FeedController() {
		setAlloyNotificationEventHelper(new LoopNotificationEventHelper());
		setAlloyServiceInvokerClass(AssetEntrySet.class);
		setFeaturedPreferenceKey("featuredAssetEntrySetIds");
		setPermissioned(true);
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.ACTION_PHASE,
		parameterNames = {
			"parentAssetEntrySetId", "payload", "privateAssetEntrySet",
			"stickyTime", "title", "type"
		},
		parameterTypes = {
			Long.class, String.class, Boolean.class, Long.class, String.class,
			Integer.class
		}
	)
	public void addMyPost() throws Exception {
		_validateAddMyPost();

		_addAssetEntrySet(true);
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.ACTION_PHASE, methodName = "addMyPost",
		parameterNames = {
			"parentAssetEntrySetId", "payload", "privateAssetEntrySet"
		},
		parameterTypes = {Long.class, String.class, Boolean.class}
	)
	public void addMyPost2() throws Exception {
		addMyPost();
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.ACTION_PHASE, methodName = "addMyPost",
		parameterNames = {
			"parentAssetEntrySetId", "payload", "privateAssetEntrySet",
			"stickyTime", "type"
		},
		parameterTypes = {
			Long.class, String.class, Boolean.class, Long.class, Integer.class
		}
	)
	public void addMyPost3() throws Exception {
		addMyPost();
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.ACTION_PHASE,
		parameterNames = {
			"creatorClassNameId", "creatorClassPK", "parentAssetEntrySetId",
			"payload", "privateAssetEntrySet", "stickyTime", "title", "type"
		},
		parameterTypes = {
			Long.class, Long.class, Long.class, String.class, Boolean.class,
			Long.class, String.class, Integer.class
		}
	)
	public void addPost() throws Exception {
		_validateAddPost();

		_addAssetEntrySet(false);
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.ACTION_PHASE, methodName = "addPost",
		parameterNames = {
			"creatorClassNameId", "creatorClassPK", "parentAssetEntrySetId",
			"payload", "privateAssetEntrySet"
		},
		parameterTypes = {
			Long.class, Long.class, Long.class, String.class, Boolean.class
		}
	)
	public void addPost2() throws Exception {
		addPost();
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.ACTION_PHASE, methodName = "addPost",
		parameterNames = {
			"creatorClassNameId", "creatorClassPK", "parentAssetEntrySetId",
			"payload", "privateAssetEntrySet", "stickyTime", "type"
		},
		parameterTypes = {
			Long.class, Long.class, Long.class, String.class, Boolean.class,
			Long.class, Integer.class
		}
	)
	public void addPost3() throws Exception {
		addPost();
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.ACTION_PHASE,
		parameterNames = {"classNameId", "classPK", "loopStreamId"},
		parameterTypes = {Long.class, Long.class, Long.class}
	)
	public void addToStream() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		_validateAddToStream();

		LoopPerson loopPerson = LoopPersonUtil.getLoopPerson(
			themeDisplay.getUserId());

		long classNameId = ParamUtil.getLong(request, "classNameId");
		long classPK = ParamUtil.getLong(request, "classPK");
		long loopStreamId = ParamUtil.getLong(
			request, "loopStreamId",
			LoopStreamConstants.LOOP_STREAM_ID_FOLLOWING);

		int defaultFollowingType = LoopStreamEntryConstants.TYPE_FOLLOWING_FULL;

		if (classNameId == PortalUtil.getClassNameId(LoopPerson.class)) {
			defaultFollowingType =
				LoopStreamEntryConstants.TYPE_FOLLOWING_LIMITED;
		}

		int followingType = ParamUtil.getInteger(
			request, "followingType", defaultFollowingType);

		boolean previouslyFollowing = LoopStreamEntryUtil.isFollowing(
			loopPerson.getLoopPersonId(), classNameId, classPK);

		LoopStreamEntryUtil.updateLoopStreamEntry(
			this, loopPerson.getLoopPersonId(), loopStreamId, classNameId,
			classPK, true, followingType);

		respondWith(
			_getFollowingJSONObject(
				classNameId, classPK, loopPerson.getLoopPersonId(),
				previouslyFollowing, true, followingType));
	}

	public void create() throws Exception {
		_validateAddPost();

		_addAssetEntrySet(false);
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.ACTION_PHASE, parameterNames = "id",
		parameterTypes = Long.class
	)
	public void delete() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		AssetEntrySet assetEntrySet = _fetchAssetEntrySet();

		_validateDelete(assetEntrySet);

		LoopStatsEntryUtil.updateScore(
			this, assetEntrySet,
			-LoopWebConfigurationValues.LOOP_STATS_ENTRY_SCORE_VALUE_ADD_POST);

		LoopStreamEntryUtil.deleteLoopStreamEntries(
			this, PortalUtil.getClassNameId(AssetEntrySet.class),
			assetEntrySet.getAssetEntrySetId());

		List<AssetEntrySet> assetEntrySets = new ArrayList<>();

		assetEntrySets.add(assetEntrySet);
		assetEntrySets.addAll(
			AssetEntrySetUtil.getAllChildAssetEntrySets(assetEntrySet));

		for (AssetEntrySet curAssetEntrySet : assetEntrySets) {
			LoopUserNotificationEventUtil.deleteLoopUserNotificationEvents(
				new Object[] {
					"classNameId",
					PortalUtil.getClassNameId(AssetEntrySet.class), "classPK",
					curAssetEntrySet.getAssetEntrySetId()
				});
			LoopUserNotificationRecordUtil.deleteLoopUserNotificationRecords(
				PortalUtil.getClassNameId(AssetEntrySet.class),
				curAssetEntrySet.getAssetEntrySetId());
		}

		LoopUserNotificationSubscriptionUtil.
			deleteLoopUserNotificationSubscriptions(
				PortalUtil.getClassNameId(AssetEntrySet.class),
				assetEntrySet.getAssetEntrySetId());

		AssetEntrySetUtil.deleteAssetEntrySet(
			assetEntrySet.getAssetEntrySetId());

		respondWith("The post was successfully deleted.");
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.ACTION_PHASE,
		parameterNames = {"classNameId", "classPK", "type"},
		parameterTypes = {Long.class, Long.class, Integer.class}
	)
	public void disableNotifications() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		_validateDisableNotifications();

		LoopPerson loopPerson = LoopPersonUtil.getLoopPerson(
			themeDisplay.getUserId());

		long classNameId = ParamUtil.getLong(request, "classNameId");
		long classPK = ParamUtil.getLong(request, "classPK");
		int deliveryType = ParamUtil.getInteger(
			request, "type",
			LoopUserNotificationConstants.DELIVERY_TYPE_IN_APP);

		LoopUserNotificationSubscriptionUtil.
			deleteLoopUserNotificationSubscription(
				loopPerson.getLoopPersonId(), classNameId, classPK,
				deliveryType);

		respondWith("Notifications were successfully disabled.");
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.ACTION_PHASE,
		parameterNames = {"classNameId", "classPK", "type"},
		parameterTypes = {Long.class, Long.class, Integer.class}
	)
	public void enableNotifications() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		_validateEnableNotifications();

		LoopPerson loopPerson = LoopPersonUtil.getLoopPerson(
			themeDisplay.getUserId());

		long classNameId = ParamUtil.getLong(request, "classNameId");
		long classPK = ParamUtil.getLong(request, "classPK");
		int deliveryType = ParamUtil.getInteger(
			request, "type",
			LoopUserNotificationConstants.DELIVERY_TYPE_IN_APP);

		LoopUserNotificationSubscriptionUtil.
			updateLoopUserNotificationSubscription(
				this, loopPerson.getLoopPersonId(), classNameId, classPK,
				deliveryType);

		respondWith("Notifications were successfully enabled.");
	}

	@Override
	public BaseModel<?> fetchBaseModel() {
		return _fetchAssetEntrySet();
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.ACTION_PHASE,
		parameterNames = {"classNameId", "classPK"},
		parameterTypes = {Long.class, Long.class}
	)
	public void follow() throws Exception {
		addToStream();
	}

	public void getMessage() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		AssetEntrySet assetEntrySet = _fetchAssetEntrySet();

		_validateAssetEntrySet(assetEntrySet);

		JSONObject truncatedMessageJSONObject =
			JSONFactoryUtil.createJSONObject();

		JSONObject payloadJSONObject = JSONFactoryUtil.createJSONObject(
			assetEntrySet.getPayload());

		String message = LoopMarkdownUtil.interpretTokens(
			themeDisplay.getCompanyId(), payloadJSONObject.getString("message"),
			payloadJSONObject.getJSONArray(
				LoopAssetEntrySetConstants.PAYLOAD_KEY_SHARED_TO),
			true);

		boolean truncated = ParamUtil.getBoolean(request, "truncated");

		if (truncated) {
			Filter filter = new Filter(LoopAssetEntrySetConstants.LABEL_POST);

			LoopHTMLTruncator htmlTruncator = new LoopHTMLTruncator(
				LoopUtil.getPortalURL(request, themeDisplay.getCompanyId()),
				message,
				GetterUtil.getInteger(
					LoopWebConfigurationUtil.get(
						LoopWebConfigurationKeys.
							LOOP_TRUNCATED_FEED_LINE_CHAR_COUNT,
						filter)),
				GetterUtil.getInteger(
					LoopWebConfigurationUtil.get(
						LoopWebConfigurationKeys.
							LOOP_TRUNCATED_FEED_LINE_CHAR_COUNT_TABLE_ELEMENT,
						filter)),
				GetterUtil.getInteger(
					LoopWebConfigurationUtil.get(
						LoopWebConfigurationKeys.
							LOOP_TRUNCATED_FEED_LINE_COUNT_GRACE,
						filter)),
				GetterUtil.getInteger(
					LoopWebConfigurationUtil.get(
						LoopWebConfigurationKeys.
							LOOP_TRUNCATED_FEED_LINE_COUNT_IMAGE,
						filter)),
				GetterUtil.getInteger(
					LoopWebConfigurationUtil.get(
						LoopWebConfigurationKeys.
							LOOP_TRUNCATED_FEED_LINE_COUNT_IMAGE_TABLE_ELEMENT,
						filter)),
				GetterUtil.getInteger(
					LoopWebConfigurationUtil.get(
						LoopWebConfigurationKeys.
							LOOP_TRUNCATED_FEED_LINE_COUNT_MAX,
						filter)));

			String truncatedHtml = htmlTruncator.truncateHtml();

			truncated = Validator.isNotNull(truncatedHtml);

			if (truncated) {
				message = truncatedHtml;
			}
		}

		truncatedMessageJSONObject.put("message", message);

		truncatedMessageJSONObject.put("truncated", truncated);

		respondWith(truncatedMessageJSONObject);
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.ACTION_PHASE,
		parameterNames = {"id", "likedParticipantsLimit"},
		parameterTypes = {Long.class, Integer.class}
	)
	public void like() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		AssetEntrySet assetEntrySet = _fetchAssetEntrySet();

		_validateLike(assetEntrySet);

		LoopPerson loopPerson = LoopPersonUtil.getLoopPerson(
			themeDisplay.getUserId());

		AssetEntrySetLike assetEntrySetLike =
			AssetEntrySetLikeLocalServiceUtil.fetchAssetEntrySetLike(
				assetEntrySet.getAssetEntrySetId(),
				PortalUtil.getClassNameId(LoopPerson.class),
				loopPerson.getLoopPersonId());

		if (assetEntrySetLike == null) {
			assetEntrySet = AssetEntrySetLocalServiceUtil.likeAssetEntrySet(
				themeDisplay.getUserId(), assetEntrySet.getAssetEntrySetId());

			if (assetEntrySet.getCreatorClassNameId() ==
					PortalUtil.getClassNameId(LoopPerson.class)) {

				alloyNotificationEventHelperPayloadJSONObject =
					JSONFactoryUtil.createJSONObject();

				alloyNotificationEventHelperPayloadJSONObject.put(
					"assetEntrySetId", assetEntrySet.getAssetEntrySetId());
				alloyNotificationEventHelperPayloadJSONObject.put(
					"loopPersonId", loopPerson.getLoopPersonId());
			}

			LoopStatsEntryUtil.updateScore(
				this, assetEntrySet,
				LoopWebConfigurationValues.LOOP_STATS_ENTRY_SCORE_VALUE_LIKE);
		}

		int likedParticipantsLimit = ParamUtil.getInteger(
			request, "likedParticipantsLimit");

		respondWith(
			AssetEntrySetUtil.getAssetEntrySetJSONObject(
				themeDisplay, assetEntrySet, 0, 0, likedParticipantsLimit));
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.ACTION_PHASE, methodName = "like",
		parameterNames = {
			"childAssetEntrySetsLimit", "id", "likedParticipantsLimit"
		},
		parameterTypes = {Integer.class, Long.class, Integer.class}
	)
	public void like1() throws Exception {
		like();
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.RENDER_PHASE,
		parameterNames = {
			"childAssetEntrySetsLimit", "classNameId", "classPK", "createTime",
			"end", "likedParticipantsLimit", "parentAssetEntrySetId", "payload",
			"start", "stickyTime"
		},
		parameterTypes = {
			Integer.class, Long.class, Long.class, Long.class, Integer.class,
			Integer.class, Long.class, String.class, Integer.class, Long.class
		}
	)
	public void refreshFeed() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		respondWith(_getRefreshJSONObject(false));
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.RENDER_PHASE,
		parameterNames = {
			"childAssetEntrySetsLimit", "end", "likedParticipantsLimit",
			"loopStreamId", "modifiedTime", "parentAssetEntrySetId", "payload",
			"start", "stickyTime"
		},
		parameterTypes = {
			Integer.class, Integer.class, Integer.class, Long.class, Long.class,
			Long.class, String.class, Integer.class, Long.class
		}
	)
	public void refreshMyFeed() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		respondWith(_getRefreshJSONObject(true));
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.ACTION_PHASE,
		parameterNames = {"classNameId", "classPK", "loopStreamId"},
		parameterTypes = {Long.class, Long.class, Long.class}
	)
	public void removeFromStream() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		_validateRemoveFromStream();

		LoopPerson loopPerson = LoopPersonUtil.getLoopPerson(
			themeDisplay.getUserId());

		long classNameId = ParamUtil.getLong(request, "classNameId");
		long classPK = ParamUtil.getLong(request, "classPK");
		long loopStreamId = ParamUtil.getLong(
			request, "loopStreamId",
			LoopStreamConstants.LOOP_STREAM_ID_FOLLOWING);

		boolean previouslyFollowing = LoopStreamEntryUtil.isFollowing(
			loopPerson.getLoopPersonId(), classNameId, classPK);

		if (loopStreamId == LoopStreamConstants.LOOP_STREAM_ID_BOOKMARKS) {
			LoopStreamEntryUtil.deleteLoopStreamEntries(
				this, loopPerson.getLoopPersonId(), loopStreamId, classNameId,
				classPK);
		}
		else {
			LoopStreamEntryUtil.updateLoopStreamEntry(
				this, loopPerson.getLoopPersonId(), loopStreamId, classNameId,
				classPK, false, LoopStreamEntryConstants.TYPE_UNFOLLOWING);
		}

		respondWith(
			_getFollowingJSONObject(
				classNameId, classPK, loopPerson.getLoopPersonId(),
				previouslyFollowing, false,
				LoopStreamEntryConstants.TYPE_UNFOLLOWING));
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.RENDER_PHASE,
		parameterNames = {
			"childAssetEntrySetsLimit", "end", "keywords",
			"likedParticipantsLimit", "start"
		},
		parameterTypes = {
			Integer.class, Integer.class, String.class, Integer.class,
			Integer.class
		}
	)
	public void search() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		Map<String, Serializable> attributes = new HashMap<>();

		attributes.put(
			"excludeTypes",
			new String[] {
				String.valueOf(LoopAssetEntrySetConstants.TYPE_PAGE)
			});

		respondWith(
			doSearch(
				_assetEntrySetIndexer, alloyServiceInvoker, attributes, null));
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.ACTION_PHASE,
		parameterNames = {"classNameId", "classPK"},
		parameterTypes = {Long.class, Long.class}
	)
	public void unfollow() throws Exception {
		removeFromStream();
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.ACTION_PHASE,
		parameterNames = {"id", "likedParticipantsLimit"},
		parameterTypes = {Long.class, Integer.class}
	)
	public void unlike() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		AssetEntrySet assetEntrySet = _fetchAssetEntrySet();

		_validateUnlike(assetEntrySet);

		assetEntrySet = AssetEntrySetLocalServiceUtil.unlikeAssetEntrySet(
			themeDisplay.getUserId(), assetEntrySet.getAssetEntrySetId());

		LoopPerson loopPerson = LoopPersonUtil.getLoopPerson(
			themeDisplay.getUserId());

		LoopUserNotificationEventUtil.deleteLoopUserNotificationEvents(
			new Object[] {
				"classNameId", PortalUtil.getClassNameId(LoopPerson.class),
				"classPK", loopPerson.getLoopPersonId(), "groupClassNameId",
				PortalUtil.getClassNameId(AssetEntrySet.class), "groupClassPK",
				assetEntrySet.getAssetEntrySetId(), "type",
				LoopUserNotificationConstants.TYPE_LIKED
			});

		LoopStatsEntryUtil.updateScore(
			this, assetEntrySet,
			-LoopWebConfigurationValues.LOOP_STATS_ENTRY_SCORE_VALUE_LIKE);

		int likedParticipantsLimit = ParamUtil.getInteger(
			request, "likedParticipantsLimit");

		respondWith(
			AssetEntrySetUtil.getAssetEntrySetJSONObject(
				themeDisplay, assetEntrySet, 0, 0, likedParticipantsLimit));
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.ACTION_PHASE, methodName = "unlike",
		parameterNames = {
			"childAssetEntrySetsLimit", "id", "likedParticipantsLimit"
		},
		parameterTypes = {Integer.class, Long.class, Integer.class}
	)
	public void unlike1() throws Exception {
		unlike();
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.ACTION_PHASE,
		parameterNames = {"id", "payload", "stickyTime", "title", "type"},
		parameterTypes = {
			Long.class, String.class, Long.class, String.class, Integer.class
		}
	)
	public void update() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		AssetEntrySet assetEntrySet = _fetchAssetEntrySet();

		_validateUpdate(assetEntrySet);

		String payload = ParamUtil.getString(request, "payload");

		JSONObject payloadJSONObject = JSONFactoryUtil.createJSONObject(
			payload);

		_processSharedTo(payloadJSONObject);

		long stickyTime = ParamUtil.getLong(
			request, "stickyTime", assetEntrySet.getStickyTime());
		String title = ParamUtil.getString(request, "title");
		int type = ParamUtil.getInteger(
			request, "type", assetEntrySet.getType());

		assetEntrySet = AssetEntrySetUtil.updateAssetEntrySet(
			request, themeDisplay, assetEntrySet.getAssetEntrySetId(),
			payloadJSONObject.toString(),
			assetEntrySet.isPrivateAssetEntrySet(), stickyTime, title, type,
			assetEntrySet.getStatus());

		_addLoopUserNotificationSubscription(assetEntrySet);

		alloyNotificationEventHelperPayloadJSONObject =
			JSONFactoryUtil.createJSONObject();

		alloyNotificationEventHelperPayloadJSONObject.put(
			"assetEntrySetId", assetEntrySet.getAssetEntrySetId());
		alloyNotificationEventHelperPayloadJSONObject.put(
			"sharedToJSONArray",
			_getSharedToJSONArray(assetEntrySet.getAssetEntrySetId()));

		respondWith(
			AssetEntrySetUtil.getAssetEntrySetJSONObject(
				themeDisplay, assetEntrySet, assetEntrySet.getAssetEntrySetId(),
				0, LoopConstants.ASSET_ENTRY_SET_LIKED_PARTICIPANTS_LIMIT));
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.ACTION_PHASE, methodName = "update",
		parameterNames = {"id", "payload", "privateAssetEntrySet"},
		parameterTypes = {Long.class, String.class, Boolean.class}
	)
	public void update2() throws Exception {
		update();
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.ACTION_PHASE, methodName = "update",
		parameterNames = {
			"id", "payload", "stickyTime", "type", "privateAssetEntrySet"
		},
		parameterTypes = {
			Long.class, String.class, Long.class, Integer.class, Boolean.class
		}
	)
	public void update3() throws Exception {
		update();
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.ACTION_PHASE, methodName = "update",
		parameterNames = {"id", "payload", "stickyTime", "type"},
		parameterTypes = {Long.class, String.class, Long.class, Integer.class}
	)
	public void update4() throws Exception {
		update();
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.ACTION_PHASE, parameterNames = "file",
		parameterTypes = File.class
	)
	public void upload() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		UploadPortletRequest uploadPortletRequest =
			PortalUtil.getUploadPortletRequest(actionRequest);

		File file = uploadPortletRequest.getFile("file");

		validateUploadImage(file);

		respondWith(
			AssetEntrySetUtil.addFileAttachment(
				themeDisplay.getUserId(), file));
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

		_view(assetEntrySet);
	}

	public void viewExternalReferenceFeed() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		_validateExternalReferenceFeed();

		long time = ParamUtil.getLong(request, "time");
		String timeFieldName = ParamUtil.getString(request, "timeFieldName");
		boolean newAssetEntrySets = ParamUtil.getBoolean(
			request, "newAssetEntrySets");

		Boolean privateAssetEntrySet = null;

		if (Validator.isNotNull(request.getParameter("privateAssetEntrySet"))) {
			privateAssetEntrySet = Boolean.valueOf(
				request.getParameter("privateAssetEntrySet"));
		}

		List<String> fullyFollowedEntities = new ArrayList<>();

		String externalReferenceName = ParamUtil.getString(
			request, "externalReferenceName");
		String externalReferencePK = ParamUtil.getString(
			request, "externalReferencePK");

		LoopExternalReferenceRel loopExternalReferenceRel =
			LoopExternalReferenceRelUtil.fetchLoopExternalReferenceRel(
				externalReferenceName, externalReferencePK);

		fullyFollowedEntities.add(
			AssetEntrySetParticipantInfoUtil.getSearchTerm(
				loopExternalReferenceRel.getClassNameId(),
				loopExternalReferenceRel.getClassPK()));

		Map<String, Serializable> attributes = _getAttributes(
			time, timeFieldName, newAssetEntrySets, privateAssetEntrySet,
			fullyFollowedEntities);

		Sort[] sorts = {
			new Sort(timeFieldName + "_sortable", Sort.LONG_TYPE, true)
		};

		respondWith(
			doSearch(
				_assetEntrySetIndexer, alloyServiceInvoker, attributes, sorts));
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.RENDER_PHASE,
		parameterNames = {"classNameId", "classPK", "end", "start"},
		parameterTypes = {Long.class, Long.class, Integer.class, Integer.class}
	)
	public void viewFollowers() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		_validateViewFollowers();

		long classNameId = ParamUtil.getLong(request, "classNameId");
		long classPK = ParamUtil.getLong(request, "classPK");
		int start = ParamUtil.getInteger(request, "start");

		int end = ParamUtil.getInteger(request, "end");

		if (end == 0) {
			end = LoopWebConfigurationValues.LOOP_PAGE_DEFAULT_DELTA;
		}

		List<LoopPersonComposite> loopPersonComposites =
			LoopStreamEntryUtil.getFollowers(
				themeDisplay, classNameId, classPK, start, end);

		respondWith(
			LoopCompositeUtil.getCompositesJSONArray(loopPersonComposites));
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.RENDER_PHASE,
		parameterNames = {"end", "id", "start"},
		parameterTypes = {Integer.class, Long.class, Integer.class}
	)
	public void viewLikedParticipants() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		AssetEntrySet assetEntrySet = _fetchAssetEntrySet();

		_validateViewLikedParticipants(assetEntrySet);

		JSONArray likedParticipantsJSONArray =
			JSONFactoryUtil.createJSONArray();

		int start = ParamUtil.getInteger(request, "start");

		int end = ParamUtil.getInteger(request, "end");

		if (end == 0) {
			end = LoopConstants.ASSET_ENTRY_SET_LIKED_PARTICIPANTS_LIMIT;
		}

		List<AssetEntrySetLike> assetEntrySetLikes =
			AssetEntrySetLikeLocalServiceUtil.getAssetEntrySetLikes(
				assetEntrySet.getAssetEntrySetId(), 0, 0, start, end);

		for (AssetEntrySetLike assetEntrySetLike : assetEntrySetLikes) {
			likedParticipantsJSONArray.put(
				LoopUtil.getCompositeJSONObject(
					themeDisplay, assetEntrySetLike.getClassNameId(),
					assetEntrySetLike.getClassPK(), StringPool.BLANK));
		}

		respondWith(likedParticipantsJSONArray);
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.RENDER_PHASE,
		parameterNames = {
			"childAssetEntrySetsLimit", "end", "likedParticipantsLimit",
			"loopStreamAliasId", "newAssetEntrySets", "parentAssetEntrySetId",
			"start", "stickyTime", "time"
		},
		parameterTypes = {
			Integer.class, Integer.class, Integer.class, Long.class,
			Boolean.class, Long.class, Integer.class, Long.class, Long.class
		}
	)
	public void viewMyFeed() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		long time = ParamUtil.getLong(request, "time");
		long loopStreamAliasId = ParamUtil.getLong(
			request, "loopStreamAliasId",
			LoopStreamConstants.LOOP_STREAM_ID_FOLLOWING);
		boolean newAssetEntrySets = ParamUtil.getBoolean(
			request, "newAssetEntrySets");

		if (loopStreamAliasId ==
				LoopStreamConstants.LOOP_STREAM_ALIAS_ID_ANNOUNCEMENTS) {

			jsonArray = _getAnnouncementsJSONArray(
				time, LoopStreamConstants.getLoopStreamId(loopStreamAliasId),
				newAssetEntrySets, false);
		}
		else if (loopStreamAliasId ==
					LoopStreamConstants.
						LOOP_STREAM_ALIAS_ID_ANNOUNCEMENTS_STICKY) {

			jsonArray = _getAnnouncementsJSONArray(
				time, LoopStreamConstants.getLoopStreamId(loopStreamAliasId),
				newAssetEntrySets, true);
		}
		else if ((loopStreamAliasId ==
					LoopStreamConstants.LOOP_STREAM_ALIAS_ID_BOOKMARKS) ||
				 (loopStreamAliasId ==
					 LoopStreamConstants.LOOP_STREAM_ALIAS_ID_FOLLOWING)) {

			jsonArray = _getMyAssetEntrySetsJSONArray(
				time, LoopStreamConstants.getLoopStreamId(loopStreamAliasId),
				null, new ArrayList<Long>(), newAssetEntrySets);
		}
		else if (loopStreamAliasId ==
					LoopStreamConstants.LOOP_STREAM_ALIAS_ID_PRIVATE) {

			jsonArray = _getMyAssetEntrySetsJSONArray(
				time, LoopStreamConstants.getLoopStreamId(loopStreamAliasId),
				Boolean.TRUE, new ArrayList<Long>(), newAssetEntrySets);
		}

		respondWith(jsonArray);
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.RENDER_PHASE,
		parameterNames = {
			"childAssetEntrySetsLimit", "createTime", "end",
			"likedParticipantsLimit", "start", "stickyOnly"
		},
		parameterTypes = {
			Integer.class, Long.class, Integer.class, Integer.class,
			Integer.class, Boolean.class
		}
	)
	public void viewMyNewAnnouncements() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		long createTime = ParamUtil.getLong(request, "createTime");
		boolean stickyOnly = ParamUtil.getBoolean(request, "stickyOnly");

		respondWith(
			_getAnnouncementsJSONArray(
				createTime, LoopStreamConstants.LOOP_STREAM_ID_FOLLOWING, true,
				stickyOnly));
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.RENDER_PHASE,
		parameterNames = {
			"childAssetEntrySetsLimit", "end", "likedParticipantsLimit",
			"loopStreamId", "modifiedTime", "parentAssetEntrySetId", "start",
			"stickyTime"
		},
		parameterTypes = {
			Integer.class, Integer.class, Integer.class, Long.class, Long.class,
			Long.class, Integer.class, Long.class
		}
	)
	public void viewMyNewFeed() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		long loopStreamId = ParamUtil.getLong(request, "loopStreamId");
		long modifiedTime = ParamUtil.getLong(request, "modifiedTime");

		respondWith(
			_getMyAssetEntrySetsJSONArray(
				modifiedTime, loopStreamId, null, new ArrayList<Long>(), true));
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.RENDER_PHASE, methodName = "viewMyNewFeed",
		parameterNames = {
			"childAssetEntrySetsLimit", "end", "likedParticipantsLimit",
			"modifiedTime", "parentAssetEntrySetId", "start"
		},
		parameterTypes = {
			Integer.class, Integer.class, Integer.class, Long.class, Long.class,
			Integer.class
		}
	)
	public void viewMyNewFeed2() throws Exception {
		viewMyNewFeed();
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.RENDER_PHASE,
		parameterNames = {
			"childAssetEntrySetsLimit", "createTime", "end",
			"likedParticipantsLimit", "start", "stickyOnly"
		},
		parameterTypes = {
			Integer.class, Long.class, Integer.class, Integer.class,
			Integer.class, Boolean.class
		}
	)
	public void viewMyOldAnnouncements() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		long createTime = ParamUtil.getLong(request, "createTime");
		boolean stickyOnly = ParamUtil.getBoolean(request, "stickyOnly");

		respondWith(
			_getAnnouncementsJSONArray(
				createTime, LoopStreamConstants.LOOP_STREAM_ID_FOLLOWING, false,
				stickyOnly));
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.RENDER_PHASE,
		parameterNames = {
			"childAssetEntrySetsLimit", "end", "likedParticipantsLimit",
			"loopStreamId", "modifiedTime", "parentAssetEntrySetId", "start",
			"stickyTime"
		},
		parameterTypes = {
			Integer.class, Integer.class, Integer.class, Long.class, Long.class,
			Long.class, Integer.class, Long.class
		}
	)
	public void viewMyOldFeed() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		long loopStreamId = ParamUtil.getLong(request, "loopStreamId");
		long modifiedTime = ParamUtil.getLong(request, "modifiedTime");

		respondWith(
			_getMyAssetEntrySetsJSONArray(
				modifiedTime, loopStreamId, null, new ArrayList<Long>(),
				false));
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.RENDER_PHASE, methodName = "viewMyOldFeed",
		parameterNames = {
			"childAssetEntrySetsLimit", "end", "likedParticipantsLimit",
			"modifiedTime", "parentAssetEntrySetId", "start"
		},
		parameterTypes = {
			Integer.class, Integer.class, Integer.class, Long.class, Long.class,
			Integer.class
		}
	)
	public void viewMyOldFeed2() throws Exception {
		viewMyOldFeed();
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.RENDER_PHASE,
		parameterNames = {
			"createTime", "end", "parentAssetEntrySetId", "start"
		},
		parameterTypes = {Long.class, Integer.class, Long.class, Integer.class}
	)
	public void viewNewComments() throws Exception {
		_getChildAssetEntrySets(true);
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.RENDER_PHASE,
		parameterNames = {
			"childAssetEntrySetsLimit", "classNameId", "classPK", "createTime",
			"end", "likedParticipantsLimit", "parentAssetEntrySetId", "start",
			"stickyTime"
		},
		parameterTypes = {
			Integer.class, Long.class, Long.class, Long.class, Integer.class,
			Integer.class, Long.class, Integer.class, Long.class
		}
	)
	public void viewNewFeed() throws Exception {
		_getAssetEntrySets(true);
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.RENDER_PHASE, methodName = "viewNewFeed",
		parameterNames = {
			"childAssetEntrySetsLimit", "classNameId", "classPK", "createTime",
			"end", "likedParticipantsLimit", "parentAssetEntrySetId", "start"
		},
		parameterTypes = {
			Integer.class, Long.class, Long.class, Long.class, Integer.class,
			Integer.class, Long.class, Integer.class
		}
	)
	public void viewNewFeed2() throws Exception {
		viewNewFeed();
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.RENDER_PHASE,
		parameterNames = {
			"createTime", "end", "parentAssetEntrySetId", "start"
		},
		parameterTypes = {Long.class, Integer.class, Long.class, Integer.class}
	)
	public void viewOldComments() throws Exception {
		_getChildAssetEntrySets(false);
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.RENDER_PHASE,
		parameterNames = {
			"childAssetEntrySetsLimit", "classNameId", "classPK", "createTime",
			"end", "likedParticipantsLimit", "parentAssetEntrySetId", "start",
			"stickyTime"
		},
		parameterTypes = {
			Integer.class, Long.class, Long.class, Long.class, Integer.class,
			Integer.class, Long.class, Integer.class, Long.class
		}
	)
	public void viewOldFeed() throws Exception {
		_getAssetEntrySets(false);
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.RENDER_PHASE, methodName = "viewOldFeed",
		parameterNames = {
			"childAssetEntrySetsLimit", "classNameId", "classPK", "createTime",
			"end", "likedParticipantsLimit", "parentAssetEntrySetId", "start"
		},
		parameterTypes = {
			Integer.class, Long.class, Long.class, Long.class, Integer.class,
			Integer.class, Long.class, Integer.class
		}
	)
	public void viewOldFeed2() throws Exception {
		viewOldFeed();
	}

	@JSONWebServiceMethod(
		lifecycle = PortletRequest.RENDER_PHASE, parameterNames = "id",
		parameterTypes = Long.class
	)
	public void viewParent() throws Exception {
		AssetEntrySet assetEntrySet = _fetchAssetEntrySet();

		_validateViewParent(assetEntrySet);

		if (assetEntrySet.getParentAssetEntrySetId() > 0) {
			assetEntrySet = AssetEntrySetLocalServiceUtil.getAssetEntrySet(
				assetEntrySet.getParentAssetEntrySetId());
		}

		_view(assetEntrySet);
	}

	public void viewSharedTo() throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		AssetEntrySet assetEntrySet = _fetchAssetEntrySet();

		_validateViewSharedTo(assetEntrySet);

		JSONObject sharedToJSONObject = JSONFactoryUtil.createJSONObject();

		JSONArray divisionsSharedToJSONArray =
			JSONFactoryUtil.createJSONArray();
		JSONArray peopleSharedToJSONArray = JSONFactoryUtil.createJSONArray();
		JSONArray topicsSharedToJSONArray = JSONFactoryUtil.createJSONArray();

		JSONObject payloadJSONObject = JSONFactoryUtil.createJSONObject(
			assetEntrySet.getPayload());

		JSONArray sharedToJSONArray = AssetEntrySetUtil.getSharedToJSONArray(
			themeDisplay, payloadJSONObject);

		for (int i = 0; i < sharedToJSONArray.length(); i++) {
			JSONObject participantJSONObject = sharedToJSONArray.getJSONObject(
				i);

			long entityClassNameId = participantJSONObject.getLong(
				"entityClassNameId");

			if (entityClassNameId == PortalUtil.getClassNameId(
					LoopDivision.class)) {

				divisionsSharedToJSONArray.put(participantJSONObject);
			}
			else if (entityClassNameId == PortalUtil.getClassNameId(
						LoopPerson.class)) {

				peopleSharedToJSONArray.put(participantJSONObject);
			}
			else if (entityClassNameId == PortalUtil.getClassNameId(
						LoopTopic.class)) {

				topicsSharedToJSONArray.put(participantJSONObject);
			}
		}

		sharedToJSONObject.put("divisions", divisionsSharedToJSONArray);
		sharedToJSONObject.put("people", peopleSharedToJSONArray);
		sharedToJSONObject.put("topics", topicsSharedToJSONArray);

		respondWith(sharedToJSONObject);
	}

	@Override
	protected JSONObject getModelJSONObject(long classPK) throws Exception {
		AssetEntrySet assetEntrySet =
			AssetEntrySetLocalServiceUtil.getAssetEntrySet(classPK);

		int childAssetEntrySetsLimit = ParamUtil.getInteger(
			request, "childAssetEntrySetsLimit");
		int likedParticipantsLimit = ParamUtil.getInteger(
			request, "likedParticipantsLimit");

		return AssetEntrySetUtil.getAssetEntrySetJSONObject(
			themeDisplay, assetEntrySet, 0, childAssetEntrySetsLimit,
			likedParticipantsLimit);
	}

	private void _addAssetEntrySet(boolean currentUser) throws Exception {
		if (!isRespondingTo("json")) {
			return;
		}

		long parentAssetEntrySetId = ParamUtil.getLong(
			request, "parentAssetEntrySetId");

		long creatorClassNameId = 0;
		long creatorClassPK = 0;

		if (currentUser) {
			creatorClassNameId = PortalUtil.getClassNameId(LoopPerson.class);

			LoopPerson loopPerson = LoopPersonUtil.getLoopPerson(
				themeDisplay.getUserId());

			creatorClassPK = loopPerson.getLoopPersonId();
		}
		else {
			creatorClassNameId = ParamUtil.getLong(
				request, "creatorClassNameId");
			creatorClassPK = ParamUtil.getLong(request, "creatorClassPK");
		}

		String payload = ParamUtil.getString(request, "payload");

		JSONObject payloadJSONObject = JSONFactoryUtil.createJSONObject(
			payload);

		payloadJSONObject.put(
			"userAgent", request.getHeader(HttpHeaders.USER_AGENT));

		_processSharedTo(payloadJSONObject);

		boolean privateAssetEntrySet = ParamUtil.getBoolean(
			request, "privateAssetEntrySet");

		if (parentAssetEntrySetId > 0) {
			AssetEntrySet parentAssetEntrySet =
				AssetEntrySetLocalServiceUtil.getAssetEntrySet(
					parentAssetEntrySetId);

			privateAssetEntrySet = parentAssetEntrySet.isPrivateAssetEntrySet();
		}

		long stickyTime = ParamUtil.getLong(request, "stickyTime");
		String title = ParamUtil.getString(request, "title");
		int type = ParamUtil.getInteger(
			request, "type", LoopAssetEntrySetConstants.TYPE_POST);

		AssetEntrySet assetEntrySet = AssetEntrySetUtil.addAssetEntrySet(
			request, themeDisplay, parentAssetEntrySetId, 0, 0,
			creatorClassNameId, creatorClassPK, payloadJSONObject.toString(),
			privateAssetEntrySet, stickyTime, title, type,
			WorkflowConstants.STATUS_APPROVED);

		LoopStatsEntryUtil.updateScore(
			this, assetEntrySet,
			LoopWebConfigurationValues.LOOP_STATS_ENTRY_SCORE_VALUE_ADD_POST);

		_addLoopUserNotificationSubscription(assetEntrySet);

		alloyNotificationEventHelperPayloadJSONObject =
			JSONFactoryUtil.createJSONObject();

		alloyNotificationEventHelperPayloadJSONObject.put(
			"assetEntrySetId", assetEntrySet.getAssetEntrySetId());
		alloyNotificationEventHelperPayloadJSONObject.put(
			"sharedToJSONArray",
			_getSharedToJSONArray(assetEntrySet.getAssetEntrySetId()));

		respondWith(
			AssetEntrySetUtil.getAssetEntrySetJSONObject(
				themeDisplay, assetEntrySet, 0, 0,
				LoopConstants.ASSET_ENTRY_SET_LIKED_PARTICIPANTS_LIMIT));
	}

	private void _addChildLoopTopics(
			List<String> fullyFollowedEntities, long classNameId, long classPK)
		throws Exception {

		if (classNameId != PortalUtil.getClassNameId(LoopTopic.class)) {
			return;
		}

		AlloyServiceInvoker loopTopicAlloyServiceInvoker =
			new AlloyServiceInvoker(LoopTopic.class.getName());

		List<LoopTopic> loopTopics =
			loopTopicAlloyServiceInvoker.executeDynamicQuery(
				new Object[] {"parentLoopTopicId", classPK});

		for (LoopTopic loopTopic : loopTopics) {
			fullyFollowedEntities.add(
				AssetEntrySetParticipantInfoUtil.getSearchTerm(
					classNameId, loopTopic.getLoopTopicId()));
		}
	}

	private void _addFollowingConditions(
			List<String> partiallyFollowedEntities,
			List<String> fullyFollowedEntities,
			List<Long> excludeAssetEntrySetIds,
			List<Long> includeAssetEntrySetIds, long loopStreamId)
		throws Exception {

		LoopPerson loopPerson = LoopPersonUtil.getLoopPerson(
			themeDisplay.getUserId());

		List<LoopStreamEntry> loopStreamEntries =
			LoopStreamEntryUtil.getLoopStreamEntries(
				loopPerson.getLoopPersonId(), loopStreamId);

		for (LoopStreamEntry loopStreamEntry : loopStreamEntries) {
			if (loopStreamEntry.getClassNameId() == PortalUtil.getClassNameId(
					AssetEntrySet.class)) {

				if (loopStreamEntry.isFollowing()) {
					includeAssetEntrySetIds.add(loopStreamEntry.getClassPK());
				}
				else {
					excludeAssetEntrySetIds.add(loopStreamEntry.getClassPK());
				}
			}
			else if (loopStreamEntry.getFollowingType() ==
						LoopStreamEntryConstants.TYPE_FOLLOWING_FULL) {

				fullyFollowedEntities.add(
					AssetEntrySetParticipantInfoUtil.getSearchTerm(
						loopStreamEntry.getClassNameId(),
						loopStreamEntry.getClassPK()));

				_addChildLoopTopics(
					fullyFollowedEntities, loopStreamEntry.getClassNameId(),
					loopStreamEntry.getClassPK());
			}
			else if (loopStreamEntry.getFollowingType() ==
						LoopStreamEntryConstants.TYPE_FOLLOWING_LIMITED) {

				partiallyFollowedEntities.add(
					AssetEntrySetParticipantInfoUtil.getSearchTerm(
						loopStreamEntry.getClassNameId(),
						loopStreamEntry.getClassPK()));
			}
		}

		if (loopStreamId == LoopStreamConstants.LOOP_STREAM_ID_FOLLOWING) {
			fullyFollowedEntities.add(
				AssetEntrySetParticipantInfoUtil.getSearchTerm(
					PortalUtil.getClassNameId(LoopPerson.class),
					loopPerson.getLoopPersonId()));
		}
	}

	private void _addLoopUserNotificationSubscription(
			AssetEntrySet assetEntrySet)
		throws Exception {

		LoopPerson loopPerson =
			LoopPersonLocalServiceUtil.getLoopPersonByPersonUserId(
				themeDisplay.getUserId());

		if (assetEntrySet.getLevel() == 0) {
			LoopUserNotificationSubscriptionUtil.
				updateLoopUserNotificationSubscriptions(
					this, loopPerson,
					PortalUtil.getClassNameId(AssetEntrySet.class),
					assetEntrySet.getAssetEntrySetId(),
					LoopUserNotificationConstants.
						SETTING_KEY_COMMENTS_ON_MY_POSTS);
		}
		else if (assetEntrySet.getLevel() == 1) {
			LoopUserNotificationSubscriptionUtil.
				updateLoopUserNotificationSubscriptions(
					this, loopPerson,
					PortalUtil.getClassNameId(AssetEntrySet.class),
					assetEntrySet.getParentAssetEntrySetId(),
					LoopUserNotificationConstants.
						SETTING_KEY_POSTS_I_COMMENTED_ON);
			LoopUserNotificationSubscriptionUtil.
				updateLoopUserNotificationSubscriptions(
					this, loopPerson,
					PortalUtil.getClassNameId(AssetEntrySet.class),
					assetEntrySet.getAssetEntrySetId(),
					LoopUserNotificationConstants.
						SETTING_KEY_REPLIES_TO_MY_COMMENTS);
		}
	}

	private AssetEntrySet _fetchAssetEntrySet() {
		long assetEntrySetId = ParamUtil.getLong(request, "id");

		return AssetEntrySetLocalServiceUtil.fetchAssetEntrySet(
			assetEntrySetId);
	}

	private JSONArray _getAnnouncementsJSONArray(
			long createTime, long loopStreamId, boolean newAssetEntrySets,
			boolean stickyOnly)
		throws Exception {

		JSONArray resultsJSONArray = JSONFactoryUtil.createJSONArray();

		long stickyTime = 0;

		if (stickyOnly) {
			stickyTime = System.currentTimeMillis();
		}

		List<String> partiallyFollowedEntities = new ArrayList<>();
		List<String> fullyFollowedEntities = new ArrayList<>();
		List<Long> excludeAssetEntrySetIds = new ArrayList<>();
		List<Long> includeAssetEntrySetIds = new ArrayList<>();

		_addFollowingConditions(
			partiallyFollowedEntities, fullyFollowedEntities,
			excludeAssetEntrySetIds, includeAssetEntrySetIds, loopStreamId);

		if (ListUtil.isNotEmpty(partiallyFollowedEntities) ||
			ListUtil.isNotEmpty(fullyFollowedEntities) ||
			ListUtil.isNotEmpty(includeAssetEntrySetIds)) {

			Map<String, Serializable> attributes = _getAttributes(
				createTime, "createTime", stickyTime,
				StringPool.GREATER_THAN_OR_EQUAL, newAssetEntrySets, null,
				partiallyFollowedEntities, fullyFollowedEntities,
				excludeAssetEntrySetIds, includeAssetEntrySetIds);

			attributes.put(
				Field.TYPE, LoopAssetEntrySetConstants.TYPE_ANNOUNCEMENT);

			Sort[] sorts = {
				new Sort("createTime_sortable", Sort.LONG_TYPE, true)
			};

			JSONObject jsonObject = doSearch(
				_assetEntrySetIndexer, alloyServiceInvoker, attributes, sorts);

			resultsJSONArray = jsonObject.getJSONArray("results");
		}

		return resultsJSONArray;
	}

	private void _getAssetEntrySets(boolean newAssetEntrySets)
		throws Exception {

		if (!isRespondingTo("json")) {
			return;
		}

		respondWith(
			_getAssetEntrySetsJSONArray(
				newAssetEntrySets, new ArrayList<Long>()));
	}

	private JSONArray _getAssetEntrySetsJSONArray(
			boolean newAssetEntrySets, List<Long> excludeAssetEntrySetIds)
		throws Exception {

		long createTime = ParamUtil.getLong(request, "createTime");

		List<String> fullyFollowedEntities = new ArrayList<>();

		long classNameId = ParamUtil.getLong(request, "classNameId");
		long classPK = ParamUtil.getLong(request, "classPK");

		fullyFollowedEntities.add(
			AssetEntrySetParticipantInfoUtil.getSearchTerm(
				classNameId, classPK));

		_addChildLoopTopics(fullyFollowedEntities, classNameId, classPK);

		return _getAssetEntrySetsJSONArray(
			createTime, "createTime", null, null, fullyFollowedEntities,
			excludeAssetEntrySetIds, null, newAssetEntrySets);
	}

	private JSONArray _getAssetEntrySetsJSONArray(
			long time, String timeFieldName, Boolean privateAssetEntrySet,
			List<String> partiallyFollowedEntities,
			List<String> fullyFollowedEntities,
			List<Long> excludeAssetEntrySetIds,
			List<Long> includeAssetEntrySetIds, boolean newAssetEntrySets)
		throws Exception {

		long stickyTime = ParamUtil.getLong(
			request, "stickyTime", Long.MAX_VALUE);

		Map<String, Serializable> attributes = _getAttributes(
			time, timeFieldName, stickyTime, StringPool.LESS_THAN,
			newAssetEntrySets, privateAssetEntrySet, partiallyFollowedEntities,
			fullyFollowedEntities, excludeAssetEntrySetIds,
			includeAssetEntrySetIds);

		Sort[] sorts = {
			new Sort(timeFieldName + "_sortable", Sort.LONG_TYPE, true)
		};

		JSONObject jsonObject = doSearch(
			_assetEntrySetIndexer, alloyServiceInvoker, attributes, sorts);

		return jsonObject.getJSONArray("results");
	}

	private Map<String, Serializable> _getAttributes(
		long time, String timeFieldName, boolean newAssetEntrySets,
		Boolean privateAssetEntrySet, List<String> fullyFollowedEntities) {

		Map<String, Serializable> attributes = new HashMap<>();

		String timeComparator = StringPool.BLANK;

		if (newAssetEntrySets) {
			timeComparator = StringPool.GREATER_THAN;
		}
		else {
			timeComparator = StringPool.LESS_THAN_OR_EQUAL;
		}

		attributes.put(timeFieldName + "_sortable", time);
		attributes.put(timeFieldName + "Comparator", timeComparator);

		attributes.put(
			"excludeTypes",
			new String[] {
				String.valueOf(LoopAssetEntrySetConstants.TYPE_PAGE)
			});
		attributes.put("privateAssetEntrySet", privateAssetEntrySet);

		if (ListUtil.isNotEmpty(fullyFollowedEntities)) {
			attributes.put(
				"sharedTo",
				ArrayUtil.toStringArray(fullyFollowedEntities.toArray()));
		}

		return attributes;
	}

	private Map<String, Serializable> _getAttributes(
		long time, String timeFieldName, long stickyTime,
		String stickyTimeComparator, boolean newAssetEntrySets,
		Boolean privateAssetEntrySet, List<String> partiallyFollowedEntities,
		List<String> fullyFollowedEntities, List<Long> excludeAssetEntrySetIds,
		List<Long> includeAssetEntrySetIds) {

		Map<String, Serializable> attributes = _getAttributes(
			time, timeFieldName, newAssetEntrySets, privateAssetEntrySet,
			fullyFollowedEntities);

		if (ListUtil.isNotEmpty(partiallyFollowedEntities)) {
			attributes.put(
				"creators",
				ArrayUtil.toStringArray(partiallyFollowedEntities.toArray()));
		}

		if (ListUtil.isNotEmpty(excludeAssetEntrySetIds)) {
			attributes.put(
				"excludeAssetEntrySetIds",
				ArrayUtil.toStringArray(excludeAssetEntrySetIds.toArray()));
		}

		if (ListUtil.isNotEmpty(includeAssetEntrySetIds)) {
			attributes.put(
				"includeAssetEntrySetIds",
				ArrayUtil.toStringArray(includeAssetEntrySetIds.toArray()));
		}

		attributes.put("stickyTime_sortable", stickyTime);
		attributes.put("stickyTimeComparator", stickyTimeComparator);

		return attributes;
	}

	private void _getChildAssetEntrySets(boolean newAssetEntrySets)
		throws Exception {

		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		List<AssetEntrySet> assetEntrySets = new ArrayList<>();

		long createTime = ParamUtil.getLong(request, "createTime");
		long parentAssetEntrySetId = ParamUtil.getLong(
			request, "parentAssetEntrySetId");
		int start = ParamUtil.getInteger(request, "start");

		int end = ParamUtil.getInteger(request, "end");

		if (end == 0) {
			end = LoopWebConfigurationValues.LOOP_PAGE_DEFAULT_DELTA;
		}

		if (newAssetEntrySets) {
			assetEntrySets = AssetEntrySetUtil.getNewChildAssetEntrySets(
				createTime, parentAssetEntrySetId, false, start, end);
		}
		else {
			assetEntrySets = AssetEntrySetUtil.getOldChildAssetEntrySets(
				createTime, parentAssetEntrySetId, start, end);
		}

		for (AssetEntrySet assetEntrySet : assetEntrySets) {
			jsonArray.put(
				AssetEntrySetUtil.getAssetEntrySetJSONObject(
					themeDisplay, assetEntrySet, 0, 0, 0));
		}

		if (getAPIVersion() < 6) {
			respondWith(jsonArray);

			return;
		}

		JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

		jsonObject.put("results", jsonArray);
		jsonObject.put(
			"total",
			AssetEntrySetLocalServiceUtil.getChildAssetEntrySetsCount(
				parentAssetEntrySetId));

		respondWith(jsonObject);
	}

	private JSONObject _getEntityJSONObject(
		long entityClassNameId, long entityClassPK) {

		JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

		jsonObject.put("entityClassNameId", entityClassNameId);
		jsonObject.put("entityClassPK", entityClassPK);

		return jsonObject;
	}

	private JSONObject _getFollowingJSONObject(
			long classNameId, long classPK, long loopPersonId,
			boolean previouslyFollowing, boolean following, int followingType)
		throws Exception {

		JSONObject followingJSONObject = JSONFactoryUtil.createJSONObject();

		int followersCount = LoopStreamEntryUtil.getFollowersCount(
			classNameId, classPK);

		if (previouslyFollowing != following) {
			if (following) {
				followersCount++;
			}
			else {
				followersCount--;
			}
		}

		followingJSONObject.put("followersCount", followersCount);

		followingJSONObject.put("following", following);
		followingJSONObject.put("followingType", followingType);
		followingJSONObject.put(
			"notifying",
			LoopUserNotificationSubscriptionUtil.isNotifying(
				loopPersonId, classNameId, classPK,
				LoopUserNotificationConstants.DELIVERY_TYPE_IN_APP));
		followingJSONObject.put(
			"notifyingEmail",
			LoopUserNotificationSubscriptionUtil.isNotifying(
				loopPersonId, classNameId, classPK,
				LoopUserNotificationConstants.DELIVERY_TYPE_EMAIL));

		return followingJSONObject;
	}

	private JSONArray _getMyAssetEntrySetsJSONArray(
			long modifiedTime, long loopStreamId, Boolean privateAssetEntrySet,
			List<Long> excludeAssetEntrySetIds, boolean newAssetEntrySets)
		throws Exception {

		List<String> partiallyFollowedEntities = new ArrayList<>();
		List<String> fullyFollowedEntities = new ArrayList<>();
		List<Long> includeAssetEntrySetIds = new ArrayList<>();

		_addFollowingConditions(
			partiallyFollowedEntities, fullyFollowedEntities,
			excludeAssetEntrySetIds, includeAssetEntrySetIds, loopStreamId);

		if (ListUtil.isNotEmpty(partiallyFollowedEntities) ||
			ListUtil.isNotEmpty(fullyFollowedEntities) ||
			ListUtil.isNotEmpty(includeAssetEntrySetIds)) {

			return _getAssetEntrySetsJSONArray(
				modifiedTime, "modifiedTime", privateAssetEntrySet,
				partiallyFollowedEntities, fullyFollowedEntities,
				excludeAssetEntrySetIds, includeAssetEntrySetIds,
				newAssetEntrySets);
		}

		return JSONFactoryUtil.createJSONArray();
	}

	private JSONObject _getRefreshJSONObject(boolean refreshMyFeed)
		throws Exception {

		JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

		JSONArray deletedJSONArray = JSONFactoryUtil.createJSONArray();
		JSONArray updatedJSONArray = JSONFactoryUtil.createJSONArray();
		int childAssetEntrySetsLimit = ParamUtil.getInteger(
			request, "childAssetEntrySetsLimit");
		int likedParticipantsLimit = ParamUtil.getInteger(
			request, "likedParticipantsLimit");
		List<Long> excludeAssetEntrySetIds = new ArrayList<>();

		String payload = ParamUtil.getString(request, "payload");

		JSONArray payloadJSONArray = JSONFactoryUtil.createJSONArray(payload);

		for (int i = 0; i < payloadJSONArray.length(); i++) {
			JSONObject assetEntrySetJSONObject = payloadJSONArray.getJSONObject(
				i);

			long assetEntrySetId = assetEntrySetJSONObject.getLong(
				"assetEntrySetId");

			AssetEntrySet assetEntrySet =
				AssetEntrySetLocalServiceUtil.fetchAssetEntrySet(
					assetEntrySetId);

			if (assetEntrySet == null) {
				deletedJSONArray.put(assetEntrySetId);
			}
			else if (_hasChanges(assetEntrySetJSONObject, assetEntrySet)) {
				updatedJSONArray.put(
					AssetEntrySetUtil.getAssetEntrySetJSONObject(
						themeDisplay, assetEntrySet, 0,
						childAssetEntrySetsLimit, likedParticipantsLimit));
			}

			excludeAssetEntrySetIds.add(assetEntrySetId);
		}

		jsonObject.put("deleted", deletedJSONArray);
		jsonObject.put("updated", updatedJSONArray);

		if (refreshMyFeed) {
			long modifiedTime = ParamUtil.getLong(request, "modifiedTime");
			long loopStreamId = ParamUtil.getLong(request, "loopStreamId");

			jsonObject.put(
				"new",
				_getMyAssetEntrySetsJSONArray(
					modifiedTime, loopStreamId, null, excludeAssetEntrySetIds,
					true));
		}
		else {
			jsonObject.put(
				"new",
				_getAssetEntrySetsJSONArray(true, excludeAssetEntrySetIds));
		}

		return jsonObject;
	}

	private AssetEntrySet _getRootAssetEntrySetTree(AssetEntrySet assetEntrySet)
		throws Exception {

		while (assetEntrySet.getParentAssetEntrySetId() > 0) {
			List<AssetEntrySet> assetEntrySets = new ArrayList<>();

			assetEntrySets.add(assetEntrySet);

			assetEntrySets.addAll(
				AssetEntrySetUtil.getNewChildAssetEntrySets(
					assetEntrySet.getCreateTime(),
					assetEntrySet.getParentAssetEntrySetId(), true, 0,
					LoopWebConfigurationValues.LOOP_CHILD_POSTS_DISPLAY_MAX));

			AssetEntrySet parentAssetEntrySet =
				AssetEntrySetLocalServiceUtil.getAssetEntrySet(
					assetEntrySet.getParentAssetEntrySetId());

			parentAssetEntrySet.setChildAssetEntrySets(assetEntrySets);

			assetEntrySet = parentAssetEntrySet;
		}

		return assetEntrySet;
	}

	private JSONArray _getSharedToJSONArray(long assetEntrySetId) {
		JSONArray sharedToJSONArray = JSONFactoryUtil.createJSONArray();

		List<AssetSharingEntry> assetSharingEntries =
			AssetSharingEntryLocalServiceUtil.getAssetSharingEntries(
				PortalUtil.getClassNameId(AssetEntrySet.class),
				assetEntrySetId);

		for (AssetSharingEntry assetSharingEntry : assetSharingEntries) {
			sharedToJSONArray.put(
				_getEntityJSONObject(
					assetSharingEntry.getSharedToClassNameId(),
					assetSharingEntry.getSharedToClassPK()));
		}

		return sharedToJSONArray;
	}

	private boolean _hasChanges(
			JSONObject jsonObject, AssetEntrySet assetEntrySet)
		throws Exception {

		if (jsonObject.getLong("modifiedTime") !=
				assetEntrySet.getModifiedTime()) {

			return true;
		}

		if (jsonObject.getInt("assetEntrySetLikesCount") !=
				AssetEntrySetLikeLocalServiceUtil.getAssetEntrySetLikeCount(
					assetEntrySet.getAssetEntrySetId())) {

			return true;
		}

		boolean bookmarked = jsonObject.getBoolean("bookmarked");

		LoopPerson loopPerson = LoopPersonUtil.getLoopPerson(
			themeDisplay.getUserId());

		LoopStreamEntry loopStreamEntry =
			LoopStreamEntryUtil.fetchLoopStreamEntry(
				loopPerson.getLoopPersonId(),
				LoopStreamConstants.LOOP_STREAM_ID_BOOKMARKS,
				PortalUtil.getClassNameId(AssetEntrySet.class),
				assetEntrySet.getAssetEntrySetId());

		if ((bookmarked && (loopStreamEntry == null)) ||
			(!bookmarked && (loopStreamEntry != null))) {

			return true;
		}

		return false;
	}

	private void _processSharedTo(JSONObject payloadJSONObject) {
		if (getAPIVersion() < 8) {
			JSONArray sharedToJSONArray = payloadJSONObject.getJSONArray(
				LoopAssetEntrySetConstants.PAYLOAD_KEY_SHARED_TO);

			for (int i = 0; i < sharedToJSONArray.length(); i++) {
				JSONObject jsonObject = sharedToJSONArray.getJSONObject(i);

				jsonObject.put(
					"entityClassNameId", jsonObject.remove("classNameId"));
				jsonObject.put("entityClassPK", jsonObject.remove("classPK"));
			}
		}
	}

	private void _validateAddAnnouncement() throws Exception {
		int type = ParamUtil.getInteger(request, "type");

		if (type == LoopAssetEntrySetConstants.TYPE_ANNOUNCEMENT) {
			LoopPerson loopPerson = LoopPersonUtil.getLoopPerson(
				themeDisplay.getUserId());

			LoopPersonComposite loopPersonComposite = new LoopPersonComposite(
				loopPerson, themeDisplay);

			if (!loopPersonComposite.getPermissionAddAnnouncement()) {
				throw new AlloyException(
					"you-do-not-have-permission-to-add-announcements");
			}
		}
	}

	private void _validateAddMyPost() throws Exception {
		_validateAddAnnouncement();
		_validateParentAssetEntrySetId();
		_validatePayload();
		_validateTitle();
		_validateType();
	}

	private void _validateAddPost() throws Exception {
		long creatorClassNameId = ParamUtil.getLong(
			request, "creatorClassNameId");
		long creatorClassPK = ParamUtil.getLong(request, "creatorClassPK");

		LoopPerson loopPerson = LoopPersonUtil.getLoopPerson(
			themeDisplay.getUserId());

		if (creatorClassNameId == PortalUtil.getClassNameId(
				LoopDivision.class)) {

			if (!LoopParticipantAssignmentUtil.isLoopDivisionLead(
					creatorClassPK, loopPerson.getLoopPersonId())) {

				throw new AlloyException(
					"you-do-not-have-permission-to-access-the-requested-" +
						"resource");
			}
		}
		else if (creatorClassNameId == PortalUtil.getClassNameId(
					LoopPerson.class)) {

			if (creatorClassPK != loopPerson.getLoopPersonId()) {
				throw new AlloyException(
					"you-do-not-have-permission-to-access-the-requested-" +
						"resource");
			}
		}
		else {
			throw new AlloyException(
				"you-do-not-have-permission-to-access-the-requested-resource");
		}

		_validateAddMyPost();
	}

	private void _validateAddToStream() throws Exception {
		_validateClassNameIdClassPK();

		long classNameId = ParamUtil.getLong(request, "classNameId");
		long classPK = ParamUtil.getLong(request, "classPK");
		long loopStreamId = ParamUtil.getLong(request, "loopStreamId");

		if ((loopStreamId == LoopStreamConstants.LOOP_STREAM_ID_BOOKMARKS) &&
			(classNameId != PortalUtil.getClassNameId(AssetEntrySet.class))) {

			throw new AlloyException(
				"you-can-only-add-posts-to-the-bookmarks-stream");
		}

		int followingType = ParamUtil.getInteger(
			request, "followingType",
			LoopStreamEntryConstants.TYPE_FOLLOWING_FULL);

		if ((classNameId != PortalUtil.getClassNameId(LoopPerson.class)) &&
			(followingType ==
				LoopStreamEntryConstants.TYPE_FOLLOWING_LIMITED)) {

			throw new AlloyException("you-can-only-limit-following-for-people");
		}

		if (classNameId == PortalUtil.getClassNameId(AssetEntrySet.class)) {
			AssetEntrySet assetEntrySet =
				AssetEntrySetLocalServiceUtil.getAssetEntrySet(classPK);

			if (assetEntrySet.getLevel() != 0) {
				throw new AlloyException("you-can-only-follow-top-level-posts");
			}
		}
	}

	private void _validateAssetEntrySet(AssetEntrySet assetEntrySet)
		throws Exception {

		if ((assetEntrySet == null) ||
			!ArrayUtil.contains(
				LoopAssetEntrySetConstants.TYPES_ELIGIBLE,
				assetEntrySet.getType())) {

			throw new AlloyException("the-feed-does-not-exist");
		}
	}

	private void _validateClassNameIdClassPK() throws Exception {
		long classNameId = ParamUtil.getLong(request, "classNameId");
		long classPK = ParamUtil.getLong(request, "classPK");

		if ((classNameId == 0) || (classPK == 0)) {
			throw new AlloyException("the-entity-does-not-exist");
		}
	}

	private void _validateDelete(AssetEntrySet assetEntrySet) throws Exception {
		_validateAssetEntrySet(assetEntrySet);
	}

	private void _validateDisableNotifications() throws Exception {
		_validateClassNameIdClassPK();
	}

	private void _validateEnableNotifications() throws Exception {
		_validateClassNameIdClassPK();
	}

	private void _validateExternalReferenceFeed() throws Exception {
		String externalReferenceName = ParamUtil.getString(
			request, "externalReferenceName");

		if (Validator.isNull(externalReferenceName)) {
			throw new AlloyException("the-external-system-name-is-not-set");
		}

		String externalReferencePK = ParamUtil.getString(
			request, "externalReferencePK");

		if (Validator.isNull(externalReferencePK)) {
			throw new AlloyException(
				"the-external-reference-primary-key-is-not-set");
		}

		LoopExternalReferenceRel loopExternalReferenceRel =
			LoopExternalReferenceRelUtil.fetchLoopExternalReferenceRel(
				externalReferenceName, externalReferencePK);

		if (loopExternalReferenceRel == null) {
			throw new AlloyException("the-external-reference-does-not-exist");
		}
	}

	private void _validateLike(AssetEntrySet assetEntrySet) throws Exception {
		_validateAssetEntrySet(assetEntrySet);
	}

	private void _validateParentAssetEntrySetId() throws Exception {
		long parentAssetEntrySetId = ParamUtil.getLong(
			request, "parentAssetEntrySetId");

		if (parentAssetEntrySetId == 0) {
			return;
		}

		AssetEntrySet parentAssetEntrySet =
			AssetEntrySetLocalServiceUtil.getAssetEntrySet(
				parentAssetEntrySetId);

		if (!LoopUtil.isSharedWithUser(
				parentAssetEntrySet, themeDisplay.getUserId())) {

			throw new AlloyException("the-feed-does-not-exist");
		}

		if (parentAssetEntrySet.getParentAssetEntrySetId() == 0) {
			return;
		}

		parentAssetEntrySet = AssetEntrySetLocalServiceUtil.getAssetEntrySet(
			parentAssetEntrySet.getParentAssetEntrySetId());

		if (parentAssetEntrySet.getParentAssetEntrySetId() > 0) {
			throw new AlloyException("you-cannot-reply-to-replies");
		}
	}

	private void _validatePayload() throws Exception {
		String payload = ParamUtil.getString(request, "payload");

		JSONObject payloadJSONObject = JSONFactoryUtil.createJSONObject(
			payload);

		JSONArray imageDataJSONArray = payloadJSONObject.getJSONArray(
			"imageData");

		if ((imageDataJSONArray == null) &&
			StringUtil.equalsIgnoreCase(
				payloadJSONObject.getString("type"),
				LoopConstants.PAYLOAD_TYPE_IMAGE)) {

			throw new AlloyException("the-post-requires-an-image");
		}

		if (imageDataJSONArray != null) {
			if (imageDataJSONArray.length() >
					LoopWebConfigurationValues.LOOP_FILE_UPLOADER_COUNT_MAX) {

				throw new AlloyException(
					translate(
						"max-file-count-exceeded-x",
						LoopWebConfigurationValues.
							LOOP_FILE_UPLOADER_COUNT_MAX));
			}

			Set<Long> fileEntryIds = new HashSet<>();

			for (int i = 0; i < imageDataJSONArray.length(); i++) {
				JSONObject imageJSONObject = imageDataJSONArray.getJSONObject(
					i);

				Iterator<String> iterator = imageJSONObject.keys();

				while (iterator.hasNext()) {
					String key = iterator.next();

					long fileEntryId = imageJSONObject.getLong(key);

					if (!fileEntryIds.add(fileEntryId)) {
						throw new AlloyException(
							"you-cannot-add-duplicate-image-data");
					}
				}
			}
		}

		String message = payloadJSONObject.getString("message");

		byte[] bytes = message.getBytes("UTF-8");

		for (byte b : bytes) {
			BitSet bitSet = BitSet.valueOf(new byte[] {b});

			int clearBitIndex = bitSet.previousClearBit(Byte.SIZE - 1);

			if (clearBitIndex <= _MAX_BYTES) {
				throw new AlloyException(
					"the-post-contains-unsupported-characters");
			}
		}
	}

	private void _validateRemoveFromStream() throws Exception {
		_validateClassNameIdClassPK();

		long classNameId = ParamUtil.getLong(request, "classNameId");
		long classPK = ParamUtil.getLong(request, "classPK");

		if (classNameId == PortalUtil.getClassNameId(AssetEntrySet.class)) {
			AssetEntrySet assetEntrySet =
				AssetEntrySetLocalServiceUtil.getAssetEntrySet(classPK);

			if (assetEntrySet.getLevel() != 0) {
				throw new AlloyException(
					"you-can-only-unfollow-top-level-posts");
			}
		}
	}

	private void _validateTitle() throws Exception {
		int type = ParamUtil.getInteger(request, "type");
		String title = ParamUtil.getString(request, "title");

		if (((type == LoopAssetEntrySetConstants.TYPE_ANNOUNCEMENT) &&
			 Validator.isNull(title)) ||
			((type != LoopAssetEntrySetConstants.TYPE_ANNOUNCEMENT) &&
			 Validator.isNotNull(title))) {

			throw new AlloyException("the-title-is-invalid");
		}
	}

	private void _validateType() throws Exception {
		int type = ParamUtil.getInteger(request, "type");

		if (!ArrayUtil.contains(
				LoopAssetEntrySetConstants.TYPES_ELIGIBLE, type)) {

			throw new AlloyException("the-type-is-invalid");
		}
	}

	private void _validateUnlike(AssetEntrySet assetEntrySet) throws Exception {
		_validateAssetEntrySet(assetEntrySet);
	}

	private void _validateUpdate(AssetEntrySet assetEntrySet) throws Exception {
		_validateAssetEntrySet(assetEntrySet);
		_validateAddAnnouncement();
		_validatePayload();
		_validateTitle();
		_validateType();
	}

	private void _validateView(AssetEntrySet assetEntrySet) throws Exception {
		_validateAssetEntrySet(assetEntrySet);

		if (!LoopUtil.isSharedWithUser(
				assetEntrySet, themeDisplay.getUserId())) {

			throw new AlloyException("the-feed-does-not-exist");
		}
	}

	private void _validateViewFollowers() throws Exception {
		_validateClassNameIdClassPK();
	}

	private void _validateViewLikedParticipants(AssetEntrySet assetEntrySet)
		throws Exception {

		_validateAssetEntrySet(assetEntrySet);
	}

	private void _validateViewParent(AssetEntrySet assetEntrySet)
		throws Exception {

		_validateView(assetEntrySet);
	}

	private void _validateViewSharedTo(AssetEntrySet assetEntrySet)
		throws Exception {

		_validateAssetEntrySet(assetEntrySet);
	}

	private void _view(AssetEntrySet assetEntrySet) throws Exception {
		long id = ParamUtil.getLong(request, "id");

		List<LoopUserNotificationEvent> loopUserNotificationEvents =
			LoopUserNotificationEventUtil.getLoopUserNotificationEvents(
				themeDisplay.getUserId(),
				PortalUtil.getClassNameId(AssetEntrySet.class), id);

		for (LoopUserNotificationEvent loopUserNotificationEvent :
				loopUserNotificationEvents) {

			LoopUserNotificationEventUtil.setOpened(
				themeDisplay.getUserId(),
				loopUserNotificationEvent.getGroupKey());
		}

		JSONObject assetEntrySetJSONObject = JSONFactoryUtil.createJSONObject();

		if (LoopHttpServletRequestUtil.isMobileRequest(
				request.getHeader(HttpHeaders.USER_AGENT)) ||
			(assetEntrySet.getParentAssetEntrySetId() == 0)) {

			assetEntrySetJSONObject =
				AssetEntrySetUtil.getAssetEntrySetJSONObject(
					themeDisplay, assetEntrySet,
					assetEntrySet.getAssetEntrySetId(),
					LoopWebConfigurationValues.LOOP_PAGE_DEFAULT_DELTA,
					LoopConstants.ASSET_ENTRY_SET_LIKED_PARTICIPANTS_LIMIT);
		}
		else {
			AssetEntrySet rootAssetEntrySet = _getRootAssetEntrySetTree(
				assetEntrySet);

			assetEntrySetJSONObject =
				AssetEntrySetUtil.getAssetEntrySetJSONObject(
					themeDisplay, rootAssetEntrySet,
					assetEntrySet.getAssetEntrySetId(), 0,
					LoopConstants.ASSET_ENTRY_SET_LIKED_PARTICIPANTS_LIMIT);
		}

		respondWith(assetEntrySetJSONObject);
	}

	private static final int _MAX_BYTES = 3;

	private static final Indexer _assetEntrySetIndexer =
		IndexerRegistryUtil.nullSafeGetIndexer(AssetEntrySet.class);

}