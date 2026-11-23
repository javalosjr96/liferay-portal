/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {APIRequestContext, request} from '@playwright/test';

import {liferayConfig} from '../../../liferay.config';

const NAMESPACE = '_com_liferay_login_web_portlet_LoginPortlet_';

function getMatch(html: string, regExp: RegExp, name: string) {
	const match = html.match(regExp);

	if (!match) {
		throw new Error(`Unable to find the ${name} in the sign in page`);
	}

	return match[1].replace(/&amp;/g, '&');
}

export async function createPortalSession(): Promise<APIRequestContext> {
	const apiRequestContext = await request.newContext({
		baseURL: liferayConfig.environment.baseUrl,
	});

	const signInPageResponse = await apiRequestContext.get('/c/portal/login');

	const html = await signInPageResponse.text();

	const loginFormHTML = getMatch(
		html,
		new RegExp(`(<form[^>]*id="${NAMESPACE}loginForm"[^>]*>)`),
		'login form'
	);

	const loginResponse = await apiRequestContext.post(
		getMatch(loginFormHTML, /action="([^"]+)"/, 'login form action'),
		{
			form: {
				[`${NAMESPACE}formDate`]: getMatch(
					html,
					new RegExp(
						`name="${NAMESPACE}formDate"[^>]*value="(\\d+)"`
					),
					'login form date'
				),
				[`${NAMESPACE}login`]: 'test@liferay.com',
				[`${NAMESPACE}password`]: liferayConfig.environment.password,
			},
			maxRedirects: 0,
		}
	);

	if (loginResponse.status() !== 302) {
		throw new Error(
			`Unable to sign in through the sign in page: HTTP ${loginResponse.status()}`
		);
	}

	return apiRequestContext;
}
