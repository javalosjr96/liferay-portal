package com.liferay.headless.portal.instances.resource.v1_0.test;

import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.db.partition.DBPartition;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.AssumeTestRule;
import com.liferay.portal.kernel.test.util.HTTPTestUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerMethodTestRule;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class DBPartitionHeadlessTest extends BasePortalInstanceResourceTestCase{
	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new AssumeTestRule("assume"), new LiferayIntegrationTestRule(),
			PermissionCheckerMethodTestRule.INSTANCE);

	public static void assume() {
		Assume.assumeTrue(DBPartition.isPartitionEnabled());

		if (db == null) {
			db = DBManagerUtil.getDB();
		}

		Assume.assumeTrue(db.isSupportsDBPartition());
	}

	@Test
	public void testAddCompanyWithHeadlessAPI() throws Exception {
		JSONObject jsonObject = HTTPTestUtil.invokeToJSONObject(
			JSONUtil.put(
				"domain", "able.com"
			).put(
				"portalInstanceId", "able.com"
			).put(
				"virtualHost", "www.able.com"
			).toString(),
			"headless-portal-instances/v1.0/portal-instances",
			Http.Method.POST);

		long companyId = jsonObject.getLong("companyId");

		Company company = _companyLocalService.fetchCompany(companyId);

		Assert.assertEquals("able.com", company.getWebId());

		_companyLocalService.deleteCompany(company);

		company = _companyLocalService.fetchCompany(companyId);

		Assert.assertNull(company);
	}

	protected static DB db;

	@Inject
	private static CompanyLocalService _companyLocalService;
}
