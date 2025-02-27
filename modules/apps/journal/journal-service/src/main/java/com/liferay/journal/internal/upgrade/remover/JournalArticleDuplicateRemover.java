package com.liferay.journal.internal.upgrade.remover;

import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.service.JournalArticleLocalService;
import com.liferay.portal.db.remover.DuplicateRemover;
import com.liferay.portal.db.remover.PortalDuplicateRemover;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;


@Component(
	property = "service.tables=test",
	service = DuplicateRemover.class)

public class JournalArticleDuplicateRemover extends PortalDuplicateRemover {

	@Override
	public String getTableName() {
		JournalArticle layout = _journalArticleLocalServiceLocalService.createJournalArticle(12357634);

		System.out.println(layout.getArticleId());

		return "JournalArticleDuplicateRemover.getTableName";
	}

	@Override
	public String getColumnName() {
		return "JournalArticleDuplicateRemover.getColumnName";
	}

	@Override
	public void removeDuplicates() {
		System.out.println("JournalArticleDuplicateRemover.removeDuplicates");
	}

	@Reference
	private JournalArticleLocalService _journalArticleLocalServiceLocalService;
}
