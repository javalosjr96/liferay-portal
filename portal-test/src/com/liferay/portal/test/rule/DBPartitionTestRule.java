package com.liferay.portal.test.rule;

import com.liferay.portal.kernel.db.partition.DBPartition;
import com.liferay.portal.kernel.exception.NoSuchCompanyException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import com.liferay.portal.kernel.test.rule.AbstractTestRule;
import org.junit.runner.Description;

import java.util.Map;

public class DBPartitionTestRule
	extends AbstractTestRule<Map<String, String>, Map<String, String>> {

	public static final DBPartitionTestRule INSTANCE =
		new DBPartitionTestRule();

	static
	{
		try {
			if (DBPartition.isPartitionEnabled()) {
				try {
					CompanyLocalServiceUtil.getCompanyByWebId("db-partition.com");
					}
				catch (Exception exception) {
					if (exception instanceof NoSuchCompanyException) {
						CompanyLocalServiceUtil.addCompany(
							null, "db-partition.com",
							"db-partition.com",
							"db-partition.com",
							0, true, true,
							null, null,
							null, null, null, null);
					}
			}

			}
		}
		catch (PortalException e) {
			throw new RuntimeException(e);
		}
	}


	@Override
	protected void afterClass(
		Description description, Map<String, String> stringStringMap)
		throws Throwable {

	}

	@Override
	protected void afterMethod(
		Description description, Map<String, String> stringStringMap,
		Object target) throws Throwable {

	}

	@Override
	protected Map<String, String> beforeClass(Description description)
		throws Throwable {
		return null;
	}

	@Override
	protected Map<String, String> beforeMethod(
		Description description, Object target) throws Throwable {
		return null;
	}
}
