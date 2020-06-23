/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Liferay Enterprise
 * Subscription License ("License"). You may not use this file except in
 * compliance with the License. You can obtain a copy of the License by
 * contacting Liferay, Inc. See the License for the specific language governing
 * permissions and limitations under the License, including but not limited to
 * distribution rights of the Software.
 *
 *
 *
 */

package com.liferay.osb.community.meetup.service.impl;

import com.liferay.osb.community.meetup.model.MeetupGroup;
import com.liferay.osb.community.meetup.service.base.MeetupGroupLocalServiceBaseImpl;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.service.ServiceContext;

import java.util.Date;

/**
 * @author Jamie Sammons
 */
public class MeetupGroupLocalServiceImpl
	extends MeetupGroupLocalServiceBaseImpl {

	@Override
	public MeetupGroup addMeetupGroup(
			String name, String city, int memberCount, String description,
			String url, ServiceContext serviceContext)
		throws PortalException {

		Date now = new Date();

		long meetupGroupId = counterLocalService.increment();

		MeetupGroup meetupGroup = meetupGroupPersistence.create(meetupGroupId);

		meetupGroup.setGroupId(serviceContext.getScopeGroupId());
		meetupGroup.setCompanyId(serviceContext.getCompanyId());
		meetupGroup.setCreateDate(serviceContext.getCreateDate(now));
		meetupGroup.setModifiedDate(serviceContext.getModifiedDate(now));
		meetupGroup.setName(name);
		meetupGroup.setCity(city);
		meetupGroup.setMemberCount(memberCount);
		meetupGroup.setDescription(description);
		meetupGroup.setUrl(url);

		return meetupGroupPersistence.update(meetupGroup);
	}

	public void deleteMeetupGroups() throws PortalException {
		meetupGroupPersistence.removeAll();
	}

}