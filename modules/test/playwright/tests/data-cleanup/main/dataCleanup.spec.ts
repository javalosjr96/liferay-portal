/**
* SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
* SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
*/

import {expect, mergeTests} from '@playwright/test';

import {applicationsMenuPageTest} from '../../../fixtures/applicationsMenuPageTest';
import {serverAdministrationPageTest} from '../../../fixtures/serverAdministrationPageTest';
import {loginTest} from '../../../fixtures/loginTest';

export const test = mergeTests(
	loginTest(),
	applicationsMenuPageTest,
	serverAdministrationPageTest
	);

test('execute all system cleanup actions', async ({ page, applicationsMenuPage }) => {
	await applicationsMenuPage.goToServerAdministration();

	const cleanupPanel = page.locator('.card, .panel',
	{ has: page.getByText('System Cleanup Actions') }).last();
	const panelHeader = cleanupPanel.getByRole('button',
	{ name: /System Cleanup Actions/i });

	if (await panelHeader.getAttribute('aria-expanded') === 'false') {
	await panelHeader.click();
	await expect(panelHeader).toHaveAttribute('aria-expanded', 'true');
	}

	const executeButtons = cleanupPanel.getByRole('button', { name: 'Execute' });
	const count = await executeButtons.count();

	for (let i = 0; i < count; i++) {

		const button = executeButtons.nth(i);

		await button.click();

		const successMessage =
		page.getByText('Success:Your request completed successfully.').first();

		await expect(successMessage).toBeVisible();
	}
});

test('execute all module cleanup actions', async ({ page, applicationsMenuPage,serverAdministrationPage }) => {
    await applicationsMenuPage.goToServerAdministration();

    const SERVLET_CONTEXT_NAMES = [
           "com.liferay.amazon.rankings.web",
           "com.liferay.document.library.file.rank.service",
           "com.liferay.chat.service", "com.liferay.currency.converter.web",
           "com.liferay.dictionary.web", "com.liferay.directory.web",
           "com.liferay.frontend.image.editor.web", "com.liferay.google.maps.web",
           "com.liferay.hello.velocity.web", "com.liferay.hello.world.web",
           "com.liferay.html.preview.service", "com.liferay.invitation.web",
           "com.liferay.loan.calculator.web", "com.liferay.mail.reader.service",
           "com.liferay.network.utilities.web", "com.liferay.oauth.service",
           "com.liferay.password.generator.web",
           "com.liferay.portal.security.wedeploy.auth.service",
           "com.liferay.quick.note.web", "com.liferay.recent.documents.web",
           "com.liferay.shopping.service", "com.liferay.social.activity.web",
           "com.liferay.social.group.statistics.web",
           "com.liferay.social.privatemessaging.service",
           "com.liferay.social.requests.web",
           "com.liferay.social.user.statistics.web",
           "com.liferay.softwarecatalog.service", "com.liferay.translator.web",
           "com.liferay.twitter.service", "com.liferay.unit.converter.web",
           "com.liferay.weather.web", "com.liferay.web.form.web",
           "com.liferay.web.proxy.web", "com.liferay.wysiwyg.web",
           "com.liferay.xsl.content.web", "com.liferay.youtube.web",
           "opensocial-portlet"
           ];

    const addReleasesScript = `
       import com.liferay.portal.kernel.service.ReleaseLocalServiceUtil
       import com.liferay.portal.kernel.model.Release

       def servletContextNames = ${JSON.stringify(SERVLET_CONTEXT_NAMES)}

       for(String servletContextName : servletContextNames) {
       Release release = ReleaseLocalServiceUtil.fetchRelease(servletContextName);

       if(release == null){
       ReleaseLocalServiceUtil.addRelease(servletContextName,"1.0.0");
       }
       }
    `;

    await serverAdministrationPage.executeScript(releaseScript);

	const refreshDataCleanupRegistratorScript = `
		import com.liferay.portal.kernel.module.util.BundleUtil
		import com.liferay.portal.kernel.module.util.SystemBundleUtil
		import org.osgi.framework.FrameworkUtil
		import org.osgi.service.component.runtime.ServiceComponentRuntime
		import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO
		import org.osgi.util.promise.Promise

		def bundleContext = FrameworkUtil.getBundle(ServiceComponentRuntime.class).getBundleContext()

		def serviceRef = bundleContext.getServiceReference(ServiceComponentRuntime.class)

		def serviceComponentRuntime = bundleContext.getService(serviceRef)

		try {
			ComponentDescriptionDTO componentDescriptionDTO =
					serviceComponentRuntime.getComponentDescriptionDTO(
							BundleUtil.getBundle(
									SystemBundleUtil.getBundleContext(),
									"com.liferay.data.cleanup.impl"),
							"com.liferay.data.cleanup.internal.DataCleanupRegistrator");

			Promise<Void> promise = serviceComponentRuntime.disableComponent(
					componentDescriptionDTO);

			promise.getValue();

			promise = serviceComponentRuntime.enableComponent(
					componentDescriptionDTO);

			promise.getValue();

		} finally {
			if (serviceRef != null) {
				bundleContext.ungetService(serviceRef)
			}
		}
	`;

	await serverAdministrationPage.executeScript(resetScript);

    await applicationsMenuPage.goToServerAdministration();

    const cleanupPanel = page.locator('.card, .panel',
       { has: page.getByText('Module Cleanup Actions') }).last();

    const panelHeader = cleanupPanel.getByRole('button',
       { name: /Module Cleanup Actions/i });

    if (await panelHeader.getAttribute('aria-expanded') === 'false') {
       await panelHeader.click();
       await expect(panelHeader).toHaveAttribute('aria-expanded', 'true');
    }

    const executeButtons = cleanupPanel.getByRole('button',
       { name: 'Execute' });

    const count = await executeButtons.count();

    for (let i = 0; i < count; i++) {
       const button = executeButtons.nth(i);
       await button.click();

       const successMessage =
       page.getByText('Success:Your request completed successfully.').first();

       await expect(successMessage).toBeVisible();
    }

    const deleteReleases = `
       import com.liferay.portal.kernel.service.ReleaseLocalServiceUtil
       import com.liferay.portal.kernel.model.Release

       def servletContextNames = ${JSON.stringify(SERVLET_CONTEXT_NAMES)}

       for(String servletContextName : servletContextNames) {
       Release release = ReleaseLocalServiceUtil.fetchRelease(servletContextName);

       if(release != null){
       ReleaseLocalServiceUtil.deleteReleases(servletContextName,"1.0.0");
       }
       }
    `;
});

