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

package com.liferay.osb.community.github.internal.util;

import com.liferay.osb.community.github.exception.GitHubAPIException;
import com.liferay.osb.community.github.exception.GitHubContributorAvatarURLException;
import com.liferay.osb.community.github.exception.GitHubContributorContributionsException;
import com.liferay.osb.community.github.exception.GitHubContributorCountException;
import com.liferay.osb.community.github.exception.GitHubContributorNameException;
import com.liferay.osb.community.github.exception.GitHubRepositoryCommitsException;
import com.liferay.osb.community.github.exception.GitHubRepositoryOpenIssuesException;
import com.liferay.osb.community.github.exception.GitHubRepositoryStarsException;
import com.liferay.osb.community.github.exception.GitHubRepositoryURLException;
import com.liferay.osb.community.github.model.GitHubContributor;
import com.liferay.osb.community.github.model.GitHubRepository;
import com.liferay.osb.community.github.service.GitHubContributorLocalServiceUtil;
import com.liferay.osb.community.github.service.GitHubRepositoryLocalServiceUtil;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;

import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

/**
 * @author Haote Chou
 */
public class GitHubCommunicatorUtil {

	public static GitHubRepository getRepository(
			String owner, String name, String apiKey)
		throws Exception {

		validateAPIKey(apiKey);

		Http.Options options = new Http.Options();

		StringBundler sb = new StringBundler(5);

		sb.append(_API_CALL_PREFIX);
		sb.append("repos/");
		sb.append(owner);
		sb.append(StringPool.SLASH);
		sb.append(name);

		String apiCallURL = sb.toString();

		apiCallURL = HttpUtil.addParameter(apiCallURL, "access_token", apiKey);

		options.setLocation(apiCallURL);

		String content = HttpUtil.URLtoString(options);

		Http.Response response = options.getResponse();

		if (response.getResponseCode() != HttpServletResponse.SC_OK) {
			throw new Exception(
				"Unable to get repository " + name + " from GitHub");
		}

		JSONObject jsonObject = JSONFactoryUtil.createJSONObject(content);

		String createAt = jsonObject.getString("created_at");

		Calendar calendar = Calendar.getInstance();

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss'Z'");

		simpleDateFormat.setCalendar(calendar);

		calendar.setTime(simpleDateFormat.parse(createAt));

		Date repositoryCreateDate = calendar.getTime();

		String url = jsonObject.getString("html_url");
		int openIssues = jsonObject.getInt("open_issues");
		int stars = jsonObject.getInt("stargazers_count");

		int commits = getCommits(owner, name, apiKey);

		validateGitHubRepository(commits, openIssues, stars, url);

		GitHubRepository gitHubRepository =
			GitHubRepositoryLocalServiceUtil.createGitHubRepository(0);

		gitHubRepository.setCommits(commits);
		gitHubRepository.setOpenIssues(openIssues);
		gitHubRepository.setStars(stars);
		gitHubRepository.setUrl(url);
		gitHubRepository.setRepositoryCreateDate(repositoryCreateDate);

		return gitHubRepository;
	}

	public static List<GitHubContributor> getTopContributors(
			String owner, String name, int count, String apiKey)
		throws Exception {

		validateCount(count);

		validateAPIKey(apiKey);

		Http.Options options = new Http.Options();

		StringBundler sb = new StringBundler(6);

		sb.append(_API_CALL_PREFIX);
		sb.append("repos/");
		sb.append(owner);
		sb.append(StringPool.SLASH);
		sb.append(name);
		sb.append("/contributors");

		String apiCallURL = sb.toString();

		apiCallURL = HttpUtil.addParameter(apiCallURL, "access_token", apiKey);

		apiCallURL = HttpUtil.addParameter(
			apiCallURL, "per_page", String.valueOf(count));

		options.setLocation(apiCallURL);

		String content = HttpUtil.URLtoString(options);

		Http.Response response = options.getResponse();

		if (response.getResponseCode() != HttpServletResponse.SC_OK) {
			throw new Exception(
				"Unable to get contributors from GitHub repository " + name);
		}

		JSONArray jsonArray = JSONFactoryUtil.createJSONArray(content);

		JSONObject jsonObject = null;

		GitHubContributor gitHubContributor = null;

		List<GitHubContributor> gitHubContributors = new LinkedList<>();

		for (int i = 0; i < count; i++) {
			jsonObject = jsonArray.getJSONObject(i);

			String avatarURL = jsonObject.getString("avatar_url");
			int contributions = jsonObject.getInt("contributions");

			String contributorName = getContributorName(
				jsonObject.getString("login"), apiKey);

			String profileURL = jsonObject.getString("html_url");

			validateGitHubContributor(
				contributorName, avatarURL, contributions);

			gitHubContributor =
				GitHubContributorLocalServiceUtil.createGitHubContributor(0);

			gitHubContributor.setName(contributorName);
			gitHubContributor.setAvatarURL(avatarURL);
			gitHubContributor.setContributions(contributions);
			gitHubContributor.setProfileURL(profileURL);

			gitHubContributors.add(gitHubContributor);
		}

		return gitHubContributors;
	}

	protected static int getCommits(String owner, String name, String apiKey)
		throws Exception {

		Http.Options options = new Http.Options();

		StringBundler sb = new StringBundler(8);

		sb.append(_API_CALL_PREFIX);
		sb.append("repos");
		sb.append(StringPool.SLASH);
		sb.append(owner);
		sb.append(StringPool.SLASH);
		sb.append(name);
		sb.append(StringPool.SLASH);
		sb.append("commits");

		String apiCallURL = sb.toString();

		apiCallURL = HttpUtil.addParameter(apiCallURL, "access_token", apiKey);

		apiCallURL = HttpUtil.addParameter(
			apiCallURL, "per_page", String.valueOf(1));

		options.setLocation(apiCallURL);

		HttpUtil.URLtoString(options);

		Http.Response response = options.getResponse();

		if (response.getResponseCode() != HttpServletResponse.SC_OK) {
			throw new Exception(
				"Unable to get total commits from GitHub repository " + name);
		}

		String pagenationInfo = response.getHeader("Link");

		int begin = pagenationInfo.lastIndexOf("per_page=1");

		int end = pagenationInfo.lastIndexOf("rel=");

		String commits = pagenationInfo.substring(begin + 16, end - 3);

		return Integer.valueOf(commits);
	}

	protected static String getContributorName(
			String contributorLogin, String apiKey)
		throws Exception {

		Http.Options options = new Http.Options();

		StringBundler sb = new StringBundler(4);

		sb.append(_API_CALL_PREFIX);
		sb.append("users");
		sb.append(StringPool.SLASH);
		sb.append(contributorLogin);

		String apiCallURL = sb.toString();

		apiCallURL = HttpUtil.addParameter(apiCallURL, "access_token", apiKey);

		options.setLocation(apiCallURL);

		String content = HttpUtil.URLtoString(options);

		Http.Response response = options.getResponse();

		if (response.getResponseCode() != HttpServletResponse.SC_OK) {
			throw new Exception(
				"Unable to get contributor name for " + contributorLogin +
					" from GitHub");
		}

		JSONObject jsonObject = JSONFactoryUtil.createJSONObject(content);

		return jsonObject.getString("name");
	}

	protected static void validateAPIKey(String apiKey) throws Exception {
		if (Validator.isNull(apiKey)) {
			throw new GitHubAPIException("API key is empty");
		}
	}

	protected static void validateCount(int count) throws Exception {
		if (count <= 0) {
			throw new GitHubContributorCountException(
				"GitHub contributor max count must grater than 0");
		}
	}

	protected static void validateGitHubContributor(
			String name, String avatarUrl, int contributions)
		throws Exception {

		if (Validator.isNull(name)) {
			throw new GitHubContributorNameException(
				"Contributor name is empty");
		}

		if (!Validator.isUrl(avatarUrl)) {
			throw new GitHubContributorAvatarURLException(
				"Avatar URL is invalid");
		}

		if (contributions < 0) {
			throw new GitHubContributorContributionsException(
				"Contributions are less than 0");
		}
	}

	protected static void validateGitHubRepository(
			int commits, int openIssues, int stars, String url)
		throws Exception {

		if (commits < 0) {
			throw new GitHubRepositoryCommitsException(
				"Commits are less than 0");
		}

		if (openIssues < 0) {
			throw new GitHubRepositoryOpenIssuesException(
				"Open issues are less than 0");
		}

		if (stars < 0) {
			throw new GitHubRepositoryStarsException("Stars are less than 0");
		}

		if (!Validator.isUrl(url)) {
			throw new GitHubRepositoryURLException("Repository URL is invalid");
		}
	}

	private static final String _API_CALL_PREFIX = "https://api.github.com/";

}