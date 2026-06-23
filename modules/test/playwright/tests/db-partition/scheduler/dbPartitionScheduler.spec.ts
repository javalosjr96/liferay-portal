/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, mergeTests} from '@playwright/test';

import {apiHelpersTest} from '../../../fixtures/apiHelpersTest';
import {loginTest} from '../../../fixtures/loginTest';
import {liferayConfig} from '../../../liferay.config';
import {getWebContentStructureId} from '../../../utils/structured-content/getBasicWebContentStructureId';

const test = mergeTests(apiHelpersTest, loginTest());

const ABLE_HOST = 'www.able.com';
const WC_STATUS_APPROVED = 0;

test.describe
	.serial('CanScheduleJobInVariousCompaniesWhenAutoUpgradeIsEnabled', () => {
	test(
		'Scheduled web content publishes in both instances after auto upgrade',
		{tag: '@LPD-91814'},
		async ({apiHelpers, page}) => {
			await apiHelpers.headlessPortalInstance.addVirtualInstance({
				domain: ABLE_HOST,
				portalInstanceId: ABLE_HOST,
				virtualHost: ABLE_HOST,
			});

			const [ableCompany, defaultCompany] = await Promise.all([
				apiHelpers.jsonWebServicesCompany.getCompanyByWebId(ABLE_HOST),
				apiHelpers.jsonWebServicesCompany.getCompanyByWebId(
					'liferay.com'
				),
			]);

			const [ableCompanyGroup, defaultCompanyGroup] = await Promise.all([
				apiHelpers.jsonWebServicesGroup.getCompanyGroup(
					ableCompany.companyId
				),
				apiHelpers.jsonWebServicesGroup.getCompanyGroup(
					defaultCompany.companyId
				),
			]);

			const [ableStructureId, defaultStructureId] = await Promise.all([
				getWebContentStructureId(
					apiHelpers,
					ableCompanyGroup.groupId,
					'BASIC-WEB-CONTENT'
				),
				getWebContentStructureId(
					apiHelpers,
					defaultCompanyGroup.groupId,
					'BASIC-WEB-CONTENT'
				),
			]);

			const scheduledDate = new Date(Date.now() + 2 * 60 * 1000);

			const displayDateDay = scheduledDate.getDate();
			const displayDateHour = scheduledDate.getHours();
			const displayDateMinute = scheduledDate.getMinutes();
			const displayDateMonth = scheduledDate.getMonth();
			const displayDateYear = scheduledDate.getFullYear();

			const ableArticle =
				await apiHelpers.jsonWebServicesJournal.addWebContentDetailed({
					ddmStructureId: ableStructureId,
					displayDateDay,
					displayDateHour,
					displayDateMinute,
					displayDateMonth,
					displayDateYear,
					groupId: ableCompanyGroup.groupId,
					titleMap: {en_US: 'Web Content Title'},
				});

			const defaultArticle =
				await apiHelpers.jsonWebServicesJournal.addWebContentDetailed({
					ddmStructureId: defaultStructureId,
					displayDateDay,
					displayDateHour,
					displayDateMinute,
					displayDateMonth,
					displayDateYear,
					groupId: defaultCompanyGroup.groupId,
					titleMap: {en_US: 'Web Content Title'},
				});

			const journalArticlePath = `${liferayConfig.environment.baseUrl}/api/jsonws/journal.journalarticle/get-article`;

			const journalHeaders = await apiHelpers.getJSONWebServicesHeaders();

			const ableArticleUrlSearchParams = new URLSearchParams();

			ableArticleUrlSearchParams.append(
				'articleId',
				ableArticle.articleId
			);
			ableArticleUrlSearchParams.append(
				'groupId',
				String(ableCompanyGroup.groupId)
			);

			const defaultArticleUrlSearchParams = new URLSearchParams();

			defaultArticleUrlSearchParams.append(
				'articleId',
				defaultArticle.articleId
			);
			defaultArticleUrlSearchParams.append(
				'groupId',
				String(defaultCompanyGroup.groupId)
			);

			try {
				await expect(async () => {
					const [defaultResponse, ableResponse] = await Promise.all([
						page.request.post(journalArticlePath, {
							data: defaultArticleUrlSearchParams.toString(),
							headers: journalHeaders,
						}),
						page.request.post(journalArticlePath, {
							data: ableArticleUrlSearchParams.toString(),
							headers: journalHeaders,
						}),
					]);

					const [defaultArticleData, ableArticleData] =
						await Promise.all([
							defaultResponse.json(),
							ableResponse.json(),
						]);

					expect(defaultArticleData.status).toBe(WC_STATUS_APPROVED);
					expect(ableArticleData.status).toBe(WC_STATUS_APPROVED);
				}).toPass({timeout: 240_000});
			}
			finally {
				await apiHelpers.jsonWebServicesJournal.deleteArticle(
					String(defaultCompanyGroup.groupId),
					defaultArticle.articleId
				);

				await apiHelpers.headlessPortalInstance.deleteVirtualInstance(
					Number(ableCompany.companyId)
				);
			}
		}
	);
});

test.describe
	.serial('ScheduleWebContentChangesWithDBPartitioningActivatedAcrossVariousCompanies', () => {
	test(
		'Scheduled web content publishes in default and virtual instance',
		{tag: '@LPD-91814'},
		async ({apiHelpers, page}) => {
			await apiHelpers.headlessPortalInstance.addVirtualInstance({
				domain: ABLE_HOST,
				portalInstanceId: ABLE_HOST,
				virtualHost: ABLE_HOST,
			});

			const [ableCompany, defaultCompany] = await Promise.all([
				apiHelpers.jsonWebServicesCompany.getCompanyByWebId(ABLE_HOST),
				apiHelpers.jsonWebServicesCompany.getCompanyByWebId(
					'liferay.com'
				),
			]);

			const [ableCompanyGroup, defaultCompanyGroup] = await Promise.all([
				apiHelpers.jsonWebServicesGroup.getCompanyGroup(
					ableCompany.companyId
				),
				apiHelpers.jsonWebServicesGroup.getCompanyGroup(
					defaultCompany.companyId
				),
			]);

			const [ableStructureId, defaultStructureId] = await Promise.all([
				getWebContentStructureId(
					apiHelpers,
					ableCompanyGroup.groupId,
					'BASIC-WEB-CONTENT'
				),
				getWebContentStructureId(
					apiHelpers,
					defaultCompanyGroup.groupId,
					'BASIC-WEB-CONTENT'
				),
			]);

			const ableScheduledDate = new Date(Date.now() + 4 * 60 * 1000);

			const ableArticle =
				await apiHelpers.jsonWebServicesJournal.addWebContentDetailed({
					ddmStructureId: ableStructureId,
					displayDateDay: ableScheduledDate.getDate(),
					displayDateHour: ableScheduledDate.getHours(),
					displayDateMinute: ableScheduledDate.getMinutes(),
					displayDateMonth: ableScheduledDate.getMonth(),
					displayDateYear: ableScheduledDate.getFullYear(),
					groupId: ableCompanyGroup.groupId,
					titleMap: {en_US: 'WC WebContent Title New Company'},
				});

			const defaultScheduledDate = new Date(Date.now() + 2 * 60 * 1000);

			const defaultArticle =
				await apiHelpers.jsonWebServicesJournal.addWebContentDetailed({
					ddmStructureId: defaultStructureId,
					displayDateDay: defaultScheduledDate.getDate(),
					displayDateHour: defaultScheduledDate.getHours(),
					displayDateMinute: defaultScheduledDate.getMinutes(),
					displayDateMonth: defaultScheduledDate.getMonth(),
					displayDateYear: defaultScheduledDate.getFullYear(),
					groupId: defaultCompanyGroup.groupId,
					titleMap: {
						en_US: 'WC WebContent Title Default Company',
					},
				});

			const journalArticlePath = `${liferayConfig.environment.baseUrl}/api/jsonws/journal.journalarticle/get-article`;

			const journalHeaders = await apiHelpers.getJSONWebServicesHeaders();

			const ableArticleUrlSearchParams = new URLSearchParams();

			ableArticleUrlSearchParams.append(
				'articleId',
				ableArticle.articleId
			);
			ableArticleUrlSearchParams.append(
				'groupId',
				String(ableCompanyGroup.groupId)
			);

			const defaultArticleUrlSearchParams = new URLSearchParams();

			defaultArticleUrlSearchParams.append(
				'articleId',
				defaultArticle.articleId
			);
			defaultArticleUrlSearchParams.append(
				'groupId',
				String(defaultCompanyGroup.groupId)
			);

			try {
				await expect(async () => {
					const [defaultResponse, ableResponse] = await Promise.all([
						page.request.post(journalArticlePath, {
							data: defaultArticleUrlSearchParams.toString(),
							headers: journalHeaders,
						}),
						page.request.post(journalArticlePath, {
							data: ableArticleUrlSearchParams.toString(),
							headers: journalHeaders,
						}),
					]);

					const [defaultArticleData, ableArticleData] =
						await Promise.all([
							defaultResponse.json(),
							ableResponse.json(),
						]);

					expect(defaultArticleData.status).toBe(WC_STATUS_APPROVED);
					expect(ableArticleData.status).toBe(WC_STATUS_APPROVED);
				}).toPass({timeout: 360_000});
			}
			finally {
				await apiHelpers.jsonWebServicesJournal.deleteArticle(
					String(defaultCompanyGroup.groupId),
					defaultArticle.articleId
				);

				await apiHelpers.headlessPortalInstance.deleteVirtualInstance(
					Number(ableCompany.companyId)
				);
			}
		}
	);
});
