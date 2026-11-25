/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.tools.upgrade.table.builder;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.test.util.RandomTestUtil;

import java.io.File;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Jorge Avalos
 */
public class UpgradeTableBuilderTest {

	@Test
	public void testBuildUpgradeTable() throws Exception {
		File baseDir = temporaryFolder.newFolder("base");

		Path baseDirPath = baseDir.toPath();

		String copyright = RandomTestUtil.randomString();

		_write(baseDirPath, "copyright.txt", copyright);

		_write(
			baseDirPath, "service.xml",
			StringBundler.concat(
				"<?xml version=\"1.0\"?>\n<!DOCTYPE service-builder PUBLIC ",
				"\"-//Liferay//DTD Service Builder 7.4.0//EN\" ",
				"\"http://www.liferay.com/dtd/liferay-",
				"service-builder_7_4_0.dtd\">\n\n",
				"<service-builder />"));

		String author = RandomTestUtil.randomString();
		String className = "Test" + RandomTestUtil.randomString() + "Table";

		Path tablePath = _write(
			baseDirPath, "upgrade/v1_0_0/util/" + className + ".java",
			StringBundler.concat(
				"package com.liferay.test.upgrade.v1_0_0.util;\n\n/**\n * ",
				"@author ", author, "\n */\npublic class ", className,
				" {\n}"));

		File tablesDir = temporaryFolder.newFolder("tables");

		String tableName = RandomTestUtil.randomString();

		String sql = "create table " + tableName + " (id LONG not null)";

		_write(
			tablesDir.toPath(),
			"1.0.0/" + className.substring(0, className.length() - 5) +
				"ModelImpl.java",
			StringBundler.concat(
				"public static final String TABLE_NAME = \"", tableName,
				"\";\npublic static final String TABLE_SQL_CREATE = \"", sql,
				"\";\n"));

		new UpgradeTableBuilder(
			baseDir.getAbsolutePath(), true, null, tablesDir.getAbsolutePath());

		Assert.assertEquals(
			StringBundler.concat(
				copyright,
				"\n\npackage com.liferay.test.upgrade.v1_0_0.util;\n\n",
				"import com.liferay.portal.kernel.upgrade.UpgradeProcess;\n",
				"import com.liferay.portal.kernel.upgrade.",
				"UpgradeProcessFactory;\n\n/**\n * @author ", author,
				"\n * @generated\n * @see ",
				UpgradeTableBuilder.class.getName(), "\n */\npublic class ",
				className,
				" {\n\n\tpublic static UpgradeProcess create() {\n\t\treturn ",
				"UpgradeProcessFactory.createTableIfMissing(\n\t\t\t",
				"_TABLE_NAME, _TABLE_SQL_CREATE);\n\t}\n\n\tprivate static ",
				"final String _TABLE_NAME = \"", tableName,
				"\";\n\n\tprivate static final String _TABLE_SQL_CREATE = \"",
				sql, "\";\n\n}"),
			new String(Files.readAllBytes(tablePath), StandardCharsets.UTF_8));
	}

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private Path _write(Path dirPath, String fileName, String content)
		throws Exception {

		Path path = dirPath.resolve(fileName);

		Files.createDirectories(path.getParent());

		Files.write(path, content.getBytes(StandardCharsets.UTF_8));

		return path;
	}

}