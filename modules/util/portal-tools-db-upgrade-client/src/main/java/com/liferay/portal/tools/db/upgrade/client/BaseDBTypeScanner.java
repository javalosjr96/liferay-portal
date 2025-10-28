/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.tools.db.upgrade.client;

import java.io.File;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.jar.*;

/**
 * @author Jorge Avalos
 */
public class BaseDBTypeScanner {

	public static List<String> getDBTypes(File file) throws Exception {
		List<URL> urls = new ArrayList<>();

		try {
			for (File jarFile :
					Objects.requireNonNull(
						file.listFiles(
							(dir, name) -> {
								String lowercaseName = name.toLowerCase();

								if (!lowercaseName.endsWith(".jar")) {
									return false;
								}

								return lowercaseName.contains("portal") ||
									   lowercaseName.contains("com.liferay") ||
									   lowercaseName.contains("log");
							}))) {

				URI uri = jarFile.toURI();

				urls.add(uri.toURL());
			}
		}
		catch (MalformedURLException malformedURLException) {
			throw new MalformedURLException(
				"Unable to convert shielded container lib jars to URLs");
		}

		List<String> dbTypes = new ArrayList<>();

		try (URLClassLoader loader = new URLClassLoader(
				urls.toArray(new URL[0]), ClassLoader.getSystemClassLoader())) {

			Class<?> baseDB = loader.loadClass(_BASE_DB_CLASS);

			for (URL url : urls) {
				try (JarFile jarFile = new JarFile(url.getFile())) {
					jarFile.stream(
					).forEach(
						entry -> _invokeGetDBType(
							entry.getName(), loader, baseDB, dbTypes)
					);
				}
			}
		}

		return dbTypes;
	}

	private static void _invokeGetDBType(
		String entry, ClassLoader cl, Class<?> baseDB, List<String> dbTypes) {

		if (!entry.contains("dao/db")) {
			return;
		}

		String className = entry.replace(
			'/', '.'
		).replace(
			".class", ""
		);

		try {
			Class<?> clazz = cl.loadClass(className);

			if (!baseDB.isAssignableFrom(clazz) || (clazz == baseDB) ||
				Modifier.isAbstract(clazz.getModifiers())) {

				return;
			}

			Constructor<?> constructor = clazz.getDeclaredConstructor(
				int.class, int.class);

			constructor.setAccessible(true);

			Object instance = constructor.newInstance(_MAJOR, _MINOR);

			Method method = clazz.getMethod("getDBType");

			if (!method.canAccess(instance)) {
				method.setAccessible(true);
			}

			Object dbType = method.invoke(instance);

			dbTypes.add(dbType.toString());
		}
		catch (Throwable ignored) {
		}
	}

	private static final String _BASE_DB_CLASS =
		"com.liferay.portal.dao.db.BaseDB";

	private static final int _MAJOR = 1, _MINOR = 0;

}