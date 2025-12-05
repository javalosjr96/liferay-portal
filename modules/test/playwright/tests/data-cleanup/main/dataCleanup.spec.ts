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
	//Go to Server Admin Page

	await applicationsMenuPage.goToServerAdministration();

	//Execute System Cleanup Actions

	await executeCleanupActions(page, 'System Cleanup Actions');

});

test('execute all module cleanup actions', async ({ page, applicationsMenuPage,serverAdministrationPage }) => {
	//Go to Server Admin Page

    await applicationsMenuPage.goToServerAdministration();

    //Add releases for Module Cleanup Actions

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

       if (release == null) {
       ReleaseLocalServiceUtil.addRelease(servletContextName,"1.0.0");
       }
       }
    `;

    try {
		await serverAdministrationPage.executeScript(addReleasesScript);

		//Reset Data Cleanup Registrator Component

		const resetDataCleanupRegistratorScript = `
			import com.liferay.portal.kernel.module.util.BundleUtil
			import com.liferay.portal.kernel.module.util.SystemBundleUtil
			import org.osgi.framework.FrameworkUtil
			import org.osgi.service.component.runtime.ServiceComponentRuntime
			import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO
			import org.osgi.util.promise.Promise

			def bundle = FrameworkUtil.getBundle(ServiceComponentRuntime.class)

			def bundleContext = bundle.getBundleContext()

			def serviceReference = bundleContext.getServiceReference(ServiceComponentRuntime.class)

			def serviceComponentRuntime = bundleContext.getService(serviceReference)

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
				if (serviceReference != null) {
					bundleContext.ungetService(serviceReference)
				}
			}
		`;

		await serverAdministrationPage.executeScript(resetDataCleanupRegistratorScript);

		await applicationsMenuPage.goToServerAdministration();

		//Find Module Cleanup Actions Panel

		await executeCleanupActions(page, 'Module Cleanup Actions');
	}
	finally {
		const deleteReleasesScript = `
			import com.liferay.portal.kernel.service.ReleaseLocalServiceUtil
			import com.liferay.portal.kernel.model.Release

			def servletContextNames = ${JSON.stringify(SERVLET_CONTEXT_NAMES)}

			for (String servletContextName : servletContextNames) {
			Release release = ReleaseLocalServiceUtil.fetchRelease(servletContextName);

			if (release != null) {
			ReleaseLocalServiceUtil.deleteRelease(release);
			}}
		`;

		await serverAdministrationPage.executeScript(deleteReleasesScript);
		}
});



async function executeCleanupActions(page: Page, panelName: string) {
	//Find Cleanup Actions Panel

	const cleanupPanel = page.locator('.card, .panel',
	{ has: page.getByText(panelName) }).last();

    const panelHeader = cleanupPanel.getByRole('button', {
        name: new RegExp(panelName, 'i'),
    });

	//Expand Cleanup Actions Menu

	if (await panelHeader.getAttribute('aria-expanded') === 'false') {

	await panelHeader.click();

	await expect(panelHeader).toHaveAttribute('aria-expanded', 'true');
	}

	//Collect all execute buttons in Cleanup Actions Menu

	const executeButtons = cleanupPanel.getByRole('button', { name: 'Execute' });

	//Execute buttons sequentially and check for success message

    for (const button of await executeButtons.all()) {
        await button.click();

        const successMessage = page.locator('.alert-success', {
            hasText: 'Your request completed successfully.',
        });

        await expect(successMessage).toBeVisible();
    }
}

