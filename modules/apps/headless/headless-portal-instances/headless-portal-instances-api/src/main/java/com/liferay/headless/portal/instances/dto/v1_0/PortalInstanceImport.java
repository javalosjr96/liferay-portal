/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.headless.portal.instances.dto.v1_0;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.liferay.petra.function.UnsafeSupplier;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.vulcan.graphql.annotation.GraphQLField;
import com.liferay.portal.vulcan.graphql.annotation.GraphQLName;
import com.liferay.portal.vulcan.util.ObjectMapperUtil;

import jakarta.annotation.Generated;

import jakarta.validation.constraints.NotEmpty;

import jakarta.xml.bind.annotation.XmlRootElement;

import java.io.Serializable;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author Alberto Chaparro
 * @generated
 */
@Generated("")
@GraphQLName(
	description = "Import configuration for a DB partition portal instance.",
	value = "PortalInstanceImport"
)
@io.swagger.v3.oas.annotations.media.Schema(
	description = "Import configuration for a DB partition portal instance.",
	requiredProperties = {"schemaName"}
)
@JsonFilter("Liferay.Vulcan")
@XmlRootElement(name = "PortalInstanceImport")
public class PortalInstanceImport implements Serializable {

	public static PortalInstanceImport toDTO(String json) {
		return ObjectMapperUtil.readValue(PortalInstanceImport.class, json);
	}

	public static PortalInstanceImport unsafeToDTO(String json) {
		return ObjectMapperUtil.unsafeReadValue(
			PortalInstanceImport.class, json);
	}

	@io.swagger.v3.oas.annotations.media.Schema(
		description = "Optional new display name for the imported portal instance."
	)
	public String getNewName() {
		if (_newNameSupplier != null) {
			newName = _newNameSupplier.get();

			_newNameSupplier = null;
		}

		return newName;
	}

	public void setNewName(String newName) {
		this.newName = newName;

		_newNameSupplier = null;
	}

	@JsonIgnore
	public void setNewName(
		UnsafeSupplier<String, Exception> newNameUnsafeSupplier) {

		_newNameSupplier = () -> {
			try {
				return newNameUnsafeSupplier.get();
			}
			catch (RuntimeException runtimeException) {
				throw runtimeException;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	@GraphQLField(
		description = "Optional new display name for the imported portal instance."
	)
	@JsonProperty(access = JsonProperty.Access.READ_WRITE)
	protected String newName;

	@JsonIgnore
	private Supplier<String> _newNameSupplier;

	@io.swagger.v3.oas.annotations.media.Schema(
		description = "Optional new virtual hostname for the imported portal instance."
	)
	public String getNewVirtualHostname() {
		if (_newVirtualHostnameSupplier != null) {
			newVirtualHostname = _newVirtualHostnameSupplier.get();

			_newVirtualHostnameSupplier = null;
		}

		return newVirtualHostname;
	}

	public void setNewVirtualHostname(String newVirtualHostname) {
		this.newVirtualHostname = newVirtualHostname;

		_newVirtualHostnameSupplier = null;
	}

	@JsonIgnore
	public void setNewVirtualHostname(
		UnsafeSupplier<String, Exception> newVirtualHostnameUnsafeSupplier) {

		_newVirtualHostnameSupplier = () -> {
			try {
				return newVirtualHostnameUnsafeSupplier.get();
			}
			catch (RuntimeException runtimeException) {
				throw runtimeException;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	@GraphQLField(
		description = "Optional new virtual hostname for the imported portal instance."
	)
	@JsonProperty(access = JsonProperty.Access.READ_WRITE)
	protected String newVirtualHostname;

	@JsonIgnore
	private Supplier<String> _newVirtualHostnameSupplier;

	@io.swagger.v3.oas.annotations.media.Schema(
		description = "Optional new web ID for the imported portal instance."
	)
	public String getNewWebId() {
		if (_newWebIdSupplier != null) {
			newWebId = _newWebIdSupplier.get();

			_newWebIdSupplier = null;
		}

		return newWebId;
	}

	public void setNewWebId(String newWebId) {
		this.newWebId = newWebId;

		_newWebIdSupplier = null;
	}

	@JsonIgnore
	public void setNewWebId(
		UnsafeSupplier<String, Exception> newWebIdUnsafeSupplier) {

		_newWebIdSupplier = () -> {
			try {
				return newWebIdUnsafeSupplier.get();
			}
			catch (RuntimeException runtimeException) {
				throw runtimeException;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	@GraphQLField(
		description = "Optional new web ID for the imported portal instance."
	)
	@JsonProperty(access = JsonProperty.Access.READ_WRITE)
	protected String newWebId;

	@JsonIgnore
	private Supplier<String> _newWebIdSupplier;

	@io.swagger.v3.oas.annotations.media.Schema(
		description = "The DB partition schema name to import (format lextracted_[companyId])."
	)
	public String getSchemaName() {
		if (_schemaNameSupplier != null) {
			schemaName = _schemaNameSupplier.get();

			_schemaNameSupplier = null;
		}

		return schemaName;
	}

	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;

		_schemaNameSupplier = null;
	}

	@JsonIgnore
	public void setSchemaName(
		UnsafeSupplier<String, Exception> schemaNameUnsafeSupplier) {

		_schemaNameSupplier = () -> {
			try {
				return schemaNameUnsafeSupplier.get();
			}
			catch (RuntimeException runtimeException) {
				throw runtimeException;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	@GraphQLField(
		description = "The DB partition schema name to import (format lextracted_[companyId])."
	)
	@JsonProperty(access = JsonProperty.Access.READ_WRITE)
	@NotEmpty
	protected String schemaName;

	@JsonIgnore
	private Supplier<String> _schemaNameSupplier;

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
		StringBundler sb = new StringBundler();

		sb.append("{");

		String newName = getNewName();

		if (newName != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"newName\": ");

			sb.append("\"");

			sb.append(_escape(newName));

			sb.append("\"");
		}

		String newVirtualHostname = getNewVirtualHostname();

		if (newVirtualHostname != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"newVirtualHostname\": ");

			sb.append("\"");

			sb.append(_escape(newVirtualHostname));

			sb.append("\"");
		}

		String newWebId = getNewWebId();

		if (newWebId != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"newWebId\": ");

			sb.append("\"");

			sb.append(_escape(newWebId));

			sb.append("\"");
		}

		String schemaName = getSchemaName();

		if (schemaName != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"schemaName\": ");

			sb.append("\"");

			sb.append(_escape(schemaName));

			sb.append("\"");
		}

		sb.append("}");

		return sb.toString();
	}

	@io.swagger.v3.oas.annotations.media.Schema(
		accessMode = io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY,
		defaultValue = "com.liferay.headless.portal.instances.dto.v1_0.PortalInstanceImport",
		name = "x-class-name"
	)
	public String xClassName;

	private static String _escape(Object object) {
		return StringUtil.replace(
			String.valueOf(object), _JSON_ESCAPE_STRINGS[0],
			_JSON_ESCAPE_STRINGS[1]);
	}

	private static boolean _isArray(Object value) {
		if (value == null) {
			return false;
		}

		Class<?> clazz = value.getClass();

		return clazz.isArray();
	}

	private static String _toJSON(Map<String, ?> map) {
		StringBuilder sb = new StringBuilder("{");

		@SuppressWarnings("unchecked")
		Set set = map.entrySet();

		@SuppressWarnings("unchecked")
		Iterator<Map.Entry<String, ?>> iterator = set.iterator();

		while (iterator.hasNext()) {
			Map.Entry<String, ?> entry = iterator.next();

			sb.append("\"");
			sb.append(_escape(entry.getKey()));
			sb.append("\": ");

			Object value = entry.getValue();

			if (_isArray(value)) {
				sb.append("[");

				Object[] valueArray = (Object[])value;

				for (int i = 0; i < valueArray.length; i++) {
					if (valueArray[i] instanceof Map) {
						sb.append(_toJSON((Map<String, ?>)valueArray[i]));
					}
					else if (valueArray[i] instanceof String) {
						sb.append("\"");
						sb.append(valueArray[i]);
						sb.append("\"");
					}
					else {
						sb.append(valueArray[i]);
					}

					if ((i + 1) < valueArray.length) {
						sb.append(", ");
					}
				}

				sb.append("]");
			}
			else if (value instanceof Map) {
				sb.append(_toJSON((Map<String, ?>)value));
			}
			else if (value instanceof String) {
				sb.append("\"");
				sb.append(_escape(value));
				sb.append("\"");
			}
			else {
				sb.append(value);
			}

			if (iterator.hasNext()) {
				sb.append(", ");
			}
		}

		sb.append("}");

		return sb.toString();
	}

	private static final String[][] _JSON_ESCAPE_STRINGS = {
		{"\\", "\"", "\b", "\f", "\n", "\r", "\t"},
		{"\\\\", "\\\"", "\\b", "\\f", "\\n", "\\r", "\\t"}
	};

	private Map<String, Serializable> _extendedProperties;

}
// LIFERAY-REST-BUILDER-HASH:-562395610