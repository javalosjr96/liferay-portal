package com.liferay.oauth.client.persistence.internal.remover;

import com.liferay.oauth.client.persistence.service.OAuthClientEntryService;
import com.liferay.portal.db.remover.DuplicateRemover;
import com.liferay.portal.db.remover.PortalDuplicateRemover;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;


@Component(
	property = "service.tables=test",
	service = DuplicateRemover.class)

public class OAuthClientEntryDuplicateRemover extends PortalDuplicateRemover {

	@Override
	public String getTableName() {
		String layout = _oAuthClientEntryService.getOSGiServiceIdentifier();

		System.out.println(layout);

		return "OAuthClientEntry.getTableName";
	}

	@Override
	public String getColumnName() {
		return "OAuthClientEntry.getColumnName";
	}

	@Override
	public void removeDuplicates() {
		System.out.println("OAuthClientEntry.removeDuplicates");
	}

	@Reference
	private OAuthClientEntryService _oAuthClientEntryService;
}
