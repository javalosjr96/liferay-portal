/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Locator, Page} from '@playwright/test';

import {VirtualInstancesPage} from '../../../../pages/portal-instances-web/VirtualInstancesPage';

export class VirtualInstanceExportPage {
	readonly errorAlert: Locator;
	readonly page: Page;
	readonly successToast: Locator;
	readonly virtualInstancesPage: VirtualInstancesPage;

	constructor(page: Page) {
		this.page = page;

		this.errorAlert = page.locator('.alert-danger');
		this.successToast = page.locator('.alert-success');
		this.virtualInstancesPage = new VirtualInstancesPage(page);
	}

	async addInstance(name: string) {
		await this.virtualInstancesPage.addNewVirtualInstance(name);
	}

	async deleteInstance(name: string) {
		await this.virtualInstancesPage.deleteVirtualInstance(name);
	}

	async export(name: string) {
		await this.virtualInstancesPage.exportVirtualInstance(name);
	}
}
