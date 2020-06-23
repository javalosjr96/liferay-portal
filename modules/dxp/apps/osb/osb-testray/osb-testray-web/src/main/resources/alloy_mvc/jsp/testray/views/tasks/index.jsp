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

<%@ include file="/alloy_mvc/jsp/testray/views/tasks/header.jspf" %>

<%@ include file="/alloy_mvc/jsp/testray/views/content_start.jspf" %>

<div class="testray-card">
	<testray:table-toolbar
		title="tasks"
	>
		<%@ include file="/alloy_mvc/jsp/testray/views/tasks/index_filter.jspf" %>

		<testray:configure-columns
			columnLabels="${TestrayTaskConstants.COLUMN_LABELS}"
			columnLabelsDefault="${TestrayTaskConstants.COLUMN_LABELS_DEFAULT}"
			sessionKey="testrayTasksIndexColumns"
		/>

		<c:if test='${testrayPermission:containsClassAction(themeDisplay, testrayTaskClass, "create")}'>
			<portlet:renderURL var="selectBuildURL" windowState="<%= LiferayWindowState.POP_UP.toString() %>">
				<portlet:param name="controller" value="tasks" />
				<portlet:param name="action" value="selectBuild" />
			</portlet:renderURL>

			<c:set value='${AlloyLanguageUtil.getUnicode("select-build")}' var="selectBuildURLTitle" />

			<c:set value="javascript:Liferay.Testray.openWindow('${selectBuildURL}', '${selectBuildURLTitle}', 1000)" var="selectBuildURL" />

			<aui:button icon="icon-plus" onClick="${selectBuildURL}" primary="${true}" value="new-task" />
		</c:if>
	</testray:table-toolbar>

	<liferay-ui:search-container
		emptyResultsMessage="there-are-no-tasks"
		id="tasksSearchContainer"
		iteratorURL="${alloySearchResult.portletURL}"
		orderByCol="${orderByCol}"
		orderByType="${orderByType}"
		total="${alloySearchResult.size}"
	>
		<liferay-ui:search-container-results
			results="${testrayTaskComposites}"
		/>

		<liferay-ui:search-container-row
			className="java.lang.Object"
			escapedModel="${true}"
			keyProperty="testrayTaskId"
			modelVar="testrayTaskComposite"
		>
			<c:set value="<%= StringUtil.merge(TestraySubtaskConstants.STATUSES_DEFAULT) %>" var="defaultSubtaskStatuses" />

			<portlet:renderURL var="viewTestraySubtasksURL">
				<portlet:param name="controller" value="subtasks" />
				<portlet:param name="action" value="index" />
				<portlet:param name="statuses" value="${(testrayTaskComposite.status == TestrayTaskConstants.STATUS_IN_ANALYSIS) ? defaultSubtaskStatuses : StringPool.BLANK}" />
				<portlet:param name="testrayTaskId" value="${testrayTaskComposite.testrayTaskId}" />

				<testray:filter-preference
					value='${TestrayFilterUtil.getUserFilterPreferences(pageContext.request, "subtasks", "index", testrayTaskComposite.testrayProjectId)}'
				/>
			</portlet:renderURL>

			<c:if test='${testrayPermission:containsModelAction(themeDisplay, testrayTaskComposite.testrayTask, "updateUsers")}'>
				<portlet:renderURL var="listUsersURL" windowState="<%= LiferayWindowState.POP_UP.toString() %>">
					<portlet:param name="controller" value="tasks" />
					<portlet:param name="action" value="listUsers" />
					<portlet:param name="id" value="${testrayTaskComposite.testrayTaskId}" />
				</portlet:renderURL>

				<c:set value='${AlloyLanguageUtil.getUnicode("users")}' var="listUsersURLTitle" />

				<c:set value="javascript:Liferay.Testray.openWindow('${listUsersURL}', '${listUsersURLTitle}', 1000)" var="listUsersURL" />
			</c:if>

			<c:if test="${testrayTasksIndexColumns.contains(TestrayTaskConstants.COLUMN_LABEL_STATUS)}">
				<liferay-ui:search-container-column-text
					href="${!param.archived ? viewTestraySubtasksURL : null}"
					name="status"
					orderable="${true}"
					orderableProperty="statusLabel_sortable"
				>
					<aui:a cssClass="status ${testrayTaskComposite.statusLabel}" href="${!param.archived ? viewTestraySubtasksURL : null}" label="${testrayTaskComposite.statusLabel}" />
				</liferay-ui:search-container-column-text>
			</c:if>

			<c:if test="${testrayTasksIndexColumns.contains(TestrayTaskConstants.COLUMN_LABEL_START_DATE)}">
				<liferay-ui:search-container-column-text
					href="${!param.archived ? viewTestraySubtasksURL : null}"
					name="start-date"
					orderable="${true}"
					orderableProperty="createDate_sortable"
				>
					<testray:date
						relative="${true}"
						value="${testrayTaskComposite.createDate}"
					/>
				</liferay-ui:search-container-column-text>
			</c:if>

			<liferay-ui:search-container-column-text
				cssClass="table-column-main"
				href="${!param.archived ? viewTestraySubtasksURL : null}"
				name="task"
				orderable="${true}"
				orderableProperty="name_sortable"
				value="${fn:escapeXml(testrayTaskComposite.name)}"
			/>

			<c:if test="${testrayTasksIndexColumns.contains(TestrayTaskConstants.COLUMN_LABEL_PROJECT_NAME)}">
				<liferay-ui:search-container-column-text
					href="${!param.archived ? viewTestraySubtasksURL : null}"
					name="project-name"
					orderable="${true}"
					orderableProperty="testrayProjectName_sortable"
					value="${fn:escapeXml(testrayTaskComposite.testrayProjectName)}"
				/>
			</c:if>

			<c:if test="${testrayTasksIndexColumns.contains(TestrayTaskConstants.COLUMN_LABEL_ROUTINE_NAME)}">
				<liferay-ui:search-container-column-text
					href="${!param.archived ? viewTestraySubtasksURL : null}"
					name="routine-name"
					orderable="${true}"
					orderableProperty="testrayRoutineName_sortable"
					value="${fn:escapeXml(testrayTaskComposite.testrayRoutineName)}"
				/>
			</c:if>

			<c:if test="${testrayTasksIndexColumns.contains(TestrayTaskConstants.COLUMN_LABEL_BUILD_NAME)}">
				<liferay-ui:search-container-column-text
					cssClass="archived-status archived-status-${param.archived} task-status task-status-${testrayTaskComposite.statusLabel} testrayBuildId testrayBuildId-${testrayTaskComposite.testrayBuildId}"
					href="${!param.archived ? viewTestraySubtasksURL : null}"
					name="build-name"
					orderable="${true}"
					orderableProperty="testrayBuildName_sortable"
					value="${fn:escapeXml(testrayTaskComposite.testrayBuildName)}"
				/>
			</c:if>

			<c:if test="${testrayTasksIndexColumns.contains(TestrayTaskConstants.COLUMN_LABEL_SCORE)}">
				<liferay-ui:search-container-column-text
					cssClass="incomplete-score incomplete-score-${testrayTaskComposite.incompleteScore}"
					name="score"
				>
					<c:set var="arguments">
						${testrayTaskComposite.completeScore};${testrayTaskComposite.totalScore}
					</c:set>

					<span class="score-label">
						<liferay-ui:message arguments="${fn:split(arguments, StringPool.SEMICOLON)}" key="x-slash-x" />

						<span class="score-percent" id="${htmlNamespace}completedPercent${testrayTaskComposite.testrayTaskId}"></span>
					</span>

					<aui:script use="testray-base">
						Liferay.Testray.addPercentLabel(
							{
								container: '#${htmlNamespace}completedPercent${testrayTaskComposite.testrayTaskId}',
								total: ${testrayTaskComposite.totalScore},
								value: ${testrayTaskComposite.completeScore}
							}
						);
					</aui:script>
				</liferay-ui:search-container-column-text>
			</c:if>

			<c:if test="${testrayTasksIndexColumns.contains(TestrayTaskConstants.COLUMN_LABEL_PROGRESS)}">
				<liferay-ui:search-container-column-text
					cssClass="metrics metrics-compact"
					name="progress"
				>
					<div class="progress-new">
						<progress class="progress-new" max="100" role="progressbar" value="10"></progress>
					</div>

					<div class="metrics-bar" id="${htmlNamespace}metricsBar${testrayTaskComposite.testrayTaskId}"></div>

					<aui:script use="testray-metrics-bar">
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
								total: ${testrayTaskComposite.totalScore}
							}
						).render();
					</aui:script>
				</liferay-ui:search-container-column-text>
			</c:if>

			<c:if test="${testrayTasksIndexColumns.contains(TestrayTaskConstants.COLUMN_LABEL_ASSIGNED_USERS)}">
				<liferay-ui:search-container-column-text
					cssClass="assignee-portraits"
					name="assigned-users"
				>
					<div class="row">
						<div class="col-md-1">
							<div class="avatar-container" id="${htmlNamespace}avatarContainer${testrayTaskComposite.testrayTaskId}" onClick="${listUsersURL}" title='<liferay-ui:message key="click-to-see-all-assigned-users" />'></div>
						</div>
					</div>

					<aui:script use="testray-avatar">
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
					</aui:script>
				</liferay-ui:search-container-column-text>
			</c:if>
		</liferay-ui:search-container-row>

		<liferay-ui:search-iterator />
	</liferay-ui:search-container>
</div>

<%@ include file="/alloy_mvc/jsp/testray/views/content_end.jspf" %>

<%@ include file="/alloy_mvc/jsp/testray/views/end.jspf" %>

<aui:script use="testray-context-menu">
	new Liferay.Testray.ContextMenu(
		{
			containerId: '${htmlNamespace}tasksSearchContainer',
			menu: [
				{
					action: 'index',
					controller: 'subtasks',
					iconCssClass: 'icon-file',
					label: '<liferay-ui:message key="view" />',
					name: 'view',
					parameters: {
						statuses: '${(testrayTaskComposite.status == TestrayTaskConstants.STATUS_IN_ANALYSIS) ? defaultSubtaskStatuses : StringPool.BLANK}'
					},
					urlTemplate: '?${htmlNamespace}testrayTaskId={id}',
					userFilterPreferences: ${TestrayFilterUtil.getUserFilterPreferences(pageContext.request, "subtasks", "index", testrayTaskComposite.testrayProjectId)},
					conditionalVisibility: ${true}
				},
				{
					action: 'index',
					controller: 'runs',
					iconCssClass: 'icon-file',
					label: '<liferay-ui:message key="view-associated-build" />',
					name: 'viewAssociatedBuild',
					parametersCss: ['testrayBuildId'],
					urlTemplate: '/index',
					userFilterPreferences: ${TestrayFilterUtil.getUserFilterPreferences(pageContext.request, "runs", "index", testrayTaskComposite.testrayProjectId)},
					visible: ${!param.archived}
				},
				{
					action: 'edit',
					controller: 'tasks',
					iconCssClass: 'icon-edit',
					label: '<liferay-ui:message key="edit" />',
					urlTemplate: '/{id}/edit',
					visible: ${testrayPermission:containsModelAction(themeDisplay, testrayTaskComposite.testrayTask, "edit")}
				},
				{
					action: 'updateStatus',
					controller: 'tasks',
					iconCssClass: 'icon-tasks',
					label: '<liferay-ui:message key="complete" />',
					method: 'POST',
					name: 'taskComplete',
					parameters: {
						statusLabel: '${TestrayTaskConstants.LABEL_COMPLETE}'
					},
					urlTemplate: '/{id}/updateStatus',
					visible: ${testrayPermission:containsModelAction(themeDisplay, testrayTaskComposite.testrayTask, "updateStatus")}
				},
				{
					action: 'updateStatus',
					controller: 'tasks',
					iconCssClass: 'icon-tasks',
					label: '<liferay-ui:message key="abandon" />',
					method: 'POST',
					name: 'taskAbandon',
					parameters: {
						statusLabel: '${TestrayTaskConstants.LABEL_ABANDONED}'
					},
					urlTemplate: '/{id}/updateStatus',
					visible: ${testrayPermission:containsModelAction(themeDisplay, testrayTaskComposite.testrayTask, "updateStatus")}
				},
				{
					action: 'updateStatus',
					controller: 'tasks',
					iconCssClass: 'icon-tasks',
					label: '<liferay-ui:message key="reanalyze" />',
					method: 'POST',
					name: 'taskReopen',
					parameters: {
						statusLabel: '${TestrayTaskConstants.LABEL_IN_ANALYSIS}'
					},
					urlTemplate: '/{id}/updateStatus',
					visible: ${testrayPermission:containsModelAction(themeDisplay, testrayTaskComposite.testrayTask, "updateStatus")}
				},
				{
					action: 'delete',
					controller: 'tasks',
					iconCssClass: 'icon-trash',
					label: '<liferay-ui:message key="delete" />',
					method: 'POST',
					name: 'taskDelete',
					urlTemplate: '/{id}/delete',
					visible: ${testrayPermission:containsModelAction(themeDisplay, testrayTaskComposite.testrayTask, "delete")}
				}
			],
			redirect: '${alloySearchResult.portletURL}'
		}
	).render();
</aui:script>