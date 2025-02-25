package com.liferay.portal.db.remover;

public interface DuplicateRemover {
	String getTableName();

	String getColumnName();

	void removeDuplicates();
}
