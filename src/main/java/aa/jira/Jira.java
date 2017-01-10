package aa.jira;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Properties;

import aa.Issue;
import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.auth.BasicHttpAuthenticationHandler;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.util.concurrent.Promise;
import io.reactivex.Observable;
import static java.util.Arrays.asList;

public class Jira implements Closeable {

	private static final HashSet<String> FIELDS = new HashSet<>(asList("summary", "issuetype", "created", "updated", "project", "status"));
	private JiraRestClient jiraRestClient;
	private IssueMapper mapper;

	public Jira() {
		this.mapper = new IssueMapper();
	}

	public void open() {
		try {
			URI uri = new URI("https://applications.prima-solutions.com/jira/");
			jiraRestClient = new AsynchronousJiraRestClientFactory().create(uri, authentication());
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	private BasicHttpAuthenticationHandler authentication() {
		Properties properties = loadProperties();
		String user = (String) properties.get("user");
		String password = (String) properties.get("password");
		return new BasicHttpAuthenticationHandler(user, new String(Base64.getDecoder().decode(password)));
	}

	private Properties loadProperties() {
		try {
			Properties properties = new Properties();
			properties.load(new FileInputStream(Paths.get(System.getProperty("user.home"), ".jira").toFile()));
			return properties;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Promise<Issue> fetchIssue(String issueKey) {
		return jiraRestClient.getIssueClient()
			.getIssue(issueKey, EnumSet.of(IssueRestClient.Expandos.CHANGELOG))
			.map(mapper::toIssue);
	}

	@Override
	public void close() throws IOException {
		jiraRestClient.close();
	}

	public Observable<Issue> fetchIssues() {
		return Observable.<com.atlassian.jira.rest.client.api.domain.Issue>create(subscriber -> {
			if (subscriber.isDisposed())
				return;
			Promise<SearchResult> searchResultPromise = jiraRestClient.getSearchClient().searchJql("project = ppc", 1000, 0, FIELDS);
			searchResultPromise.fail(subscriber::onError);
			IssueMapper.stream(searchResultPromise.get().getIssues()).forEach(subscriber::onNext);
			subscriber.onComplete();
		}).map(jiraIssue -> {
			try {
				return fetchIssue(jiraIssue.getKey()).get();
			}
			catch (Exception e) {
				throw new RuntimeException("Can't retrieve issue " + jiraIssue.getKey(), e);
			}
		});
	}
}