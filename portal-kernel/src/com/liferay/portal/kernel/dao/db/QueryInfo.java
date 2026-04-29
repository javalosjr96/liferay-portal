/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.dao.db;

import com.liferay.petra.lang.HashUtil;
import com.liferay.petra.string.StringBundler;

import java.util.Objects;

/**
 * @author Jorge Avalos
 */
public class QueryInfo {

	public QueryInfo(
		long duration, String id, String query, String schema, String state) {

		_duration = duration;
		_id = id;
		_query = query;
		_schema = schema;
		_state = state;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!(object instanceof QueryInfo)) {
			return false;
		}

		QueryInfo queryInfo = (QueryInfo)object;

		if ((_duration == queryInfo._duration) &&
			Objects.equals(_id, queryInfo._id) &&
			Objects.equals(_query, queryInfo._query) &&
			Objects.equals(_schema, queryInfo._schema) &&
			Objects.equals(_state, queryInfo._state)) {

			return true;
		}

		return false;
	}

	public long getDuration() {
		return _duration;
	}

	public String getId() {
		return _id;
	}

	public String getQuery() {
		return _query;
	}

	public String getSchema() {
		return _schema;
	}

	public String getState() {
		return _state;
	}

	@Override
	public int hashCode() {
		int hash = HashUtil.hash(0, _duration);

		hash = HashUtil.hash(hash, _id);
		hash = HashUtil.hash(hash, _query);
		hash = HashUtil.hash(hash, _schema);

		return HashUtil.hash(hash, _state);
	}

	@Override
	public String toString() {
		return StringBundler.concat(
			"{duration=", _duration, ", id=", _id, ", query=", _query,
			", schema=", _schema, ", state=", _state, "}");
	}

	private final long _duration;
	private final String _id;
	private final String _query;
	private final String _schema;
	private final String _state;

}