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

			const ableCompany =
				await apiHelpers.jsonWebServicesCompany.getCompanyByWebId(
					ABLE_HOST
				);

			const ableCompanyGroup =
				await apiHelpers.jsonWebServicesGroup.getCompanyGroup(
					ableCompany.companyId
				);

			const defaultCompany =
				await apiHelpers.jsonWebServicesCompany.getCompanyByWebId(
					'liferay.com'
				);

			const defaultCompanyGroup =
				await apiHelpers.jsonWebServicesGroup.getCompanyGroup(
					defaultCompany.companyId
				);

			const ableStructureId = await getWebContentStructureId(
				apiHelpers,
				ableCompanyGroup.groupId,
				'BASIC-WEB-CONTENT'
			);

			const defaultStructureId = await getWebContentStructureId(
				apiHelpers,
				defaultCompanyGroup.groupId,
				'BASIC-WEB-CONTENT'
			);

			const scheduledDate = new Date(Date.now() + 2 * 60 * 1000);

			const displayDateDay = scheduledDate.getUTCDate();
			const displayDateHour = scheduledDate.getUTCHours();
			const displayDateMinute = scheduledDate.getUTCMinutes();
			const displayDateMonth = scheduledDate.getUTCMonth();
			const displayDateYear = scheduledDate.getUTCFullYear();

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
					const defaultResponse = await page.request.post(
						journalArticlePath,
						{
							data: defaultArticleUrlSearchParams.toString(),
							headers: journalHeaders,
						}
					);

					const defaultArticleData = await defaultResponse.json();

					expect(defaultArticleData.status).toBe(WC_STATUS_APPROVED);

					const ableResponse = await page.request.post(
						journalArticlePath,
						{
							data: ableArticleUrlSearchParams.toString(),
							headers: journalHeaders,
						}
					);

					const ableArticleData = await ableResponse.json();

					expect(ableArticleData.status).toBe(WC_STATUS_APPROVED);
				}).toPass({timeout: 240_000});
			}
			finally {
				await apiHelpers.jsonWebServicesJournal.deleteArticle(
					String(defaultCompanyGroup.groupId),
					defaultArticle.articleId
				);

				await apiHelpers.headlessPortalInstance.deleteVirtualInstance(
					ableCompany.companyId
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

			const ableCompany =
				await apiHelpers.jsonWebServicesCompany.getCompanyByWebId(
					ABLE_HOST
				);

			const ableCompanyGroup =
				await apiHelpers.jsonWebServicesGroup.getCompanyGroup(
					ableCompany.companyId
				);

			const defaultCompany =
				await apiHelpers.jsonWebServicesCompany.getCompanyByWebId(
					'liferay.com'
				);

			const defaultCompanyGroup =
				await apiHelpers.jsonWebServicesGroup.getCompanyGroup(
					defaultCompany.companyId
				);

			const ableStructureId = await getWebContentStructureId(
				apiHelpers,
				ableCompanyGroup.groupId,
				'BASIC-WEB-CONTENT'
			);

			const defaultStructureId = await getWebContentStructureId(
				apiHelpers,
				defaultCompanyGroup.groupId,
				'BASIC-WEB-CONTENT'
			);

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
					const defaultResponse = await page.request.post(
						journalArticlePath,
						{
							data: defaultArticleUrlSearchParams.toString(),
							headers: journalHeaders,
						}
					);

					const defaultArticleData = await defaultResponse.json();

					expect(defaultArticleData.status).toBe(WC_STATUS_APPROVED);

					const ableResponse = await page.request.post(
						journalArticlePath,
						{
							data: ableArticleUrlSearchParams.toString(),
							headers: journalHeaders,
						}
					);

					const ableArticleData = await ableResponse.json();

					expect(ableArticleData.status).toBe(WC_STATUS_APPROVED);
				}).toPass({timeout: 360_000});
			}
			finally {
				await apiHelpers.jsonWebServicesJournal.deleteArticle(
					String(defaultCompanyGroup.groupId),
					defaultArticle.articleId
				);

				await apiHelpers.headlessPortalInstance.deleteVirtualInstance(
					ableCompany.companyId
				);
			}
		}
	);
});
