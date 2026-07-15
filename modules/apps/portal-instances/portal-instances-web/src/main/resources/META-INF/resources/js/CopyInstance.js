/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {openToast} from 'frontend-js-components-web';
import {fetch, getOpener} from 'frontend-js-web';

export default function ({namespace}) {
	const form = document.getElementById(`${namespace}fm`);

	const content = document.querySelector('.copy-instance-content');
	const loading = document.querySelector('.copy-instance-loading');

	const showError = (message, alertContainer) => {
		content.classList.add('d-block');
		loading.classList.add('d-none');
		loading.classList.remove('d-flex');

		openToast({
			autoClose: false,
			container: alertContainer,
			message,
			toastProps: {
				onClose: null,
			},
			type: 'danger',
			variant: 'stripe',
		});
	};

	const onSubmit = (event) => {
		event.preventDefault();

		const formData = new FormData(form);

		content.classList.add('d-none');
		content.classList.remove('d-block');
		loading.classList.add('d-flex');

		const alertContainer = document.querySelector(
			'.copy-instance-alert-container'
		);

		alertContainer.innerHTML = '';

		fetch(form.action, {
			body: formData,
			method: 'POST',
		})
			.then((response) => {
				if (!response.ok) {
					throw new Error();
				}

				return response.json();
			})
			.then((response) => {
				const opener = getOpener();

				if (!response.error) {
					opener.Liferay.fire('closeModal', {
						redirect: opener.location.href,
					});
				}
				else {
					showError(response.error, alertContainer);
				}
			})
			.catch(() => {
				showError(
					Liferay.Language.get('an-unexpected-error-occurred'),
					alertContainer
				);
			});
	};

	form.addEventListener('submit', onSubmit);

	return {
		dispose() {
			form.removeEventListener('submit', onSubmit);
		},
	};
}
