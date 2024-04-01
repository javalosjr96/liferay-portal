/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.search.internal.spi.model.query.contributor;

import com.liferay.portal.kernel.search.BooleanClauseOccur;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.filter.BooleanFilter;
import com.liferay.portal.kernel.search.filter.TermsFilter;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.search.spi.model.query.contributor.QueryPreFilterContributor;

import org.osgi.service.component.annotations.Component;

/**
 * @author Jürgen Kappler
 */
@Component(service = QueryPreFilterContributor.class)
public class AssetVocabularyIdsQueryPreFilterContributor
	implements QueryPreFilterContributor {

	@Override
	public void contribute(
		BooleanFilter fullQueryBooleanFilter, SearchContext searchContext) {

		long[] assetVocabularyIds = searchContext.getAssetVocabularyIds();

		if (ArrayUtil.isEmpty(assetVocabularyIds)) {
			return;
		}

		TermsFilter assetVocabularyIdsTermsFilter = new TermsFilter(
			Field.ASSET_VOCABULARY_IDS);

		assetVocabularyIdsTermsFilter.addValues(
			ArrayUtil.toStringArray(assetVocabularyIds));

		if (!searchContext.isIncludeInternalAssetCategories()) {
			fullQueryBooleanFilter.add(
				assetVocabularyIdsTermsFilter, BooleanClauseOccur.MUST);

			return;
		}

		BooleanFilter booleanFilter = new BooleanFilter();

		TermsFilter internalAssetVocabularyIdsTermsFilter = new TermsFilter(
			Field.ASSET_INTERNAL_VOCABULARY_IDS);

		internalAssetVocabularyIdsTermsFilter.addValues(
			ArrayUtil.toStringArray(assetVocabularyIds));

		booleanFilter.add(assetVocabularyIdsTermsFilter);
		booleanFilter.add(internalAssetVocabularyIdsTermsFilter);

		fullQueryBooleanFilter.add(booleanFilter, BooleanClauseOccur.MUST);
	}

}