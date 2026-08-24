#!/bin/bash
#
# Test plan for branch: LPD-84027-monitor
# Generated: 2026-04-28
# Estimated time: ~15m / 20m budget
#
# Changes: 8 commits, 16 files changed (+679 / -2)
# Affected areas: DB interface (portal-kernel), per-vendor DB impls
# (portal-impl HypersonicDB/MySQLDB/PostgreSQLDB, dxp DB2DB/OracleDB/SQLServerDB),
# UpgradeQueryMonitor, DBUpgrader, new upgrade.query.monitor.* props
#
# Functional/Playwright/Poshi: skipped — backend infrastructure change with no UI surface.
# Integration tests assume a portal is running with the changes deployed.
#

REPO_ROOT="$(cd "$(dirname "${0}")" && pwd)"
EXIT_CODE=0

# BaseDB, HypersonicDB, MySQLDB, PostgreSQLDB were modified — one ant pass runs all three matching unit tests under com.liferay.portal.dao.db
(cd "${REPO_ROOT}" && ANT_OPTS="-Xmx2560m" ant test-unit -Dtest.package=com.liferay.portal.dao.db) || EXIT_CODE=1

# DB2DB, OracleDB, SQLServerDB in dxp/portal-dao-db were modified — module's test task covers all three
"${REPO_ROOT}/gradlew" --project-dir "${REPO_ROOT}/modules" :dxp:apps:portal:portal-dao-db:test || EXIT_CODE=1

# DBTest was edited to add testGetLockedQueries and testGetLongRunningQueries against the new DB API; running the full class also guards against regressions in the existing alterColumn/getIndex paths through the modified BaseDB
"${REPO_ROOT}/gradlew" --project-dir "${REPO_ROOT}/modules" :apps:portal:portal-dao-test:testIntegration --tests "com.liferay.portal.dao.db.test.DBTest" || EXIT_CODE=1

exit ${EXIT_CODE}