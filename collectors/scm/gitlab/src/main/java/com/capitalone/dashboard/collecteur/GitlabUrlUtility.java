package com.capitalone.dashboard.collecteur;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.capitalone.dashboard.model.GitlabGitRepo;

@Component
public class GitlabUrlUtility {
	
	private static final Log LOG = LogFactory.getLog(GitlabUrlUtility.class);
	
	private GitlabSettings gitlabSettings;
	
	private static final String GIT_EXTENSION = ".git";
	private static final String PROTOCOL = "https";
    private static final String SEGMENT_API = "/api/v3/projects/";
	private static final String COMMITS_API = "/repository/commits/";
	private static final String PRIVATE_TOKEN_QUERY_PARAM_KEY = "private_token";
	private static final String DATE_QUERY_PARAM_KEY = "since";
	private static final String BRANCH_QUERY_PARAM_KEY = "ref_name";
	private static final String PER_PAGE_QUERY_PARAM_KEY = "per_page";
    private static final String PUBLIC_GITLAB_HOST_NAME = "gitlab.company.com";
	private static final int FIRST_RUN_HISTORY_DEFAULT = 14;
	
	@Autowired
	public GitlabUrlUtility(GitlabSettings gitlabSettings) {
		this.gitlabSettings = gitlabSettings;
	}
	
	public URI buildApiUrl(GitlabGitRepo repo, boolean firstRun, int resultsPerPage) {
		String repoUrl = repo.getRepoUrl();
        if (repoUrl.endsWith(GIT_EXTENSION)) {
            repoUrl = StringUtils.removeEnd(repoUrl, GIT_EXTENSION);
        }
        
		String repoName = getRepoName(repoUrl);
		String host = getRepoHost();
		String date = getDateForCommits(repo, firstRun);

		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		URI uri = builder.scheme(PROTOCOL)
				.host(host)
				.path(SEGMENT_API)
				.path(repoName)
				.path(COMMITS_API)
				.queryParam(BRANCH_QUERY_PARAM_KEY, repo.getBranch())
				.queryParam(DATE_QUERY_PARAM_KEY, date)
				.queryParam(PER_PAGE_QUERY_PARAM_KEY, resultsPerPage)
				.queryParam(PRIVATE_TOKEN_QUERY_PARAM_KEY, gitlabSettings.getApiToken())
				.build(true).toUri();

		return uri;
    }

	private String getRepoHost() {
		String providedGitLabHost = gitlabSettings.getHost();
		String apiHost;
		if (StringUtils.isBlank(providedGitLabHost)) {
			apiHost = PUBLIC_GITLAB_HOST_NAME;
		} else {
			apiHost = providedGitLabHost;
		}
		return apiHost;
	}

	private String getRepoName(String repoUrl) {
		String repoName = "";
		try {
			URL url = new URL(repoUrl);
			repoName = url.getFile();
		} catch (MalformedURLException e) {
			LOG.error(e.getMessage());
		}
		repoName = StringUtils.removeStart(repoName, "/");
		repoName = repoName.replace("/", "%2F");
		return repoName;
	}

	private String getDateForCommits(GitlabGitRepo repo, boolean firstRun) {
		Date dt;
		if (firstRun) {
			int firstRunDaysHistory = gitlabSettings.getFirstRunHistoryDays();
			if (firstRunDaysHistory > 0) {
				dt = getDate(new Date(), -firstRunDaysHistory, 0);
			} else {
				dt = getDate(new Date(), -FIRST_RUN_HISTORY_DEFAULT, 0);
			}
		} else {
			dt = getDate(new Date(repo.getLastUpdated()), 0, -10);
		}
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		String thisMoment = df.format(dt);
		return thisMoment;
	}

	private Date getDate(Date dateInstance, int offsetDays, int offsetMinutes) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(dateInstance);
		cal.add(Calendar.DATE, offsetDays);
		cal.add(Calendar.MINUTE, offsetMinutes);
		return cal.getTime();
	}

	public URI updatePage(URI uri, int nextPage) {
		URI updatedUri = UriComponentsBuilder.fromUri(uri).replaceQueryParam("page", nextPage).build(true).toUri();
		return updatedUri;
	}

}
