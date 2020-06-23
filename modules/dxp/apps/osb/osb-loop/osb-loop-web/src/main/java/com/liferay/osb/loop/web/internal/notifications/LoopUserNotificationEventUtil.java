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

package com.liferay.osb.loop.web.internal.notifications;

import com.liferay.alloy.mvc.AlloyServiceInvoker;
import com.liferay.osb.loop.asset.entry.set.model.AssetEntrySet;
import com.liferay.osb.loop.asset.entry.set.service.AssetEntrySetLocalServiceUtil;
import com.liferay.osb.loop.asset.entry.set.util.AssetEntrySetParticipantInfoUtil;
import com.liferay.osb.loop.constants.LoopConstants;
import com.liferay.osb.loop.model.LoopDivision;
import com.liferay.osb.loop.model.LoopPerson;
import com.liferay.osb.loop.model.LoopTopic;
import com.liferay.osb.loop.model.LoopUserNotificationEvent;
import com.liferay.osb.loop.model.LoopUserNotificationSubscription;
import com.liferay.osb.loop.model.impl.LoopUserNotificationEventModelImpl;
import com.liferay.osb.loop.service.LoopDivisionLocalServiceUtil;
import com.liferay.osb.loop.service.LoopPersonLocalServiceUtil;
import com.liferay.osb.loop.service.LoopUserNotificationEventLocalServiceUtil;
import com.liferay.osb.loop.service.LoopUserNotificationSubscriptionLocalServiceUtil;
import com.liferay.osb.loop.web.internal.constants.LoopAssetEntrySetConstants;
import com.liferay.osb.loop.web.internal.constants.LoopDivisionConstants;
import com.liferay.osb.loop.web.internal.constants.LoopUserNotificationConstants;
import com.liferay.osb.loop.web.internal.util.LoopMarkdownUtil;
import com.liferay.osb.loop.web.internal.util.LoopPersonUtil;
import com.liferay.osb.loop.web.internal.util.LoopSQLUtil;
import com.liferay.osb.loop.web.internal.util.LoopUtil;
import com.liferay.petra.string.CharPool;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.Projection;
import com.liferay.portal.kernel.dao.orm.ProjectionFactoryUtil;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.OrganizationLocalServiceUtil;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.OrderByComparatorFactoryUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

/**
 * @author Sherry Yang
 * @author Timothy Bell
 */
public class LoopUserNotificationEventUtil {

	public static void deleteLoopUserNotificationEvents(
			List<LoopUserNotificationEvent> loopUserNotificationEvents)
		throws Exception {

		Set<Long> userIds = new HashSet<>();

		for (LoopUserNotificationEvent loopUserNotificationEvent :
				loopUserNotificationEvents) {

			LoopUserNotificationEventLocalServiceUtil.
				deleteLoopUserNotificationEvent(loopUserNotificationEvent);

			userIds.add(loopUserNotificationEvent.getRecipientUserId());
		}

		for (long userId : userIds) {
			LoopPersonUtil.updateGroupedUserNotificationEventsCount(userId);
		}
	}

	public static void deleteLoopUserNotificationEvents(Object[] properties)
		throws Exception {

		List<LoopUserNotificationEvent> loopUserNotificationEvents =
			_alloyServiceInvoker.executeDynamicQuery(properties);

		deleteLoopUserNotificationEvents(loopUserNotificationEvents);
	}

	public static void filterInactiveUsers(Set<Long> userIds) {
		Iterator<Long> iterator = userIds.iterator();

		while (iterator.hasNext()) {
			long userId = iterator.next();

			User user = UserLocalServiceUtil.fetchUser(userId);

			if ((user == null) ||
				(user.getStatus() == WorkflowConstants.STATUS_INACTIVE)) {

				iterator.remove();
			}
		}
	}

	public static void filterUsers(
		long curUserId, Set<Long> recipientUserIds,
		Set<Long> sentRecipientUserIds) {

		recipientUserIds.remove(curUserId);

		filterInactiveUsers(recipientUserIds);

		if (sentRecipientUserIds != null) {
			recipientUserIds.removeAll(sentRecipientUserIds);

			sentRecipientUserIds.addAll(recipientUserIds);
		}
	}

	public static String getAction(AssetEntrySet assetEntrySet) {
		if (assetEntrySet.getType() == LoopAssetEntrySetConstants.TYPE_PAGE) {
			return LoopUserNotificationConstants.ACTION_PAGE;
		}
		else if (assetEntrySet.getLevel() >= 2) {
			return LoopUserNotificationConstants.ACTION_REPLIES;
		}

		return LoopUserNotificationConstants.ACTION_POST;
	}

	public static String getDisplayURL(
			long companyId, LoopUserNotificationEvent loopUserNotificationEvent)
		throws Exception {

		long assetEntrySetId = loopUserNotificationEvent.getGroupClassPK();

		if (LoopUserNotificationConstants.isGroupedComments(
				loopUserNotificationEvent.getType())) {

			assetEntrySetId = loopUserNotificationEvent.getClassPK();
		}

		String url = LoopConstants.URL_LOOP_FEED;

		if (loopUserNotificationEvent.getType() ==
				LoopUserNotificationConstants.
					TYPE_FOLLOWING_ENTITY_PAGE_CREATED) {

			AssetEntrySet assetEntrySet =
				AssetEntrySetLocalServiceUtil.getAssetEntrySet(assetEntrySetId);

			LoopDivision loopDivision =
				LoopDivisionLocalServiceUtil.getLoopDivision(
					assetEntrySet.getClassPK());

			StringBundler sb = new StringBundler(4);

			sb.append(LoopDivisionConstants.getTypeURL(loopDivision.getType()));
			sb.append(LoopConstants.URL_TOKEN_LOOP_DIVISION_NAME);

			Organization organization =
				OrganizationLocalServiceUtil.getOrganization(
					loopDivision.getOrganizationId());

			sb.append(organization.getName());

			sb.append(LoopConstants.URL_LOOP_ASSET_ENTRY_SET_PAGE);

			url = sb.toString();
		}

		return LoopUtil.getParticipantURL(
			companyId, url, String.valueOf(assetEntrySetId));
	}

	public static JSONObject getGroupedLoopUserNotificationEventJSONObject(
		ThemeDisplay themeDisplay,
		List<LoopUserNotificationEvent> loopUserNotificationEvents,
		LoopUserNotificationEvent loopUserNotificationEvent, boolean mobile) {

		try {
			JSONObject userNotificationEventJSONObject =
				JSONFactoryUtil.createJSONObject();

			userNotificationEventJSONObject.put(
				"displayJSONObject",
				getDisplayJSONObject(loopUserNotificationEvents));
			userNotificationEventJSONObject.put(
				"id", loopUserNotificationEvent.getGroupKey());

			AssetEntrySet assetEntrySet = getAssetEntrySet(
				loopUserNotificationEvent);

			userNotificationEventJSONObject.put(
				"message",
				getMessage(
					assetEntrySet, loopUserNotificationEvent.getType(),
					loopUserNotificationEvents.size()));

			userNotificationEventJSONObject.put(
				"private", isPrivate(loopUserNotificationEvent));
			userNotificationEventJSONObject.put(
				"read", isOpened(loopUserNotificationEvents));
			userNotificationEventJSONObject.put(
				"summary",
				themeDisplay.translate(
					getSummary(
						loopUserNotificationEvent, themeDisplay.getUserId()),
					getArguments(
						themeDisplay, loopUserNotificationEvents,
						loopUserNotificationEvent, mobile)));
			userNotificationEventJSONObject.put(
				"timestamp", getCreateTime(loopUserNotificationEvents));

			return userNotificationEventJSONObject;
		}
		catch (Exception e) {
			return JSONFactoryUtil.createJSONObject();
		}
	}

	public static JSONObject getGroupedLoopUserNotificationEventJSONObject(
			ThemeDisplay themeDisplay,
			LoopUserNotificationEvent loopUserNotificationEvent, boolean mobile,
			int apiVersion)
		throws Exception {

		List<LoopUserNotificationEvent> loopUserNotificationEvents =
			getLoopUserNotificationEvents(
				loopUserNotificationEvent.getGroupKey());

		if (loopUserNotificationEvents.isEmpty()) {
			return JSONFactoryUtil.createJSONObject();
		}

		JSONObject groupedLoopUserNotificationEventJSONObject =
			getGroupedLoopUserNotificationEventJSONObject(
				themeDisplay, loopUserNotificationEvents,
				loopUserNotificationEvent, mobile);

		if (groupedLoopUserNotificationEventJSONObject.length() == 0) {
			return JSONFactoryUtil.createJSONObject();
		}

		if (mobile) {
			return getMobileGroupedLoopUserNotificationEventJSONObject(
				themeDisplay, loopUserNotificationEvent,
				loopUserNotificationEvents,
				groupedLoopUserNotificationEventJSONObject, apiVersion);
		}

		groupedLoopUserNotificationEventJSONObject.put(
			"displayURL",
			getDisplayURL(
				themeDisplay.getCompanyId(),
				loopUserNotificationEvents.get(0)));

		return groupedLoopUserNotificationEventJSONObject;
	}

	public static int getGroupKeyCount(long userId) throws Exception {
		DynamicQuery loopUserNotificationEventDynamicQuery =
			_alloyServiceInvoker.buildDynamicQuery(
				new Object[] {"recipientUserId", userId, "received", false});

		Projection groupKeyProjection = ProjectionFactoryUtil.countDistinct(
			"groupKey");

		long count = _alloyServiceInvoker.executeDynamicQueryCount(
			loopUserNotificationEventDynamicQuery, groupKeyProjection);

		return Math.toIntExact(count);
	}

	public static List<Long> getGroupKeys(
			long userId, long groupClassNameId, long groupClassPK, int type)
		throws Exception {

		DynamicQuery dynamicQuery = _alloyServiceInvoker.buildDynamicQuery(
			new Object[] {
				"recipientUserId", userId, "groupClassNameId", groupClassNameId,
				"groupClassPK", groupClassPK, "type", type
			});

		Projection projection = ProjectionFactoryUtil.distinct(
			PropertyFactoryUtil.forName("groupKey"));

		dynamicQuery.setProjection(projection);

		return _alloyServiceInvoker.executeDynamicQuery(dynamicQuery);
	}

	public static List<LoopUserNotificationEvent> getLoopUserNotificationEvents(
			long groupKey)
		throws Exception {

		DynamicQuery dynamicQuery = _alloyServiceInvoker.buildDynamicQuery(
			new Object[] {"groupKey", groupKey});

		OrderByComparator obc = OrderByComparatorFactoryUtil.create(
			LoopUserNotificationEventModelImpl.TABLE_NAME, "createTime", false);

		return _alloyServiceInvoker.executeDynamicQuery(
			dynamicQuery, QueryUtil.ALL_POS, QueryUtil.ALL_POS, obc);
	}

	public static List<LoopUserNotificationEvent> getLoopUserNotificationEvents(
			long createTime, long userId, boolean gtTimestamp, int start,
			int end)
		throws Exception {

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			List<LoopUserNotificationEvent> loopUserNotificationEvents =
				new ArrayList<>();

			con = DataAccess.getConnection();

			String sql = LoopSQLUtil.getCustomSQL(
				LoopUserNotificationEvent.class.getName(), "findByCT_RUI");

			if (gtTimestamp) {
				sql = StringUtil.replace(
					sql, "[$TIMESTAMP_COMPARATOR$]", StringPool.GREATER_THAN);
			}
			else {
				sql = StringUtil.replace(
					sql, "[$TIMESTAMP_COMPARATOR$]",
					StringPool.LESS_THAN_OR_EQUAL);
			}

			sql = StringUtil.replace(
				sql, "[$LIMIT$]", LoopSQLUtil.getLimit(start, end));

			ps = con.prepareStatement(sql);

			ps.setLong(1, createTime);
			ps.setLong(2, userId);

			rs = ps.executeQuery();

			while (rs.next()) {
				LoopUserNotificationEvent loopUserNotificationEvent =
					LoopUserNotificationEventLocalServiceUtil.
						getLoopUserNotificationEvent(rs.getLong(1));

				loopUserNotificationEvents.add(loopUserNotificationEvent);
			}

			return loopUserNotificationEvents;
		}
		finally {
			DataAccess.cleanUp(con, ps, rs);
		}
	}

	public static List<LoopUserNotificationEvent> getLoopUserNotificationEvents(
			long userId, long classNameId, long classPK)
		throws Exception {

		StringBundler sb = new StringBundler(13);

		sb.append("where LoopUserNotificationEvent.recipientUserId = ");
		sb.append(userId);
		sb.append(" and ((LoopUserNotificationEvent.classNameId = ");
		sb.append(classNameId);
		sb.append(" and LoopUserNotificationEvent.classPK = ");
		sb.append(classPK);
		sb.append(") or (LoopUserNotificationEvent.classNameId <> ");
		sb.append(classNameId);
		sb.append(" and LoopUserNotificationEvent.groupClassNameId = ");
		sb.append(classNameId);
		sb.append(" and LoopUserNotificationEvent.groupClassPK = ");
		sb.append(classPK);
		sb.append("))");

		return getLoopUserNotificationEvents(sb.toString());
	}

	public static String getMessage(long classNameId, long classPK)
		throws Exception {

		if (classNameId == PortalUtil.getClassNameId(AssetEntrySet.class)) {
			AssetEntrySet assetEntrySet =
				AssetEntrySetLocalServiceUtil.getAssetEntrySet(classPK);

			JSONObject payloadJSONObject = JSONFactoryUtil.createJSONObject(
				assetEntrySet.getPayload());

			return LoopMarkdownUtil.interpretTokens(
				PortalUtil.getDefaultCompanyId(),
				payloadJSONObject.getString("message"),
				payloadJSONObject.getJSONArray(
					LoopAssetEntrySetConstants.PAYLOAD_KEY_SHARED_TO),
				false);
		}

		return StringPool.BLANK;
	}

	public static String getMessage(
			long classNameId, long classPK, int length, boolean stripHtml)
		throws Exception {

		String message = getMessage(classNameId, classPK);

		if (stripHtml) {
			message = replaceElements(message);

			message = HtmlUtil.stripHtml(message);

			message = StringUtil.replace(message, "&quot;", StringPool.QUOTE);

			message = HtmlUtil.unescape(message);

			message = message.trim();
		}

		return StringUtil.shorten(message, length);
	}

	public static JSONObject
			getMobileGroupedLoopUserNotificationEventJSONObject(
				ThemeDisplay themeDisplay,
				LoopUserNotificationEvent loopUserNotificationEvent,
				List<LoopUserNotificationEvent> loopUserNotificationEvents,
				JSONObject groupedLoopUserNotificationEventJSONObject,
				int apiVersion)
		throws Exception {

		AssetEntrySet assetEntrySet = getAssetEntrySet(
			loopUserNotificationEvent);

		groupedLoopUserNotificationEventJSONObject.put(
			"action", getAction(assetEntrySet));
		groupedLoopUserNotificationEventJSONObject.put(
			"assetEntrySetId", assetEntrySet.getAssetEntrySetId());

		if (apiVersion == 1) {
			groupedLoopUserNotificationEventJSONObject.put(
				"arguments",
				getMobileArgumentsJSONArray(
					loopUserNotificationEvent, loopUserNotificationEvents,
					themeDisplay.getUserId()));
			groupedLoopUserNotificationEventJSONObject.put(
				"summary",
				getMobileSummary(
					loopUserNotificationEvent, themeDisplay.getUserId()));
		}

		return groupedLoopUserNotificationEventJSONObject;
	}

	public static String getNotificationMessagePattern(
		int level, int notificationType, int deliveryType) {

		String pattern = StringPool.BLANK;

		if (level == 0) {
			if (notificationType ==
					LoopUserNotificationConstants.TYPE_ANNOUNCEMENT) {

				pattern = "x-shared-an-announcement-with-you";
			}
			else if (notificationType ==
						LoopUserNotificationConstants.
							TYPE_FOLLOWING_CREATOR_POSTS) {

				pattern = "x-created-a-new-post";
			}
			else if (notificationType ==
						LoopUserNotificationConstants.
							TYPE_FOLLOWING_DIVISION_MENTIONED) {

				pattern = "x-shared-a-post-with-a-group-you-are-following";
			}
			else if (notificationType ==
						LoopUserNotificationConstants.
							TYPE_FOLLOWING_ENTITY_PAGE_CREATED) {

				pattern = "x-created-a-page-for-a-group-you-are-following";
			}
			else if (notificationType ==
						LoopUserNotificationConstants.
							TYPE_FOLLOWING_PERSON_MENTIONED) {

				pattern = "x-shared-a-post-with-a-person-you-are-following";
			}
			else if (notificationType ==
						LoopUserNotificationConstants.
							TYPE_FOLLOWING_TOPIC_MENTIONED) {

				pattern = "x-shared-a-post-with-a-topic-you-are-following";
			}
			else if (notificationType ==
						LoopUserNotificationConstants.TYPE_LIKED) {

				pattern = "x-liked-your-post";
			}
			else if (notificationType ==
						LoopUserNotificationConstants.TYPE_MENTIONED_DIRECTLY) {

				pattern = "x-shared-a-post-with-you";
			}
		}
		else if (level == 1) {
			if (notificationType ==
					LoopUserNotificationConstants.TYPE_COMMENTED_ON_MY_POST) {

				pattern = "x-commented-on-your-post";
			}
			else if (notificationType ==
						LoopUserNotificationConstants.
							TYPE_COMMENTED_ON_SUBSCRIBED_POST) {

				pattern = "x-commented-on-a-post-you-are-following";
			}
			else if (notificationType ==
						LoopUserNotificationConstants.
							TYPE_FOLLOWING_CREATOR_COMMENTS_AND_REPLIES) {

				pattern = "x-commented-on-a-post";
			}
			else if (notificationType ==
						LoopUserNotificationConstants.
							TYPE_FOLLOWING_DIVISION_MENTIONED) {

				pattern = "x-mentioned-a-group-you-are-following-in-a-comment";
			}
			else if (notificationType ==
						LoopUserNotificationConstants.
							TYPE_FOLLOWING_PERSON_MENTIONED) {

				pattern = "x-mentioned-a-person-you-are-following-in-a-comment";
			}
			else if (notificationType ==
						LoopUserNotificationConstants.
							TYPE_FOLLOWING_TOPIC_MENTIONED) {

				pattern = "x-mentioned-a-topic-you-are-following-in-a-comment";
			}
			else if (notificationType ==
						LoopUserNotificationConstants.TYPE_LIKED) {

				pattern = "x-liked-your-comment";
			}
			else if (notificationType ==
						LoopUserNotificationConstants.TYPE_MENTIONED_DIRECTLY) {

				pattern = "x-mentioned-you-in-a-comment";
			}
		}
		else if (level == 2) {
			if (notificationType ==
					LoopUserNotificationConstants.TYPE_COMMENTED_ON_MY_POST) {

				pattern = "x-replied-to-your-comment";
			}
			else if (notificationType ==
						LoopUserNotificationConstants.
							TYPE_FOLLOWING_CREATOR_COMMENTS_AND_REPLIES) {

				pattern = "x-replied-to-a-comment";
			}
			else if (notificationType ==
						LoopUserNotificationConstants.
							TYPE_FOLLOWING_DIVISION_MENTIONED) {

				pattern = "x-mentioned-a-group-you-are-following-in-a-reply";
			}
			else if (notificationType ==
						LoopUserNotificationConstants.
							TYPE_FOLLOWING_PERSON_MENTIONED) {

				pattern = "x-mentioned-a-person-you-are-following-in-a-reply";
			}
			else if (notificationType ==
						LoopUserNotificationConstants.
							TYPE_FOLLOWING_TOPIC_MENTIONED) {

				pattern = "x-mentioned-a-topic-you-are-following-in-a-reply";
			}
			else if (notificationType ==
						LoopUserNotificationConstants.TYPE_LIKED) {

				pattern = "x-liked-your-reply";
			}
			else if (notificationType ==
						LoopUserNotificationConstants.TYPE_MENTIONED_DIRECTLY) {

				pattern = "x-mentioned-you-in-a-reply";
			}
		}

		if (deliveryType == LoopUserNotificationConstants.DELIVERY_TYPE_PUSH) {
			pattern = pattern + "-x";
		}

		return pattern;
	}

	public static String getTruncatedMessage(AssetEntrySet assetEntrySet)
		throws Exception {

		JSONObject payloadJSONObject = JSONFactoryUtil.createJSONObject(
			assetEntrySet.getPayload());

		String message = payloadJSONObject.getString("message");

		if (payloadJSONObject.getBoolean("truncated")) {
			message = payloadJSONObject.getString("truncatedMessage");
		}

		return LoopMarkdownUtil.interpretTokens(
			PortalUtil.getDefaultCompanyId(), message,
			payloadJSONObject.getJSONArray(
				LoopAssetEntrySetConstants.PAYLOAD_KEY_SHARED_TO),
			false);
	}

	public static void setOpened(long userId, long groupKey) throws Exception {
		List<LoopUserNotificationEvent> loopUserNotificationEvents =
			_alloyServiceInvoker.executeDynamicQuery(
				new Object[] {"groupKey", groupKey});

		for (LoopUserNotificationEvent loopUserNotificationEvent :
				loopUserNotificationEvents) {

			if (loopUserNotificationEvent.isReceived() &&
				loopUserNotificationEvent.isOpened()) {

				continue;
			}

			loopUserNotificationEvent.setReceived(true);
			loopUserNotificationEvent.setOpened(true);

			LoopUserNotificationEventLocalServiceUtil.
				updateLoopUserNotificationEvent(loopUserNotificationEvent);
		}

		LoopPersonUtil.updateGroupedUserNotificationEventsCount(userId);
	}

	protected static String encode(String value) {
		return LoopConstants.ENCODE_TOKEN + value + LoopConstants.ENCODE_TOKEN;
	}

	protected static Object[] getArguments(
			LoopUserNotificationEvent loopUserNotificationEvent,
			List<LoopUserNotificationEvent> loopUserNotificationEvents,
			long userId)
		throws Exception {

		AssetEntrySet assetEntrySet = getAssetEntrySet(
			loopUserNotificationEvent);

		if (loopUserNotificationEvent.getType() ==
				LoopUserNotificationConstants.TYPE_FOLLOWING_CREATOR_POSTS) {

			return new Object[] {
				getEntityJSONObject(
					assetEntrySet.getCreatorClassNameId(),
					assetEntrySet.getCreatorClassPK())
			};
		}
		else if (loopUserNotificationEvent.getType() ==
					LoopUserNotificationConstants.
						TYPE_FOLLOWING_ENTITY_PAGE_CREATED) {

			return new Object[] {
				getEntityJSONObject(
					assetEntrySet.getCreatorClassNameId(),
					assetEntrySet.getCreatorClassPK()),
				getEntityJSONObject(
					assetEntrySet.getClassNameId(), assetEntrySet.getClassPK())
			};
		}
		else if ((loopUserNotificationEvent.getType() ==
					LoopUserNotificationConstants.
						TYPE_COMMENTED_ON_SUBSCRIBED_POST) ||
				 (loopUserNotificationEvent.getType() ==
					 LoopUserNotificationConstants.
						 TYPE_FOLLOWING_CREATOR_COMMENTS_AND_REPLIES)) {

			AssetEntrySet parentAssetEntrySet =
				AssetEntrySetLocalServiceUtil.getAssetEntrySet(
					loopUserNotificationEvent.getGroupClassPK());

			return new Object[] {
				getCreatorArgumentJSONArray(loopUserNotificationEvents),
				getEntityJSONObject(
					parentAssetEntrySet.getCreatorClassNameId(),
					parentAssetEntrySet.getCreatorClassPK())
			};
		}
		else if ((loopUserNotificationEvent.getType() ==
					LoopUserNotificationConstants.TYPE_ANNOUNCEMENT) ||
				 (loopUserNotificationEvent.getType() ==
					 LoopUserNotificationConstants.
						 TYPE_FOLLOWING_ENTITY_MENTIONED)) {

			if (!isSharedWithUser(loopUserNotificationEvent, userId)) {
				return new Object[] {
					getCreatorArgumentJSONArray(loopUserNotificationEvents),
					getSharedToArgumentJSONArray(
						loopUserNotificationEvent, userId)
				};
			}
		}
		else if (loopUserNotificationEvent.getType() ==
					LoopUserNotificationConstants.TYPE_LIKED) {

			return new Object[] {
				getLikeParticipantArgumentJSONArray(loopUserNotificationEvents)
			};
		}

		return new Object[] {
			getCreatorArgumentJSONArray(loopUserNotificationEvents)
		};
	}

	protected static Object[] getArguments(
			ThemeDisplay themeDisplay,
			List<LoopUserNotificationEvent> loopUserNotificationEvents,
			LoopUserNotificationEvent loopUserNotificationEvent, boolean mobile)
		throws Exception {

		Object[] arguments = getArguments(
			loopUserNotificationEvent, loopUserNotificationEvents,
			themeDisplay.getUserId());

		for (int i = 0; i < arguments.length; i++) {
			Object argument = arguments[i];

			if (argument instanceof JSONObject) {
				JSONObject jsonObject = (JSONObject)argument;

				if (mobile) {
					arguments[i] = encode(getMobileName(jsonObject));
				}
				else {
					arguments[i] = getEntityLink(
						themeDisplay.getCompanyId(),
						jsonObject.getLong("entityClassNameId"),
						jsonObject.getLong("entityClassPK"));
				}
			}
			else if (argument instanceof JSONArray) {
				List<String> entities = new ArrayList<>();

				JSONArray jsonArray = (JSONArray)argument;

				for (int j = 0; j < jsonArray.length(); j++) {
					JSONObject jsonObject = jsonArray.getJSONObject(j);

					if (mobile) {
						entities.add(encode(getMobileName(jsonObject)));
					}
					else {
						entities.add(
							getEntityLink(
								themeDisplay.getCompanyId(),
								jsonObject.getLong("entityClassNameId"),
								jsonObject.getLong("entityClassPK")));
					}
				}

				arguments[i] = getEntityNames(themeDisplay, entities);
			}
		}

		return arguments;
	}

	protected static AssetEntrySet getAssetEntrySet(
			LoopUserNotificationEvent loopUserNotificationEvent)
		throws Exception {

		long assetEntrySetId = loopUserNotificationEvent.getClassPK();

		if (loopUserNotificationEvent.getType() ==
				LoopUserNotificationConstants.TYPE_LIKED) {

			assetEntrySetId = loopUserNotificationEvent.getGroupClassPK();
		}

		return AssetEntrySetLocalServiceUtil.getAssetEntrySet(assetEntrySetId);
	}

	protected static long getCreateTime(
		List<LoopUserNotificationEvent> loopUserNotificationEvents) {

		long createTime = 0;

		for (LoopUserNotificationEvent loopUserNotificationEvent :
				loopUserNotificationEvents) {

			if (loopUserNotificationEvent.getCreateTime() > createTime) {
				createTime = loopUserNotificationEvent.getCreateTime();
			}
		}

		return createTime;
	}

	protected static JSONArray getCreatorArgumentJSONArray(
			List<LoopUserNotificationEvent> loopUserNotificationEvents)
		throws Exception {

		JSONArray creatorJSONArray = JSONFactoryUtil.createJSONArray();
		List<Long> creatorClassPKs = new ArrayList<>();

		for (LoopUserNotificationEvent loopUserNotificationEvent :
				loopUserNotificationEvents) {

			AssetEntrySet assetEntrySet =
				AssetEntrySetLocalServiceUtil.getAssetEntrySet(
					loopUserNotificationEvent.getClassPK());

			if (creatorClassPKs.contains(assetEntrySet.getCreatorClassPK())) {
				continue;
			}

			creatorClassPKs.add(assetEntrySet.getCreatorClassPK());

			creatorJSONArray.put(
				getEntityJSONObject(
					assetEntrySet.getCreatorClassNameId(),
					assetEntrySet.getCreatorClassPK()));
		}

		return creatorJSONArray;
	}

	protected static JSONObject getDisplayJSONObject(
			List<LoopUserNotificationEvent> loopUserNotificationEvents)
		throws Exception {

		LoopUserNotificationEvent loopUserNotificationEvent =
			loopUserNotificationEvents.get(0);

		long classNameId = 0;
		long classPK = 0;

		if (loopUserNotificationEvent.getType() ==
				LoopUserNotificationConstants.TYPE_LIKED) {

			classNameId = loopUserNotificationEvent.getClassNameId();
			classPK = loopUserNotificationEvent.getClassPK();
		}
		else {
			AssetEntrySet assetEntrySet =
				AssetEntrySetLocalServiceUtil.getAssetEntrySet(
					loopUserNotificationEvent.getClassPK());

			classNameId = assetEntrySet.getCreatorClassNameId();
			classPK = assetEntrySet.getCreatorClassPK();
		}

		return getEntityJSONObject(classNameId, classPK);
	}

	protected static JSONObject getEntityJSONObject(
		long classNameId, long classPK) {

		JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

		jsonObject.put("entityClassNameId", classNameId);
		jsonObject.put("entityClassPK", classPK);

		return jsonObject;
	}

	protected static String getEntityLink(
			long companyId, long classNameId, long classPK)
		throws Exception {

		StringBundler sb = new StringBundler(8);

		sb.append("<a ");

		if (classNameId == PortalUtil.getClassNameId(LoopPerson.class)) {
			LoopPerson loopPerson = LoopPersonLocalServiceUtil.getLoopPerson(
				classPK);

			User user = UserLocalServiceUtil.getUser(
				loopPerson.getPersonUserId());

			if (user.getStatus() == WorkflowConstants.STATUS_INACTIVE) {
				sb.append("class=\"inactive\" ");
			}
		}

		sb.append("href=\"");
		sb.append(LoopUtil.getDisplayURL(companyId, classNameId, classPK));
		sb.append("\">");

		if (classNameId == PortalUtil.getClassNameId(LoopTopic.class)) {
			sb.append(StringPool.POUND);
		}

		sb.append(
			HtmlUtil.escape(
				AssetEntrySetParticipantInfoUtil.getParticipantName(
					classNameId, classPK)));
		sb.append("</a>");

		return sb.toString();
	}

	protected static String getEntityNames(
		ThemeDisplay themeDisplay, List<String> entities) {

		if (entities.size() == 1) {
			return entities.get(0);
		}

		Object[] arguments = new Object[2];

		if (entities.size() == 2) {
			arguments[0] = entities.get(0);
			arguments[1] = entities.get(1);
		}
		else {
			arguments[0] = StringUtil.merge(
				ListUtil.subList(entities, 0, 2), StringPool.COMMA_AND_SPACE);

			String pattern = "x-other";

			if (entities.size() > 3) {
				pattern = "x-others";
			}

			arguments[1] = themeDisplay.translate(pattern, entities.size() - 2);
		}

		return themeDisplay.translate("x-and-x", arguments);
	}

	protected static JSONArray getLikeParticipantArgumentJSONArray(
		List<LoopUserNotificationEvent> loopUserNotificationEvents) {

		JSONArray likeParticipantJSONArray = JSONFactoryUtil.createJSONArray();

		for (LoopUserNotificationEvent loopUserNotificationEvent :
				loopUserNotificationEvents) {

			likeParticipantJSONArray.put(
				getEntityJSONObject(
					loopUserNotificationEvent.getClassNameId(),
					loopUserNotificationEvent.getClassPK()));
		}

		return likeParticipantJSONArray;
	}

	protected static List<LoopUserNotificationEvent>
			getLoopUserNotificationEvents(String whereClause)
		throws Exception {

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			List<LoopUserNotificationEvent> loopUserNotificationEvents =
				new ArrayList<>();

			con = DataAccess.getConnection();

			String sql = LoopSQLUtil.getCustomSQL(
				LoopUserNotificationEvent.class.getName(),
				"findLoopUserNotificationEventIds");

			sql = StringUtil.replace(sql, "[$WHERE_CLAUSE$]", whereClause);

			ps = con.prepareStatement(sql);

			rs = ps.executeQuery();

			while (rs.next()) {
				LoopUserNotificationEvent loopUserNotificationEvent =
					LoopUserNotificationEventLocalServiceUtil.
						getLoopUserNotificationEvent(rs.getLong(1));

				loopUserNotificationEvents.add(loopUserNotificationEvent);
			}

			return loopUserNotificationEvents;
		}
		finally {
			DataAccess.cleanUp(con, ps, rs);
		}
	}

	protected static String getMessage(
			AssetEntrySet assetEntrySet, int notificationType,
			int loopUserNotificationEventsSize)
		throws Exception {

		if ((loopUserNotificationEventsSize > 1) &&
			LoopUserNotificationConstants.isGroupedComments(notificationType)) {

			return StringPool.BLANK;
		}

		if ((notificationType ==
				LoopUserNotificationConstants.TYPE_ANNOUNCEMENT) ||
			(notificationType ==
				LoopUserNotificationConstants.
					TYPE_FOLLOWING_ENTITY_PAGE_CREATED)) {

			return assetEntrySet.getTitle();
		}

		return getMessage(
			PortalUtil.getClassNameId(AssetEntrySet.class),
			assetEntrySet.getAssetEntrySetId(), 100, true);
	}

	protected static JSONArray getMobileArgumentsJSONArray(
			LoopUserNotificationEvent loopUserNotificationEvent,
			List<LoopUserNotificationEvent> loopUserNotificationEvents,
			long userId)
		throws Exception {

		JSONArray mobileArgumentsJSONArray = JSONFactoryUtil.createJSONArray();

		Object[] arguments = getArguments(
			loopUserNotificationEvent, loopUserNotificationEvents, userId);

		for (Object argument : arguments) {
			JSONArray argumentJSONArray = JSONFactoryUtil.createJSONArray();

			if (argument instanceof JSONObject) {
				JSONObject jsonObject = (JSONObject)argument;

				argumentJSONArray.put(getMobileName(jsonObject));
			}
			else if (argument instanceof JSONArray) {
				JSONArray jsonArray = (JSONArray)argument;

				if (jsonArray.length() == 0) {
					continue;
				}

				for (int i = 0; i < jsonArray.length(); i++) {
					if (i > 1) {
						argumentJSONArray.put(jsonArray.length() - 2);

						break;
					}

					argumentJSONArray.put(
						getMobileName(jsonArray.getJSONObject(i)));
				}
			}
			else {
				argumentJSONArray.put(argument.toString());
			}

			mobileArgumentsJSONArray.put(argumentJSONArray);
		}

		return mobileArgumentsJSONArray;
	}

	protected static String getMobileName(JSONObject jsonObject)
		throws Exception {

		long classNameId = jsonObject.getLong("entityClassNameId");

		String name = AssetEntrySetParticipantInfoUtil.getParticipantName(
			classNameId, jsonObject.getLong("entityClassPK"));

		if (classNameId == PortalUtil.getClassNameId(LoopTopic.class)) {
			name = StringPool.POUND + name;
		}

		return name;
	}

	protected static String getMobileSummary(
			LoopUserNotificationEvent loopUserNotificationEvent, long userId)
		throws Exception {

		String summary = getSummary(loopUserNotificationEvent, userId);

		summary = StringUtil.replace(
			summary, CharPool.DASH, CharPool.UNDERLINE);
		summary = StringUtil.replace(summary, "'s", StringPool.BLANK);

		return summary;
	}

	protected static JSONArray getSharedToArgumentJSONArray(
			LoopUserNotificationEvent loopUserNotificationEvent, long userId)
		throws Exception {

		AssetEntrySet assetEntrySet =
			AssetEntrySetLocalServiceUtil.getAssetEntrySet(
				loopUserNotificationEvent.getClassPK());

		JSONObject payloadJSONObject = JSONFactoryUtil.createJSONObject(
			assetEntrySet.getPayload());

		JSONArray sharedToJSONArray = payloadJSONObject.getJSONArray(
			"sharedTo");

		LoopPerson loopPerson =
			LoopPersonLocalServiceUtil.getLoopPersonByPersonUserId(userId);

		JSONArray sharedToArgumentJSONArray = JSONFactoryUtil.createJSONArray();

		for (int i = 0; i < sharedToJSONArray.length(); i++) {
			JSONObject sharedToJSONObject = sharedToJSONArray.getJSONObject(i);

			long entityClassNameId = sharedToJSONObject.getLong(
				"entityClassNameId");
			long entityClassPK = sharedToJSONObject.getLong("entityClassPK");

			LoopUserNotificationSubscription loopUserNotificationSubscription =
				LoopUserNotificationSubscriptionLocalServiceUtil.
					fetchLoopUserNotificationSubscription(
						loopPerson.getLoopPersonId(), entityClassNameId,
						entityClassPK,
						LoopUserNotificationConstants.DELIVERY_TYPE_IN_APP);

			if (loopUserNotificationSubscription != null) {
				sharedToArgumentJSONArray.put(
					getEntityJSONObject(entityClassNameId, entityClassPK));
			}
		}

		return sharedToArgumentJSONArray;
	}

	protected static String getSummary(
			LoopUserNotificationEvent loopUserNotificationEvent, long userId)
		throws Exception {

		AssetEntrySet assetEntrySet = getAssetEntrySet(
			loopUserNotificationEvent);

		if (assetEntrySet.getLevel() == 0) {
			if (loopUserNotificationEvent.getType() ==
					LoopUserNotificationConstants.TYPE_ANNOUNCEMENT) {

				if (isSharedWithUser(loopUserNotificationEvent, userId)) {
					return "x-shared-an-announcement-with-you";
				}

				JSONArray sharedToArgumentJSONArray =
					getSharedToArgumentJSONArray(
						loopUserNotificationEvent, userId);

				if (sharedToArgumentJSONArray.length() > 0) {
					return "x-shared-an-announcement-with-x";
				}

				return "x-created-a-new-announcement";
			}
			else if (loopUserNotificationEvent.getType() ==
						LoopUserNotificationConstants.
							TYPE_FOLLOWING_CREATOR_POSTS) {

				return "x-created-a-new-post";
			}
			else if (loopUserNotificationEvent.getType() ==
						LoopUserNotificationConstants.
							TYPE_FOLLOWING_ENTITY_PAGE_CREATED) {

				return "x-created-a-new-page-for-x";
			}
			else if (loopUserNotificationEvent.getType() ==
						LoopUserNotificationConstants.
							TYPE_FOLLOWING_ENTITY_MENTIONED) {

				JSONArray sharedToArgumentJSONArray =
					getSharedToArgumentJSONArray(
						loopUserNotificationEvent, userId);

				if (sharedToArgumentJSONArray.length() > 0) {
					return "x-shared-a-post-with-x";
				}

				return "x-shared-a-post";
			}
			else if (loopUserNotificationEvent.getType() ==
						LoopUserNotificationConstants.TYPE_LIKED) {

				return "x-liked-your-post";
			}
			else if (loopUserNotificationEvent.getType() ==
						LoopUserNotificationConstants.TYPE_MENTIONED_DIRECTLY) {

				if (isSharedWithUser(loopUserNotificationEvent, userId)) {
					return "x-shared-a-post-with-you";
				}

				return "x-shared-a-post";
			}
		}
		else if (assetEntrySet.getLevel() == 1) {
			if (loopUserNotificationEvent.getType() ==
					LoopUserNotificationConstants.TYPE_COMMENTED_ON_MY_POST) {

				return "x-commented-on-your-post";
			}
			else if ((loopUserNotificationEvent.getType() ==
						LoopUserNotificationConstants.
							TYPE_COMMENTED_ON_SUBSCRIBED_POST) ||
					 (loopUserNotificationEvent.getType() ==
						 LoopUserNotificationConstants.
							 TYPE_FOLLOWING_CREATOR_COMMENTS_AND_REPLIES)) {

				return "x-commented-on-x's-post";
			}
			else if (loopUserNotificationEvent.getType() ==
						LoopUserNotificationConstants.
							TYPE_FOLLOWING_ENTITY_MENTIONED) {

				JSONArray sharedToArgumentJSONArray =
					getSharedToArgumentJSONArray(
						loopUserNotificationEvent, userId);

				if (sharedToArgumentJSONArray.length() > 0) {
					return "x-mentioned-x-in-a-comment";
				}

				return "x-commented-on-a-post";
			}
			else if (loopUserNotificationEvent.getType() ==
						LoopUserNotificationConstants.TYPE_LIKED) {

				return "x-liked-your-comment";
			}
			else if (loopUserNotificationEvent.getType() ==
						LoopUserNotificationConstants.TYPE_MENTIONED_DIRECTLY) {

				if (isSharedWithUser(loopUserNotificationEvent, userId)) {
					return "x-mentioned-you-in-a-comment";
				}

				return "x-commented-on-a-post";
			}
		}
		else if (assetEntrySet.getLevel() == 2) {
			if (loopUserNotificationEvent.getType() ==
					LoopUserNotificationConstants.TYPE_COMMENTED_ON_MY_POST) {

				return "x-replied-to-your-comment";
			}
			else if (loopUserNotificationEvent.getType() ==
						LoopUserNotificationConstants.
							TYPE_FOLLOWING_CREATOR_COMMENTS_AND_REPLIES) {

				return "x-replied-to-x's-comment";
			}
			else if (loopUserNotificationEvent.getType() ==
						LoopUserNotificationConstants.
							TYPE_FOLLOWING_ENTITY_MENTIONED) {

				JSONArray sharedToArgumentJSONArray =
					getSharedToArgumentJSONArray(
						loopUserNotificationEvent, userId);

				if (sharedToArgumentJSONArray.length() > 0) {
					return "x-mentioned-x-in-a-reply";
				}

				return "x-replied-to-a-comment";
			}
			else if (loopUserNotificationEvent.getType() ==
						LoopUserNotificationConstants.TYPE_LIKED) {

				return "x-liked-your-reply";
			}
			else if (loopUserNotificationEvent.getType() ==
						LoopUserNotificationConstants.TYPE_MENTIONED_DIRECTLY) {

				if (isSharedWithUser(loopUserNotificationEvent, userId)) {
					return "x-mentioned-you-in-a-reply";
				}

				return "x-replied-to-a-comment";
			}
		}

		return StringPool.BLANK;
	}

	protected static boolean isOpened(
		List<LoopUserNotificationEvent> loopUserNotificationEvents) {

		for (LoopUserNotificationEvent loopUserNotificationEvent :
				loopUserNotificationEvents) {

			if (!loopUserNotificationEvent.isOpened()) {
				return false;
			}
		}

		return true;
	}

	protected static boolean isPrivate(
			LoopUserNotificationEvent loopUserNotificationEvent)
		throws Exception {

		AssetEntrySet assetEntrySet =
			AssetEntrySetLocalServiceUtil.getAssetEntrySet(
				loopUserNotificationEvent.getGroupClassPK());

		return assetEntrySet.isPrivateAssetEntrySet();
	}

	protected static boolean isSharedWithUser(
			LoopUserNotificationEvent loopUserNotificationEvent, long userId)
		throws Exception {

		if (loopUserNotificationEvent.getClassNameId() !=
				PortalUtil.getClassNameId(AssetEntrySet.class)) {

			return false;
		}

		AssetEntrySet assetEntrySet =
			AssetEntrySetLocalServiceUtil.getAssetEntrySet(
				loopUserNotificationEvent.getClassPK());

		JSONObject payloadJSONObject = JSONFactoryUtil.createJSONObject(
			assetEntrySet.getPayload());

		JSONArray sharedToJSONArray = payloadJSONObject.getJSONArray(
			"sharedTo");

		LoopPerson loopPerson =
			LoopPersonLocalServiceUtil.getLoopPersonByPersonUserId(userId);

		for (int i = 0; i < sharedToJSONArray.length(); i++) {
			JSONObject sharedToJSONObject = sharedToJSONArray.getJSONObject(i);

			if ((sharedToJSONObject.getLong("entityClassNameId") ==
					PortalUtil.getClassNameId(LoopPerson.class)) &&
				(sharedToJSONObject.getLong("entityClassPK") ==
					loopPerson.getLoopPersonId())) {

				return true;
			}
		}

		return false;
	}

	protected static String replaceElements(String message) {
		Document document = Jsoup.parse(message);

		for (Map.Entry<String, String> entry :
				_elementPlaceholders.entrySet()) {

			for (Element element : document.select(entry.getKey())) {
				Element replacementElement = new Element(
					Tag.valueOf("p"), StringPool.BLANK);

				replacementElement.text(entry.getValue());

				element.replaceWith(replacementElement);
			}
		}

		return document.toString();
	}

	private static final AlloyServiceInvoker _alloyServiceInvoker =
		new AlloyServiceInvoker(LoopUserNotificationEvent.class.getName());
	private static final Map<String, String> _elementPlaceholders =
		new HashMap<>();

	static {
		_elementPlaceholders.put("img", "[IMAGE]");
		_elementPlaceholders.put("table", "[TABLE]");
	}

}