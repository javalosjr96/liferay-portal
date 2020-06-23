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

package com.liferay.osb.community.github.service.impl;

import com.liferay.osb.community.github.exception.GitHubAPIException;
import com.liferay.osb.community.github.internal.util.GitHubCommunicatorUtil;
import com.liferay.osb.community.github.internal.util.GitHubServiceConfigurationUtil;
import com.liferay.osb.community.github.model.GitHubContributor;
import com.liferay.osb.community.github.model.GitHubRepository;
import com.liferay.osb.community.github.service.base.GitHubRepositoryLocalServiceBaseImpl;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.util.PortalUtil;

import java.util.Date;
import java.util.List;

/**
 * @author Haote Chou
 */
public class GitHubRepositoryLocalServiceImpl
	extends GitHubRepositoryLocalServiceBaseImpl {

	@Override
	public GitHubRepository deleteGitHubRepository(
		GitHubRepository gitHubRepository) {

		// GitHub repository

		gitHubRepositoryPersistence.remove(gitHubRepository);

		// GitHub contributor

		gitHubContributorPersistence.removeByGitHubRepositoryId(
			gitHubRepository.getGitHubRepositoryId());

		return gitHubRepository;
	}

	@Override
	public GitHubRepository deleteGitHubRepository(long gitHubRepositoryId)
		throws PortalException {

		GitHubRepository gitHubRepository =
			gitHubRepositoryPersistence.findByPrimaryKey(gitHubRepositoryId);

		return deleteGitHubRepository(gitHubRepository);
	}

	public GitHubRepository deleteGitHubRepository(String owner, String name)
		throws PortalException {

		GitHubRepository gitHubRepository =
			gitHubRepositoryPersistence.findByO_N(owner, name);

		return deleteGitHubRepository(gitHubRepository);
	}

	public GitHubRepository getGitHubRepository(
			long userId, String owner, String name)
		throws PortalException {

		GitHubRepository gitHubRepository =
			gitHubRepositoryPersistence.fetchByO_N(owner, name);

		if (gitHubRepository == null) {
			try {
				gitHubRepository = GitHubCommunicatorUtil.getRepository(
					owner, name, GitHubServiceConfigurationUtil.getAPIKey());
			}
			catch (Exception e) {
				throw new GitHubAPIException(e);
			}

			return addGitHubRepository(
				userId, owner, name, gitHubRepository.getCommits(),
				gitHubRepository.getOpenIssues(),
				gitHubRepository.getRepositoryCreateDate(),
				gitHubRepository.getStars(), gitHubRepository.getUrl());
		}

		return gitHubRepository;
	}

	public void updateGitHubRepositoryCache() throws PortalException {
		List<GitHubRepository> gitHubRepositories =
			gitHubRepositoryPersistence.findAll();

		for (GitHubRepository gitHubRepository : gitHubRepositories) {
			GitHubRepository gitHubRepositoryHolder = null;

			try {
				gitHubRepositoryHolder = GitHubCommunicatorUtil.getRepository(
					gitHubRepository.getOwner(), gitHubRepository.getName(),
					GitHubServiceConfigurationUtil.getAPIKey());
			}
			catch (Exception e) {
				throw new GitHubAPIException(e);
			}

			gitHubRepository.setModifiedDate(new Date());
			gitHubRepository.setCommits(gitHubRepositoryHolder.getCommits());
			gitHubRepository.setOpenIssues(
				gitHubRepositoryHolder.getOpenIssues());
			gitHubRepository.setStars(gitHubRepositoryHolder.getStars());
			gitHubRepository.setUrl(gitHubRepositoryHolder.getUrl());

			gitHubRepository = gitHubRepositoryPersistence.update(
				gitHubRepository);

			updateGitHubContributorCache(
				gitHubRepository.getGitHubRepositoryId(),
				gitHubRepository.getOwner(), gitHubRepository.getName());
		}
	}

	protected GitHubRepository addGitHubRepository(
			long userId, String owner, String name, int commits, int openIssues,
			Date repositoryCreateDate, int stars, String url)
		throws PortalException {

		if (userId == 0) {
			userId = userLocalService.getDefaultUserId(
				PortalUtil.getDefaultCompanyId());
		}

		User user = userLocalService.getUser(userId);

		long gitHubRepositoryId = counterLocalService.increment();

		GitHubRepository gitHubRepository = gitHubRepositoryPersistence.create(
			gitHubRepositoryId);

		Date now = new Date();

		gitHubRepository.setCompanyId(user.getCompanyId());
		gitHubRepository.setUserId(userId);
		gitHubRepository.setUserName(user.getFullName());
		gitHubRepository.setCreateDate(now);
		gitHubRepository.setModifiedDate(now);
		gitHubRepository.setOwner(owner);
		gitHubRepository.setName(name);
		gitHubRepository.setCommits(commits);
		gitHubRepository.setOpenIssues(openIssues);
		gitHubRepository.setStars(stars);
		gitHubRepository.setUrl(url);
		gitHubRepository.setRepositoryCreateDate(repositoryCreateDate);

		return gitHubRepositoryPersistence.update(gitHubRepository);
	}

	protected void updateGitHubContributorCache(
			long gitHubRepositoryId, String owner, String name)
		throws PortalException {

		gitHubContributorPersistence.removeByGitHubRepositoryId(
			gitHubRepositoryId);

		Date now = new Date();

		GitHubRepository gitHubRepository =
			gitHubRepositoryPersistence.findByPrimaryKey(gitHubRepositoryId);

		List<GitHubContributor> gitHubContributorHolders = null;

		try {
			gitHubContributorHolders =
				GitHubCommunicatorUtil.getTopContributors(
					owner, name,
					GitHubServiceConfigurationUtil.
						getGitHubContributorMaxCount(),
					GitHubServiceConfigurationUtil.getAPIKey());
		}
		catch (Exception e) {
			throw new GitHubAPIException(e);
		}

		for (GitHubContributor gitHubContributorHolder :
				gitHubContributorHolders) {

			long gitHubContributorId = counterLocalService.increment();

			GitHubContributor gitHubContributor =
				gitHubContributorPersistence.create(gitHubContributorId);

			gitHubContributor.setCompanyId(gitHubRepository.getCompanyId());
			gitHubContributor.setUserId(gitHubRepository.getUserId());
			gitHubContributor.setUserName(gitHubRepository.getUserName());
			gitHubContributor.setCreateDate(now);
			gitHubContributor.setModifiedDate(now);
			gitHubContributor.setGitHubRepositoryId(gitHubRepositoryId);
			gitHubContributor.setName(gitHubContributorHolder.getName());
			gitHubContributor.setAvatarURL(
				gitHubContributorHolder.getAvatarURL());
			gitHubContributor.setContributions(
				gitHubContributorHolder.getContributions());
			gitHubContributor.setProfileURL(
				gitHubContributorHolder.getProfileURL());

			gitHubContributorPersistence.update(gitHubContributor);
		}
	}

}