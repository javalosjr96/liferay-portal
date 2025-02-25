package com.liferay.portal.db.remover;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;


@Component(
	property = "service.tables=portal",
	service = DuplicateRemover.class
)
public class PortalDuplicateRemover implements DuplicateRemover {

	@Override
	public String getTableName() {
		return "getTableName";
	}

	@Override
	public String getColumnName() {
		return "getColumnName";
	}

	@Override
	public void removeDuplicates() {
		System.out.println("PortalDuplicateRemover.removeDuplicates");

	}
}
