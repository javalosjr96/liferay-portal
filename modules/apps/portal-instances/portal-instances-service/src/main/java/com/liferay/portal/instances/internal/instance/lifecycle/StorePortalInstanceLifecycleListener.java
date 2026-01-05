/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.instances.internal.instance.lifecycle;

import com.liferay.document.library.kernel.store.Store;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.persistence.ReloadablePersistenceManager;
import com.liferay.portal.db.partition.util.DBPartitionUtil;
import com.liferay.portal.instance.lifecycle.BasePortalInstanceLifecycleListener;
import com.liferay.portal.instance.lifecycle.PortalInstanceLifecycleListener;
import com.liferay.portal.kernel.io.unsync.UnsyncByteArrayInputStream;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.util.MapUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.util.PortalInstances;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.cm.file.ConfigurationHandler;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Jorge Avalos
 */
@Component(service = PortalInstanceLifecycleListener.class)
public class StorePortalInstanceLifeCycleListener
	extends BasePortalInstanceLifecycleListener {

	@Override
	public void portalInstanceUnregistered(Company company) throws Exception {
		_store.deleteCompany(company.getCompanyId());
	}

	@Reference(target = "(default=true)")
	private Store _store;


}