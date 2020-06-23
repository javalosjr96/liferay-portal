<nav class="${nav_css_class}" id="navigation" role="navigation">
	<h1 class="hide-accessible"><@liferay.language key="navigation" /></h1>

	<div class="container">
		<div class="navbar">
			<div class="navbar-logo">
				<a class="navbar-logo-link" href="${site_default_url}" title='<@liferay.language_format arguments=" ${site_name} " key="go-to-x" />'>
					<img alt="${logo_description}" class="navbar-logo-image" src="${images_folder}/logo.svg" />
				</a>
			</div>

			<nav class="navbar-menu">
				<a class="navbar-toggler" data-content="#wrapper" data-toggle="sidenav" data-type="fixed-push" href="#mobileSidenav" id="mobileSidenavOpen">
					<@liferay_aui.icon
						image="bars"
						markupView="lexicon"
					/>
				</a>

				<ul aria-label='<@liferay.language key="site-pages" />' class="navbar-list" role="menubar">
					<#list nav_items as nav_item>
						<#assign navbar_link_class = "navbar-link" />

						<#if nav_item.isSelected()>
							<#assign navbar_link_class = "navbar-link navbar-link-selected" />
						</#if>

						<li class="navbar-item">
							<a class="${navbar_link_class}" href="${nav_item.getURL()}" ${nav_item.getTarget()}>
								<span>${nav_item.getName()}</span></a>
						</li>
					</#list>

					<#if !is_signed_in>
						<li class="navbar-item">
							<a class="navbar-link" data-redirect="${is_login_redirect_required?string}" href="${sign_in_url}" id="sign-in" rel="nofollow">
								<span>${sign_in_text}</span>
							</a>
						</li>
					<#else>
						<li class="navbar-item">
							<@liferay.user_personal_bar />
						</li>
					</#if>
				</ul>
			</nav>
		</div>
	</div>
</nav>

<div class="closed sidenav-fixed sidenav-menu-slider sidenav-right" id="mobileSidenav">
	<div class="sidebar sidebar-default sidenav-menu">
		<div class="sidenav-header">
			<div class="user-personal-bar">
				<@liferay.user_personal_bar />

				<a class="close-sidenav pull-right" data-content="#wrapper" data-toggle="sidenav" data-type="fixed-push" href="#mobileSidenav" id="mobileSidenavClose">
					<@liferay_aui.icon
						image="times"
						markupView="lexicon"
					/>
				</a>
			</div>
		</div>

		<#list nav_items as sidenav_item>
			<#assign
				sidenav_item_attr_selected = "false"
				sidenav_item_css_class = ""
			/>

			<#if sidenav_item.isSelected()>
				<#assign
					sidenav_item_attr_selected = "true"
					sidenav_item_css_class = "active"
				/>
			</#if>

			<div aria-selected="${sidenav_item_attr_selected}" class="${sidenav_item_css_class} sidenav-item" id="layout_${sidenav_item.getName()}" role="presentation">
				<a aria-labelledby="layout_${sidenav_item.getName()}" class="sidenav-item-link" href="${sidenav_item.getURL()}" ${sidenav_item.getTarget()} role="menuitem">${sidenav_item.getName()}</a>
			</div>
		</#list>

		<#if is_signed_in>
			<div class="logout sidenav-item">
				<a class="sidenav-item-link" href="/c/portal/logout"><@liferay.language key="sign-out" /></a>
			</div>
		</#if>
	</div>
</div>