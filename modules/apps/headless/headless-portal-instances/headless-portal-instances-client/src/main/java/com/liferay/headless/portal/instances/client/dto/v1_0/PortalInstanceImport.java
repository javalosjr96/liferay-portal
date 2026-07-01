/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.headless.portal.instances.client.dto.v1_0;

import com.liferay.headless.portal.instances.client.function.UnsafeSupplier;
import com.liferay.headless.portal.instances.client.serdes.v1_0.PortalInstanceImportSerDes;

import jakarta.annotation.Generated;

import java.io.Serializable;

import java.util.Objects;

/**
 * @author Alberto Chaparro
 * @generated
 */
@Generated("")
public class PortalInstanceImport implements Cloneable, Serializable {

	public static PortalInstanceImport toDTO(String json) {
		return PortalInstanceImportSerDes.toDTO(json);
	}

	public String getNewName() {
		return newName;
	}

	public void setNewName(String newName) {
		this.newName = newName;
	}

	public void setNewName(
		UnsafeSupplier<String, Exception> newNameUnsafeSupplier) {

		try {
			newName = newNameUnsafeSupplier.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected String newName;

	public String getNewVirtualHostname() {
		return newVirtualHostname;
	}

	public void setNewVirtualHostname(String newVirtualHostname) {
		this.newVirtualHostname = newVirtualHostname;
	}

	public void setNewVirtualHostname(
		UnsafeSupplier<String, Exception> newVirtualHostnameUnsafeSupplier) {

		try {
			newVirtualHostname = newVirtualHostnameUnsafeSupplier.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected String newVirtualHostname;

	public String getNewWebId() {
		return newWebId;
	}

	public void setNewWebId(String newWebId) {
		this.newWebId = newWebId;
	}

	public void setNewWebId(
		UnsafeSupplier<String, Exception> newWebIdUnsafeSupplier) {

		try {
			newWebId = newWebIdUnsafeSupplier.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected String newWebId;

	public String getSchemaName() {
		return schemaName;
	}

	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

	public void setSchemaName(
		UnsafeSupplier<String, Exception> schemaNameUnsafeSupplier) {

		try {
			schemaName = schemaNameUnsafeSupplier.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected String schemaName;

	@Override
	public PortalInstanceImport clone() throws CloneNotSupportedException {
		return (PortalInstanceImport)super.clone();
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!(object instanceof PortalInstanceImport)) {
			return false;
		}

		PortalInstanceImport portalInstanceImport =
			(PortalInstanceImport)object;

		return Objects.equals(toString(), portalInstanceImport.toString());
	}

	@Override
	public int hashCode() {
		String string = toString();

		return string.hashCode();
	}

	public String toString() {
		return PortalInstanceImportSerDes.toJSON(this);
	}

}
// LIFERAY-REST-BUILDER-HASH:-100048910