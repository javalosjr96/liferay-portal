/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {openModal, openToast} from 'frontend-js-components-web';
import {fetch} from 'frontend-js-web';

import openDeleteCompanyModal from './openDeleteCompanyModal';
import openExportCompanyModal from './openExportCompanyModal';

const ACTIONS = {
	copyInstance(itemData, portletNamespace) {
		openModal({
			buttons: [
				{
					displayType: 'secondary',
					label: Liferay.Language.get('cancel'),
					type: 'cancel',
				},
				{
					formId: `${portletNamespace}fm`,
					label: Liferay.Language.get('copy'),
					type: 'submit',
				},
			],
			height: '60vh',
			iframeBodyCssClass: '',
			size: 'md',
			title: Liferay.Language.get('copy-instance'),
			url: itemData.copyInstanceURL,
		});
	},

	deleteInstance(itemData) {
		openDeleteCompanyModal({
			onDelete: () => {
				fetch(itemData.deleteURL, {method: 'POST'}).then(() => {
					window.location.reload();
				});
			},
		});
	},

	exportInstance(itemData) {
		openExportCompanyModal({
			onExport: () => {
				fetch(itemData.exportURL, {method: 'POST'})
					.then((response) => response.json())
					.then((responseJSON) => {
						if (responseJSON.successMessage) {
							openToast({
								message: responseJSON.successMessage,
								type: 'success',
							});
						}
						else {
							openToast({
								message: responseJSON.error,
								type: 'danger',
							});
						}
					})
					.catch(() => {
						openToast({
							message: Liferay.Language.get(
								'an-unexpected-error-occurred'
							),
							type: 'danger',
						});
					});
			},
		});
	},
};

export default function propsTransformer({items, portletNamespace, ...props}) {
	return {
		...props,
		items: items.map((item) => {
			return {
				...item,
				items: item.items.map((child) => ({
					...child,
					onClick(event) {
						const action = child.data?.action;

						if (action) {
							event.preventDefault();

							ACTIONS[action](child.data, portletNamespace);
						}
					},
				})),
			};
		}),
	};
}
