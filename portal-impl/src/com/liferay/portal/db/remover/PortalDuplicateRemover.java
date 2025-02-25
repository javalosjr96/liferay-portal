package com.liferay.portal.db.remover;

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
