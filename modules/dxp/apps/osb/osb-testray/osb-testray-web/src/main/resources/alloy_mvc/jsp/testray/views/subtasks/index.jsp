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

<%@ include file="/alloy_mvc/jsp/testray/views/init.jsp" %>

<%@ include file="/alloy_mvc/jsp/testray/views/start.jspf" %>

<liferay-util:include page="/alloy_mvc/jsp/testray/views/header.jsp" servletContext="<%= application %>">
	<liferay-util:param name="title" value="${testrayTaskComposite.name}" />
</liferay-util:include>

<%@ include file="/alloy_mvc/jsp/testray/views/content_start.jspf" %>

<c:choose>
	<c:when test="${testrayTaskComposite.complete}">
		<aui:button-row cssClass="button-row-top">
			<c:if test='${testrayPermission:containsModelAction(themeDisplay, testrayTaskComposite.testrayTask, "updateUsers")}'>
				<portlet:renderURL var="selectUsersURL" windowState="<%= LiferayWindowState.POP_UP.toString() %>">
					<portlet:param name="controller" value="tasks" />
					<portlet:param name="action" value="selectUsers" />
					<portlet:param name="testrayTaskId" value="${testrayTaskComposite.testrayTaskId}" />
				</portlet:renderURL>

				<c:set value='${AlloyLanguageUtil.getUnicode("users")}' var="selectUsersURLTitle" />

				<c:set value="javascript:Liferay.Testray.openWindow('${selectUsersURL}', '${selectUsersURLTitle}', 1000)" var="selectUsersURL" />

				<aui:button disabled="${testrayTaskComposite.testrayBuildArchived}" onClick="${selectUsersURL}" value="assign-users" />
			</c:if>

			<c:if test='${testrayPermission:containsModelAction(themeDisplay, testrayTaskComposite.testrayTask, "updateUsers")}'>
				<portlet:renderURL var="listUsersURL" windowState="<%= LiferayWindowState.POP_UP.toString() %>">
					<portlet:param name="controller" value="tasks" />
					<portlet:param name="action" value="listUsers" />
					<portlet:param name="id" value="${testrayTaskComposite.testrayTaskId}" />
				</portlet:renderURL>

				<c:set value='${AlloyLanguageUtil.getUnicode("users")}' var="listUsersURLTitle" />

				<c:set value="javascript:Liferay.Testray.openWindow('${listUsersURL}', '${listUsersURLTitle}', 1000)" var="listUsersURL" />
			</c:if>

			<c:if test='${testrayPermission:containsModelAction(themeDisplay, testrayTaskComposite.testrayTask, "edit")}'>
				<portlet:renderURL var="editTestrayTaskURL">
					<portlet:param name="controller" value="tasks" />
					<portlet:param name="action" value="edit" />
					<portlet:param name="id" value="${testrayTaskComposite.testrayTaskId}" />
					<portlet:param name="redirect" value="${portletURL}" />
				</portlet:renderURL>

				<aui:button onClick="${editTestrayTaskURL}" value="edit-task" />
			</c:if>

			<c:if test='${testrayPermission:containsModelAction(themeDisplay, testrayTaskComposite.testrayTask, "updateStatus")}'>
				<c:choose>
					<c:when test="${(testrayTaskComposite.status == TestrayTaskConstants.STATUS_IN_ANALYSIS) && (testrayTaskComposite.incompleteScore == 0)}">
						<portlet:actionURL var="updateAnalysisURLComplete">
							<portlet:param name="controller" value="tasks" />
							<portlet:param name="action" value="updateStatus" />
							<portlet:param name="id" value="${testrayTaskComposite.testrayTaskId}" />
							<portlet:param name="redirect" value="${portletURL}" />
							<portlet:param name="statusLabel" value="complete" />
						</portlet:actionURL>

						<aui:button disabled="${testrayTaskComposite.testrayBuildArchived}" onClick="${updateAnalysisURLComplete}" value="complete" />
					</c:when>
					<c:when test="${(testrayTaskComposite.status == TestrayTaskConstants.STATUS_IN_ANALYSIS) && (testrayTaskComposite.incompleteScore > 0)}">
						<portlet:actionURL var="updateAnalysisURLAbandon">
							<portlet:param name="controller" value="tasks" />
							<portlet:param name="action" value="updateStatus" />
							<portlet:param name="id" value="${testrayTaskComposite.testrayTaskId}" />
							<portlet:param name="redirect" value="${portletURL}" />
							<portlet:param name="statusLabel" value="abandoned" />
						</portlet:actionURL>

						<aui:button disabled="${testrayTaskComposite.testrayBuildArchived}" onClick="${updateAnalysisURLAbandon}" value="abandon" />
					</c:when>
					<c:when test="${(testrayTaskComposite.status == TestrayTaskConstants.STATUS_ABANDONED) || (testrayTaskComposite.status == TestrayTaskConstants.STATUS_COMPLETE)}">
						<portlet:actionURL var="updateAnalysisURLComplete">
							<portlet:param name="controller" value="tasks" />
							<portlet:param name="action" value="updateStatus" />
							<portlet:param name="id" value="${testrayTaskComposite.testrayTaskId}" />
							<portlet:param name="redirect" value="${portletURL}" />
							<portlet:param name="statusLabel" value="in-analysis" />
						</portlet:actionURL>

						<aui:button disabled="${testrayTaskComposite.testrayBuildArchived}" onClick="${updateAnalysisURLComplete}" value="reanalyze" />
					</c:when>
				</c:choose>
			</c:if>

			<c:if test='${testrayPermission:containsModelAction(themeDisplay, testrayTaskComposite.testrayTask, "delete")}'>
				<c:if test="${(testrayTaskComposite.status == TestrayTaskConstants.STATUS_ABANDONED) || (testrayTaskComposite.status == TestrayTaskConstants.STATUS_COMPLETE)}">
					<portlet:renderURL var="viewTestrayTaskIndexURL">
						<portlet:param name="controller" value="tasks" />
						<portlet:param name="action" value="index" />
						<portlet:param name="statuses" value="${TestrayTaskConstants.STATUS_IN_ANALYSIS}" />

						<testray:filter-preference
							value='${TestrayFilterUtil.getUserFilterPreferences(pageContext.request, "tasks", "index", testrayProjectId)}'
						/>
					</portlet:renderURL>

					<portlet:actionURL var="deleteTestrayTaskURL">
						<portlet:param name="controller" value="tasks" />
						<portlet:param name="action" value="delete" />
						<portlet:param name="id" value="${testrayTaskComposite.testrayTaskId}" />
						<portlet:param name="redirect" value="${viewTestrayTaskIndexURL}" />
					</portlet:actionURL>

					<aui:button onClick="${deleteTestrayTaskURL}" value="delete" />
				</c:if>
			</c:if>
		</aui:button-row>

		<div class="testray-card">
			<h2 class="testray-data-header">
				<liferay-ui:message key="task-details" />
			</h2>

			<aui:row>
				<aui:col lg="4" md="5" sm="12">
					<dl class="data-list dl-horizontal">
						<dt>
							<liferay-ui:message key="status" />
						</dt>
						<dd>
							<span class="status ${testrayTaskComposite.statusLabel}">
								<liferay-ui:message key="${testrayTaskComposite.statusLabel}" />
							</span>
						</dd>
						<dt>
							<liferay-ui:message key="assigned-users" />
						</dt>
						<dd>
							<div class="row">
								<div class="col-md-1">
									<div class="avatar-container" id="${htmlNamespace}avatarContainer${testrayTaskComposite.testrayTaskId}" onClick="${listUsersURL}" title='<liferay-ui:message key="click-to-see-all-assigned-users" />'></div>
								</div>
							</div>
						</dd>
						<dt>
							<liferay-ui:message key="created" />
						</dt>
						<dd>
							<testray:date
								relative="${true}"
								value="${testrayTaskComposite.createDate}"
							/>
						</dd>
					</dl>
				</aui:col>

				<aui:col lg="8" md="7" sm="12">
					<dl class="data-list dl-horizontal">
						<dt>
							<liferay-ui:message key="project-name" />
						</dt>
						<dd>
							<portlet:renderURL var="viewTestrayProjectURL">
								<portlet:param name="controller" value="routines" />
								<portlet:param name="action" value="index" />
								<portlet:param name="testrayProjectId" value="${testrayTaskComposite.testrayProjectId}" />

								<testray:filter-preference
									value='${TestrayFilterUtil.getUserFilterPreferences(pageContext.request, "routines", "index", testrayProjectId)}'
								/>
							</portlet:renderURL>

							<aui:a cssClass="unstyle-link" href="${viewTestrayProjectURL}" label="${fn:escapeXml(testrayTaskComposite.testrayProjectName)}" localizeLabel="${false}" />
						</dd>
						<dt>
							<liferay-ui:message key="routine-name" />
						</dt>
						<dd>
							<portlet:renderURL var="viewTestrayRoutineURL">
								<portlet:param name="controller" value="builds" />
								<portlet:param name="action" value="index" />
								<portlet:param name="testrayRoutineId" value="${testrayTaskComposite.testrayRoutineId}" />

								<testray:filter-preference
									value='${TestrayFilterUtil.getUserFilterPreferences(pageContext.request, "builds", "index", testrayProjectId)}'
								/>
							</portlet:renderURL>

							<aui:a cssClass="unstyle-link" href="${viewTestrayRoutineURL}" label="${fn:escapeXml(testrayTaskComposite.testrayRoutineName)}" localizeLabel="${false}" />
						</dd>
						<dt>
							<liferay-ui:message key="build-name" />
						</dt>
						<dd>
							<portlet:renderURL var="viewTestrayBuildURL">
								<portlet:param name="controller" value="case_results" />
								<portlet:param name="action" value="index" />
								<portlet:param name="testrayBuildId" value="${testrayTaskComposite.testrayBuildId}" />

								<testray:filter-preference
									value='${TestrayFilterUtil.getUserFilterPreferences(pageContext.request, "case_results", "index", testrayProjectId)}'
								/>
							</portlet:renderURL>

							<aui:a cssClass="unstyle-link" href="${viewTestrayBuildURL}" label="${fn:escapeXml(testrayTaskComposite.testrayBuildName)}" localizeLabel="${false}" />
						</dd>
					</dl>

					<div class="build-metrics metrics-container">
						<div class="metrics-bar small" id="${htmlNamespace}buildMetricsBar${testrayTaskComposite.testrayTaskId}"></div>

						<div class="metrics-legend small" id="${htmlNamespace}buildMetricsLegend${testrayTaskComposite.testrayTaskId}"></div>
					</div>
				</aui:col>
			</aui:row>
		</div>

		<div class="metrics-container testray-card">
			<h2 class="testray-data-header">
				<liferay-ui:message key="progress-score" />
			</h2>

			<div class="metrics-bar" id="${htmlNamespace}metricsBar${testrayTaskComposite.testrayTaskId}"></div>

			<div class="metrics-legend" id="${htmlNamespace}metricsLegend${testrayTaskComposite.testrayTaskId}">
				<div class="legend-item legend-total-container">
					<div class="legend-item-numbers">
						<span class="total">${testrayTaskComposite.completeScore}</span>

						<span class="sub total"><liferay-ui:message arguments="${testrayTaskComposite.totalScore}" key="forward-slash-x" /></span>
					</div>

					<div class="legend-item-label">
						<liferay-ui:message key="total-completed" />
					</div>
				</div>
			</div>
		</div>

		<div class="testray-card">
			<c:set value="${testraySubtasksCount},${testraySubtasksTotalCount}" var="argumentsString" />
			<c:set value="${fn:split(argumentsString, StringPool.COMMA)}" var="arguments" />

			<testray:table-toolbar
				count='${AlloyLanguageUtil.format("showing-x-of-x-total-subtasks", arguments)}'
				title="subtasks"
			>
				<%@ include file="/alloy_mvc/jsp/testray/views/subtasks/index_filter.jspf" %>

				<testray:configure-columns
					columnLabels="${TestraySubtaskConstants.COLUMN_LABELS}"
					columnLabelsDefault="${TestraySubtaskConstants.COLUMN_LABELS_DEFAULT}"
					sessionKey="testraySubtasksIndexColumns"
				/>
			</testray:table-toolbar>

			<div class="inactive row-checker-toolbar-container" id="${htmlNamespace}subtaskRowCheckerToolbar">
				<div class="row-checker-toolbar-content">
					<span class="selected-counter">
						<span class="item-count"></span>

						<liferay-ui:message key="selected" />
					</span>
					<span class="actions-container">
						<c:set value='${AlloyLanguageUtil.get("deselect-items")}' var="clearButtonTitle" />

						<aui:button cssClass="testray-tooltip-trigger" name="rowCheckerActionClear" title="${clearButtonTitle}" value="clear" />

						<c:set value='${AlloyLanguageUtil.get("merge-the-selected-subtasks-into-the-subtask-with-the-higher-score")}' var="mergeButtonTitle" />

						<aui:button cssClass="testray-tooltip-trigger" name="rowCheckerActionMerge" primary="${true}" title="${mergeButtonTitle}" value="merge-subtasks" />
					</span>
				</div>
			</div>

			<c:set value='${testrayPermission:containsModelAction(themeDisplay, testraySubtaskComposites[0].testraySubtask, "merge") ? rowChecker : StringPool.BLANK}' var="mergeRowChecker" />

			<div class="table-error table-row-checker table-row-checker-hide-check-all" id="${htmlNamespace}subtaskRowCheckerSearchContainer">
				<liferay-ui:search-container
					emptyResultsMessage="there-are-no-subtasks"
					id="subtasksSearchContainer"
					iteratorURL="${portletURL}"
					orderByCol="${orderByCol}"
					orderByType="${orderByType}"
					rowChecker="${mergeRowChecker}"
					total="${testraySubtasksCount}"
				>
					<liferay-ui:search-container-results
						results="${testraySubtaskComposites}"
					/>

					<liferay-ui:search-container-row
						className="java.lang.Object"
						escapedModel="${true}"
						keyProperty="testraySubtaskId"
						modelVar="testraySubtaskComposite"
					>
						<portlet:renderURL var="viewTestraySubtaskURL">
							<portlet:param name="controller" value="subtasks" />
							<portlet:param name="action" value="view" />
							<portlet:param name="id" value="${testraySubtaskComposite.testraySubtaskId}" />

							<testray:filter-preference
								value='${TestrayFilterUtil.getUserFilterPreferences(pageContext.request, "subtasks", "view", testrayProjectId)}'
							/>
						</portlet:renderURL>

						<liferay-ui:search-container-column-text
							cssClass="subtask-name table-column-main valign-top"
							href="${viewTestraySubtaskURL}"
							orderable="${true}"
							orderableProperty="name_sortable"
							property="name"
						/>

						<c:if test="${testraySubtasksIndexColumns.contains(TestraySubtaskConstants.COLUMN_LABEL_STATUS)}">
							<liferay-ui:search-container-column-text
								cssClass="subtask-status subtask-status-${testraySubtaskComposite.statusLabel} valign-top"
								name="status"
								orderable="${true}"
								orderableProperty="statusLabel_sortable"
							>
								<aui:a cssClass="status ${testraySubtaskComposite.statusLabel}" href="${viewTestraySubtaskURL}" label="${testraySubtaskComposite.statusLabel}" />
							</liferay-ui:search-container-column-text>
						</c:if>

						<c:if test="${testraySubtasksIndexColumns.contains(TestraySubtaskConstants.COLUMN_LABEL_SCORE)}">
							<liferay-ui:search-container-column-text
								cssClass="subtask-score valign-top"
								href="${viewTestraySubtaskURL}"
								orderable="${true}"
								orderableProperty="score_sortable"
								property="score"
							/>
						</c:if>

						<c:if test="${testraySubtasksIndexColumns.contains(TestraySubtaskConstants.COLUMN_LABEL_TESTS)}">
							<liferay-ui:search-container-column-text
								cssClass="subtask-tests valign-top"
								href="${viewTestraySubtaskURL}"
								name="tests"
								orderable="${true}"
								orderableProperty="testsCount_sortable"
								property="testsCount"
							/>
						</c:if>

						<c:if test="${testraySubtasksIndexColumns.contains(TestraySubtaskConstants.COLUMN_LABEL_ERRORS)}">
							<liferay-ui:search-container-column-text
								cssClass="subtask-errors table-column-errors"
								name="errors"
							>
								<pre>${fn:escapeXml(testraySubtaskComposite.errors)}</pre>
							</liferay-ui:search-container-column-text>
						</c:if>

						<c:if test="${testraySubtasksIndexColumns.contains(TestraySubtaskConstants.COLUMN_LABEL_ASSIGNEE)}">
							<liferay-ui:search-container-column-text
								cssClass="assignee assignee-${testraySubtaskComposite.assignedUserId} valign-top"
								name="assignee"
							>
								<div class="avatar-container" id="${htmlNamespace}avatarContainer${testraySubtaskComposite.testraySubtaskId}">${testraySubtaskComposite.assignedUserName}</div>

								<portlet:actionURL var="assignToMeTestraySubtaskURL">
									<portlet:param name="controller" value="subtasks" />
									<portlet:param name="action" value="updateUser" />
									<portlet:param name="id" value="${testraySubtaskComposite.testraySubtaskId}" />
									<portlet:param name="redirect" value="${portletURL}" />
									<portlet:param name="assignedUserId" value="${themeDisplay.userId}" />
								</portlet:actionURL>

								<aui:script use="testray-avatar">
									var testraySubtaskJSON = ${testraySubtaskComposite.getJSONObject()};

									new Liferay.Testray.Avatar(
										{
											container: '#${htmlNamespace}avatarContainer${testraySubtaskComposite.testraySubtaskId}',
											data: [testraySubtaskJSON.assignedUser],
											<c:if test='${testrayPermission:containsModelAction(themeDisplay, testraySubtaskComposite.testraySubtask, "updateUser")}'>
												emptyLinkConfig: {
													iconCssClass: 'user',
													label: '<liferay-ui:message key="assign-to-me" />',
													url: '${assignToMeTestraySubtaskURL}'
												},
											</c:if>
											showLabel: true
										}
									).render();
								</aui:script>
							</liferay-ui:search-container-column-text>
						</c:if>
					</liferay-ui:search-container-row>

					<liferay-ui:search-iterator />
				</liferay-ui:search-container>
			</div>
		</div>

		<aui:script use="testray-avatar,testray-context-menu,testray-metrics-bar,testray-row-checker-toolbar">
			$(document).ready(
				function() {
					$('[data-toggle="tooltip"]').tooltip();
				}
			);

			var testrayTaskJSON = ${testrayTaskComposite.getJSONObject()};

			new Liferay.Testray.Avatar(
				{
					container: '#${htmlNamespace}avatarContainer${testrayTaskComposite.testrayTaskId}',
					data: testrayTaskJSON.assignedUsers,
					maxVisible: 5
				}
			).render();

			var data = {};

			<c:forEach items="${testrayTaskComposite.testrayBuildComposite.testrayCaseResultReporter.simplifiedTestrayCaseResultStatusCounts}" var="statusCount">
				data['${statusCount.key}'] = {
					label: '<liferay-ui:message key="${statusCount.key}" />',
					value: ${statusCount.value}
				};
			</c:forEach>

			new Liferay.Testray.MetricsBar(
				{
					container: '#${htmlNamespace}buildMetricsBar${testrayTaskComposite.testrayTaskId}',
					data: data,
					legendContainer: '#${htmlNamespace}buildMetricsLegend${testrayTaskComposite.testrayTaskId}'
				}
			).render();

			new Liferay.Testray.MetricsBar(
				{
					container: '#${htmlNamespace}metricsBar${testrayTaskComposite.testrayTaskId}',
					data: {
						'incomplete': {
							label: '<liferay-ui:message key="incomplete" />',
							value: ${testrayTaskComposite.incompleteScore}
						},
						'others-completed': {
							label: '<liferay-ui:message key="others-completed" />',
							value: ${testrayTaskComposite.completeScore} - ${testrayTaskComposite.currentUserCompleteScore}
						},
						'self-completed': {
							label: '<liferay-ui:message key="self-completed" />',
							value: ${testrayTaskComposite.currentUserCompleteScore}
						}
					},
					legendContainer: '#${htmlNamespace}metricsLegend${testrayTaskComposite.testrayTaskId}',
					total: ${testrayTaskComposite.totalScore}
				}
			).render();

			new Liferay.Testray.ContextMenu(
				{
					containerId: '${htmlNamespace}subtasksSearchContainer',
					menu: [
						{
							action: 'view',
							controller: 'subtasks',
							iconCssClass: 'icon-file',
							label: '<liferay-ui:message key="view" />',
							urlTemplate: '/{id}',
							userFilterPreferences: ${TestrayFilterUtil.getUserFilterPreferences(pageContext.request, "subtasks", "view", testrayTaskComposite.testrayProjectId)}
						},
						{
							action: 'updateUser',
							controller: 'subtasks',
							iconCssClass: 'icon-user',
							label: '<liferay-ui:message key="assign-to-me-and-begin-analysis" />',
							method: 'POST',
							name: 'subtaskAssignToMeAndBeginAnalysis',
							parameters: {
								assignedUserId: '${themeDisplay.userId}'
							},
							visible: ${testrayPermission:containsModelAction(themeDisplay, testraySubtaskComposite.testraySubtask, "updateUser")}
						},
						{
							action: 'updateUser',
							controller: 'subtasks',
							iconCssClass: 'icon-user',
							label: '<liferay-ui:message key="assign-to-me-and-reanalyze" />',
							method: 'POST',
							name: 'subtaskAssignToMeAndReanalyze',
							parameters: {
								assignedUserId: '${themeDisplay.userId}'
							},
							visible: ${testrayPermission:containsModelAction(themeDisplay, testraySubtaskComposite.testraySubtask, "updateUser")}
						},
						{
							action: 'select',
							controller: 'users',
							iconCssClass: 'icon-user',
							label: '<liferay-ui:message key="assign" />',
							menuAction: 'modal',
							modalConfig: {
								title: '<liferay-ui:message key="users" />'
							},
							name: 'subtaskAssign',
							urlTemplate: '/select?submitIdValue={id}&submitAction=updateUser&submitController=subtasks&submitIdName=id',
							visible: ${testrayPermission:containsModelAction(themeDisplay, testraySubtaskComposite.testraySubtask, "updateUser")}
						},
						{
							action: 'select',
							controller: 'users',
							iconCssClass: 'icon-user',
							label: '<liferay-ui:message key="assign-and-begin-analysis" />',
							menuAction: 'modal',
							modalConfig: {
								title: '<liferay-ui:message key="users" />'
							},
							name: 'subtaskAssignAndBeginAnalysis',
							urlTemplate: '/select?submitIdValue={id}&submitAction=updateUser&submitController=subtasks&submitIdName=id',
							visible: ${testrayPermission:containsModelAction(themeDisplay, testraySubtaskComposite.testraySubtask, "updateUser")}
						},
						{
							action: 'select',
							controller: 'users',
							iconCssClass: 'icon-user',
							label: '<liferay-ui:message key="assign-and-reanalyze" />',
							menuAction: 'modal',
							modalConfig: {
								title: '<liferay-ui:message key="users" />'
							},
							name: 'subtaskAssignAndReanalyze',
							urlTemplate: '/select?submitIdValue={id}&submitAction=updateUser&submitController=subtasks&submitIdName=id',
							visible: ${testrayPermission:containsModelAction(themeDisplay, testraySubtaskComposite.testraySubtask, "updateUser")}
						},
						{
							action: 'updateUser',
							controller: 'subtasks',
							iconCssClass: 'icon-tasks',
							label: '<liferay-ui:message key="return-to-open" />',
							method: 'POST',
							name: 'subtaskReturnToOpen',
							parameters: {
								assignedUserId: '0'
							},
							visible: ${testrayPermission:containsModelAction(themeDisplay, testraySubtaskComposite.testraySubtask, "updateUser")}
						},
						{
							action: 'editStatus',
							controller: 'subtasks',
							iconCssClass: 'icon-tasks',
							label: '<liferay-ui:message key="complete" />',
							menuAction: 'modal',
							modalConfig: {
								title: '<liferay-ui:message key="edit-status" />'
							},
							name: 'subtaskComplete',
							visible: ${testrayPermission:containsModelAction(themeDisplay, testraySubtaskComposite.testraySubtask, "editStatus")}
						}
					],
					redirect: '${portletURL}'
				}
			).render();

			<c:if test='${testrayPermission:containsModelAction(themeDisplay, testraySubtaskComposite.testraySubtask, "merge")}'>
				new Liferay.Testray.RowCheckerToolbar(
					{
						actionMerge: {
							button: '#rowCheckerActionMerge',
							redirect: '${portletURL}',
							testrayTaskId: ${testrayTaskComposite.testrayTaskId}
						},
						alertContainerId: '#${htmlNamespace}testrayAlertContainer',
						container: '#${htmlNamespace}subtaskRowCheckerToolbar',
						searchContainerId: 'subtasksSearchContainer',
						searchContainerWrapper: '#${htmlNamespace}subtaskRowCheckerSearchContainer',
						selectAllCheckbox: '#${htmlNamespace}subtaskRowCheckerSearchContainer [name="allRowIds"]'
					}
				).render();
			</c:if>
		</aui:script>
	</c:when>
	<c:otherwise>
		<div class="testray-loading-section">
			<div class="linear loading-icon loading-icon-md"></div>

			<div id="testrayTaskLoadingContainer"></div>

			<h2 class="testray-loading-message">
				<liferay-ui:message key="preparing-your-task" />
			</h2>

			<div class="testray-loading-message">
				<portlet:actionURL var="reindexTestraySubtasksURL">
					<portlet:param name="controller" value="subtasks" />
					<portlet:param name="action" value="reindex" />
					<portlet:param name="testrayTaskId" value="${testrayTaskComposite.testrayTaskId}" />
					<portlet:param name="redirect" value="${portletURL}" />
				</portlet:actionURL>

				<aui:button href="${reindexTestraySubtasksURL}" value="refresh" />
			</div>
		</div>

		<aui:script use="testray-loading-bar">
			new Liferay.Testray.LoadingBar(
				{
					container: '#testrayTaskLoadingContainer',
					requestConfig: {
						controller: 'tasks',
						controllerMethod: 'progress.json',
						params: {
							id: ${testrayTaskComposite.testrayTaskId}
						}
					},
					totalName: 'totalSubtasks',
					valueName: 'completeSubtasks'
				}
			).render();
		</aui:script>
	</c:otherwise>
</c:choose>

<%@ include file="/alloy_mvc/jsp/testray/views/content_end.jspf" %>

<%@ include file="/alloy_mvc/jsp/testray/views/end.jspf" %>