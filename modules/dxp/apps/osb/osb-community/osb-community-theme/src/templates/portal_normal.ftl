<!DOCTYPE html>

<#include init />

<html class="${root_css_class}" dir='<@liferay.language key="lang.dir" />' lang="${w3c_language_id}">

<head>
	<title>${the_title} - ${company_name}</title>

	<meta content="initial-scale=1.0, width=device-width" name="viewport" />

	<@liferay_util["include"] page=top_head_include />

	<link href="https://fonts.googleapis.com/css?family=Source+Sans+Pro:400,500,600,700" rel="stylesheet">
</head>

<body class="${css_class}">

<#if validator.isNotNull(google_tag_manager_id)>
	<noscript>
		<iframe height="0" src="//www.googletagmanager.com/ns.html?id=${google_tag_manager_id}" style="display:none;visibility:hidden" width="0"></iframe>
	</noscript>

	<#if validator.isNotNull(google_tag_manager_id)>
		<noscript>
			<iframe height="0" src="//www.googletagmanager.com/ns.html?id=${google_tag_manager_id}" style="display:none;visibility:hidden" width="0"></iframe>
		</noscript>

		<script>
			(function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({'gtm.start':new Date().getTime(),event:'gtm.js'});var f=d.getElementsByTagName(s)[0],j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';j.async=true;j.src='//www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);})(window,document,'script','dataLayer','${google_tag_manager_id}');
		</script>
	</#if>
</#if>

<@liferay_ui["quick-access"] contentId="#main-content" />

<@liferay_util["include"] page=body_top_include />

<@liferay.control_menu />

<div class="site" id="wrapper">
	<header class="banner" role="banner">
		<#include "${full_templates_path}/navigation.ftl" />
	</header>

	<section class="content">
		<@liferay_util["include"] page=content_include />
	</section>

	<footer class="footer" role="contentinfo">
		<#include "${full_templates_path}/footer.ftl" />
	</footer>
</div>

<@liferay_util["include"] page=body_bottom_include />

<@liferay_util["include"] page=bottom_include />

<!-- inject:js -->

<!-- endinject -->

</body>
</html>