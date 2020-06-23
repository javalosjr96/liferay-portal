<%--
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
--%>

<%@ include file="/blogs/init.jsp" %>

<%
SearchContainer searchContainer = (SearchContainer)request.getAttribute("view_entry_content.jsp-searchContainer");

BlogsEntry entry = (BlogsEntry)request.getAttribute("view_entry_content.jsp-entry");

AssetEntry assetEntry = (AssetEntry)request.getAttribute("view_entry_content.jsp-assetEntry");

String socialBookmarksDisplayPosition = blogsPortletInstanceConfiguration.socialBookmarksDisplayPosition();
%>

<c:choose>
	<c:when test="<%= BlogsEntryPermission.contains(permissionChecker, entry, ActionKeys.VIEW) && (entry.isVisible() || (entry.getUserId() == user.getUserId()) || BlogsEntryPermission.contains(permissionChecker, entry, ActionKeys.UPDATE)) %>">
		<div class="entry" id="<portlet:namespace /><%= entry.getEntryId() %>">

			<%
			String mvcRenderCommandName = ParamUtil.getString(request, "mvcRenderCommandName");

			long assetCategoryId = ParamUtil.getLong(request, "categoryId");
			String assetTagName = ParamUtil.getString(request, "tag");

			boolean viewSingleEntry = mvcRenderCommandName.equals("/blogs/view_entry") && (assetCategoryId == 0) && Validator.isNull(assetTagName);

			String colCssClass = StringPool.BLANK;

			if (viewSingleEntry) {
				colCssClass = "col-md-offset-2 col-md-8";
			}
			%>

			<div class="entry-body">
				<div class="container">
					<portlet:renderURL var="viewEntryURL">
						<portlet:param name="mvcRenderCommandName" value="/blogs/view_entry" />
						<portlet:param name="redirect" value="<%= currentURL %>" />
						<portlet:param name="urlTitle" value="<%= entry.getUrlTitle() %>" />
					</portlet:renderURL>

					<div class="entry-title">
						<c:if test="<%= !viewSingleEntry %>">
							<h4>
								<aui:a href="<%= viewEntryURL %>"><%= HtmlUtil.escape(entry.getTitle()) %></aui:a>
							</h4>

							<c:if test="<%= !entry.isApproved() %>">
								<h5>
									<aui:workflow-status markupView="lexicon" showIcon="<%= false %>" showLabel="<%= false %>" status="<%= entry.getStatus() %>" />
								</h5>
							</c:if>

							<c:if test="<%= BlogsEntryPermission.contains(permissionChecker, entry, ActionKeys.DELETE) || BlogsEntryPermission.contains(permissionChecker, entry, ActionKeys.PERMISSIONS) || BlogsEntryPermission.contains(permissionChecker, entry, ActionKeys.UPDATE) %>">
								<liferay-util:include page="/blogs/entry_action.jsp" servletContext="<%= application %>" />
							</c:if>

							<c:if test='<%= blogsPortletInstanceConfiguration.enableSocialBookmarks() && socialBookmarksDisplayPosition.equals("top") %>'>
								<liferay-util:include page="/blogs/social_bookmarks.jsp" servletContext="<%= application %>" />
							</c:if>
						</c:if>

						<div class="entry-info text-muted ">
							<c:if test="<%= !viewSingleEntry %>">
								<small>
									<strong><%= HtmlUtil.escape(entry.getUserName()) %></strong>

									<span> | </span>
									<span class="hide-accessible"><liferay-ui:message key="published-date" /></span>
									<%= dateFormatDate.format(entry.getDisplayDate()) %>
								</small>
							</c:if>

							<c:if test='<%= viewSingleEntry && blogsPortletInstanceConfiguration.enableSocialBookmarks() && socialBookmarksDisplayPosition.equals("top") && viewSingleEntry %>'>
								<liferay-util:include page="/blogs/social_bookmarks.jsp" servletContext="<%= application %>" />
							</c:if>
						</div>
					</div>

					<%
					String subtitle = entry.getSubtitle();
					%>

					<c:if test="<%= viewSingleEntry && Validator.isNotNull(subtitle) %>">
						<div class="entry-subtitle">
							<p>
								<strong><%= HtmlUtil.escape(subtitle) %></strong>
							</p>
						</div>
					</c:if>

					<%
					String coverImageURL = entry.getCoverImageURL(themeDisplay);
					%>

					<c:if test="<%= viewSingleEntry && Validator.isNotNull(coverImageURL) %>">
						<div class="entry-cover-image">
							<figure>
								<img class="blog-img-shadow" src="<%= coverImageURL %>" />
							</figure>
						</div>
					</c:if>
				</div>
			</div>

			<div class="entry-body">
				<c:choose>
					<c:when test="<%= blogsPortletInstanceConfiguration.displayStyle().equals(BlogsUtil.DISPLAY_STYLE_ABSTRACT) && !viewSingleEntry %>">
						<c:if test="<%= entry.isSmallImage() && Validator.isNull(coverImageURL) %>">
							<div class="asset-small-image">
								<img alt="" class="asset-small-image img-thumbnail" src="<%= HtmlUtil.escape(entry.getSmallImageURL(themeDisplay)) %>" width="150" />
							</div>
						</c:if>

						<%
						String summary = entry.getDescription();

						if (Validator.isNull(summary)) {
							summary = entry.getContent();
						}
						%>

						<p>
							<%= StringUtil.shorten(HtmlUtil.stripHtml(summary), pageAbstractLength) %>
						</p>
					</c:when>
					<c:when test="<%= blogsPortletInstanceConfiguration.displayStyle().equals(BlogsUtil.DISPLAY_STYLE_FULL_CONTENT) || viewSingleEntry %>">
						<article class="container">
							<c:if test="<%= BlogsEntryPermission.contains(permissionChecker, entry, ActionKeys.UPDATE) && viewSingleEntry %>">
								<portlet:renderURL var="editEntryURL" windowState="<%= WindowState.MAXIMIZED.toString() %>">
									<portlet:param name="mvcRenderCommandName" value="/blogs/edit_entry" />
									<portlet:param name="redirect" value="<%= currentURL %>" />
									<portlet:param name="entryId" value="<%= String.valueOf(entry.getEntryId()) %>" />
								</portlet:renderURL>

								<div class="entry-options">
									<aui:button cssClass="icon-monospaced" href="<%= editEntryURL %>" icon="icon-pencil" />
								</div>
							</c:if>

							<div class="entry-content">
								<%= entry.getContent() %>
							</div>

							<liferay-expando:custom-attributes-available
								className="<%= BlogsEntry.class.getName() %>"
							>
								<liferay-expando:custom-attribute-list
									className="<%= BlogsEntry.class.getName() %>"
									classPK="<%= entry.getEntryId() %>"
									editable="<%= false %>"
									label="<%= true %>"
								/>
							</liferay-expando:custom-attributes-available>
						</article>
					</c:when>
				</c:choose>
			</div>

			<c:if test="<%= viewSingleEntry %>">
				<aui:container cssClass='<%= colCssClass + " entry-metadata" %>'>
					<aui:col width="<%= 40 %>">
						<c:if test="<%= blogsPortletInstanceConfiguration.enableRelatedAssets() %>">
							<div class="entry-links">
								<liferay-ui:asset-links
									assetEntryId="<%= (assetEntry != null) ? assetEntry.getEntryId() : 0 %>"
									className="<%= BlogsEntry.class.getName() %>"
									classPK="<%= entry.getEntryId() %>"
								/>
							</div>
						</c:if>
					</aui:col>
				</aui:container>
			</c:if>

			<div class='<%= viewSingleEntry ? "border-top" : StringPool.BLANK %> entry-footer'>
				<div class="container">
					<div class="entry-social">
						<c:if test="<%= !viewSingleEntry && blogsPortletInstanceConfiguration.enableComments() %>">

							<%
							int messagesCount = CommentManagerUtil.getCommentsCount(BlogsEntry.class.getName(), entry.getEntryId());
							%>

							<portlet:renderURL var="viewEntryCommentsURL">
								<portlet:param name="mvcRenderCommandName" value="/blogs/view_entry" />
								<portlet:param name="scroll" value='<%= renderResponse.getNamespace() + "discussionContainer" %>' />
								<portlet:param name="urlTitle" value="<%= entry.getUrlTitle() %>" />
							</portlet:renderURL>

							<div class="comments">
								<c:if test="<%= assetEntry != null %>">
									<i class="icon-eye-open icon-monospaced"></i>

									<span><%= String.valueOf(assetEntry.getViewCount()) %></span>
								</c:if>

								<a href="<%= viewEntryCommentsURL %>">
									<i class="icon-comment icon-monospaced"></i>

									<span><%= String.valueOf(messagesCount) %></span>
								</a>
							</div>
						</c:if>

						<c:if test="<%= blogsPortletInstanceConfiguration.enableRatings() %>">
							<div class="ratings">
								<liferay-ui:ratings
									className="<%= BlogsEntry.class.getName() %>"
									classPK="<%= entry.getEntryId() %>"
								/>
							</div>
						</c:if>

						<c:if test='<%= blogsPortletInstanceConfiguration.enableSocialBookmarks() && socialBookmarksDisplayPosition.equals("bottom") && viewSingleEntry %>'>
							<liferay-util:include page="/blogs/social_bookmarks.jsp" servletContext="<%= application %>" />
						</c:if>

						<c:if test="<%= viewSingleEntry && blogsPortletInstanceConfiguration.enableFlags() %>">
							<div class="flags">
								<liferay-flags:flags
									className="<%= BlogsEntry.class.getName() %>"
									classPK="<%= entry.getEntryId() %>"
									contentTitle="<%= entry.getTitle() %>"
									reportedUserId="<%= entry.getUserId() %>"
								/>
							</div>
						</c:if>
					</div>
				</div>
			</div>
		</div>
	</c:when>
	<c:otherwise>

		<%
		if (searchContainer != null) {
			searchContainer.setTotal(searchContainer.getTotal() - 1);
		}
		%>

	</c:otherwise>
</c:choose>