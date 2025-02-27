package com.liferay.layout.internal.upgrade.remover;

import com.liferay.portal.db.remover.DuplicateRemover;
import com.liferay.portal.db.remover.PortalDuplicateRemover;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.service.LayoutLocalService;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;


@Component(
	property = "service.tables=test",
	service = DuplicateRemover.class)

public class LayoutDuplicateRemover extends PortalDuplicateRemover {

	@Override
	public String getTableName() {
		Layout layout = _layoutLocalService.createLayout(12357634);

		System.out.println(layout.getPlid());

		return "LayoutDuplicateRemover.getTableName";
	}

	@Override
	public String getColumnName() {
		return "LayoutDuplicateRemover.getColumnName";
	}

	@Override
	public void removeDuplicates() {
		System.out.println("LayoutDuplicateRemover.removeDuplicates");
	}

@Reference
private LayoutLocalService _layoutLocalService;
}
