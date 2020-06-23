<!DOCTYPE html>

<#include init />

<html class="${root_css_class}" dir='<@liferay.language key="lang.dir" />' lang="${w3c_language_id}">

<head>
	<title>${the_title}</title>

	<meta content="initial-scale=1.0, width=device-width" name="viewport" />

	<@liferay_util["include"] page=top_head_include />
</head>

<body class="${css_class}">

<@liferay_ui["quick-access"] contentId="#main-content" />

<@liferay_util["include"] page=body_top_include />

<#if validator.isNotNull(google_tag_manager_id)>
	<noscript><iframe height="0" src="//www.googletagmanager.com/ns.html?id=${google_tag_manager_id}" style="display: none; visibility: hidden" width="0"></iframe></noscript>

	<script>
		(function(w, d, s, l, i) {
			w[l] = w[l] || [];

			w[l].push(
				{
					event: 'gtm.js',
					'gtm.start': new Date().getTime()
				}
			);

			var dl = l != 'dataLayer' ? '&l=' + l : '';
			var f = d.getElementsByTagName(s)[0];
			var j = d.createElement(s);

			j.async = true;

			j.src = '//www.googletagmanager.com/gtm.js?id=' + i + dl;

			f.parentNode.insertBefore(j, f);
		})(window, document, 'script', 'dataLayer', '${google_tag_manager_id}');
	</script>
</#if>

<div class="container-fluid" id="wrapper">
	<section id="content">
		<h1 class="hide-accessible">${the_title}</h1>

		<#if selectable>
			<@liferay_util["include"] page=content_include />
		<#else>
			${portletDisplay.recycle()}

			${portletDisplay.setTitle(the_title)}

			<@liferay_theme["wrap-portlet"] page="portlet.ftl">
				<@liferay_util["include"] page=content_include />
			</@>
		</#if>
	</section>
</div>

<@liferay_util["include"] page=body_bottom_include />

<@liferay_util["include"] page=bottom_include />

</body>

</html>