/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.tools.db.upgrade.client;

import java.io.File;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * @author Jorge Avalos
 */
public class BaseDBTypeScanner {

	public static String[] getDBTypes(File file) {
		Path portalDaoDBPath = Paths.get(
			file.getAbsolutePath(), _PORTAL_DAO_DB_BUNDLE_NAME + ".jar");

		try (JarFile jarFile = new JarFile(portalDaoDBPath.toFile())) {
			Manifest manifest = jarFile.getManifest();

			if (manifest != null) {
				Attributes attributes = manifest.getMainAttributes();

				if (Objects.equals(
						attributes.getValue("Bundle-SymbolicName"),
						_PORTAL_DAO_DB_BUNDLE_NAME)) {

					return _DXP_DATABASE_TYPES;
				}
			}
		}
		catch (Exception exception) {
			return _CE_DATABASE_TYPES;
		}

		return _CE_DATABASE_TYPES;
	}

	private static final String[] _CE_DATABASE_TYPES = {
		"hypersonic", "mariadb", "mysql", "postgresql"
	};

	private static final String[] _DXP_DATABASE_TYPES = {
		"db2", "mariadb", "mysql", "oracle", "postgresql", "sqlserver"
	};

	private static final String _PORTAL_DAO_DB_BUNDLE_NAME =
		"com.liferay.portal.dao.db";

}