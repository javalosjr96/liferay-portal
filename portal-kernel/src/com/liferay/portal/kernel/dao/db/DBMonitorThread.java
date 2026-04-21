/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.dao.db;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PropsValues;

import java.sql.Connection;
import java.sql.SQLTimeoutException;

import java.util.List;

/**
 * @author Jorge Avalos
 */
public class DBMonitorThread extends Thread {

	public DBMonitorThread(DB db) {
		super("Database-Monitor-Thread");

		setDaemon(true);

		_db = db;

		_monitorEnabled = PropsValues.UPGRADE_QUERY_MONITOR_ENABLED;
		_running = true;
	}

	public void close() {
		_running = false;
		interrupt();
	}

	@Override
	public void run() {
		if (!_monitorEnabled) {
			return;
		}

		if (_log.isInfoEnabled()) {
			_log.info("Starting upgrade monitor thread");
		}

		while (_running && _monitorEnabled) {
			try {
				Thread.sleep(_QUERY_INTERVAL);

				_checkDatabaseQueries();
			}
			catch (InterruptedException interruptedException) {
				_running = false;
				Thread.currentThread(
				).interrupt();

				if (_log.isDebugEnabled()) {
					_log.debug(
						"Upgrade monitor thread interrupted",
						interruptedException);
				}
			}
			catch (SQLTimeoutException sqlTimeoutException) {
				if (_log.isWarnEnabled()) {
					_log.warn(
						StringBundler.concat(
							"Timeout occurred while executing query to ",
							"monitor upgrade query execution status. ",
							"Monitoring will be disabled for the remainder of ",
							"the upgrade. Please verify database resource ",
							"usage.", sqlTimeoutException));
				}

				_monitorEnabled = false;
			}
			catch (Exception exception) {
				if (_log.isWarnEnabled()) {
					_log.warn(
						StringBundler.concat(
							"Unable to execute query to monitor upgrade query ",
							"execution status. Monitoring will be disabled ",
							"for the remainder of the upgrade. Please verify ",
							"database permissions for execution of: ",
							_db.getActiveQueriesSQL()),
						exception);
				}

				_monitorEnabled = false;
			}
		}
	}

	private void _checkDatabaseQueries() throws Exception {
		try (Connection connection = DataAccess.getConnection()) {
			List<DB.RunningQuery> activeQueries = _db.getActiveQueries(
				connection);

			for (DB.RunningQuery activeQuery : activeQueries) {
				String schema = activeQuery.getSchema();

				if ((schema == null) || schema.isEmpty()) {
					schema = "Unknown";
				}

				if (activeQuery.isLocked() &&
					(activeQuery.getDuration() >
						PropsValues.UPGRADE_QUERY_MONITOR_LOCK_THRESHOLD)) {

					if (_log.isWarnEnabled()) {
						_log.warn(
							StringBundler.concat(
								"LOCKED QUERY: Session ID ",
								activeQuery.getId(), " in schema ", schema,
								" with state ", activeQuery.getState(),
								" has been waiting on resources for ",
								activeQuery.getDuration(), " ms. Query: ",
								activeQuery.getQuery()));
					}
				}
			}
		}
	}

	private static final long _QUERY_INTERVAL = 60000;

	private static final Log _log = LogFactoryUtil.getLog(
		DBMonitorThread.class);

	private final DB _db;
	private boolean _monitorEnabled;
	private volatile boolean _running;

}