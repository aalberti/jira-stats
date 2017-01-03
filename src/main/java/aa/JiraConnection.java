package aa;

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

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.auth.BasicHttpAuthenticationHandler;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.util.concurrent.Promise;
import io.reactivex.Observable;
import static java.util.Arrays.asList;

public class JiraConnection implements Closeable {

	public static final HashSet<String> FIELDS = new HashSet<>(asList("summary", "issuetype", "created", "updated", "project", "status"));
	private JiraRestClient jiraRestClient;
	private IssueMapper mapper;

	public JiraConnection() {
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

	public Promise<Issue> getIssue(String issueKey) {
		return jiraRestClient.getIssueClient()
			.getIssue(issueKey, EnumSet.of(IssueRestClient.Expandos.CHANGELOG))
			.map(mapper::toIssue);
	}

	@Override
	public void close() throws IOException {
		jiraRestClient.close();
	}

	public Observable<Issue> getIssues() {
		return Observable.create(subscriber -> {
			SearchResult searchResult = jiraRestClient.getSearchClient().searchJql("project = prin", 20, 0, FIELDS).get();
			for (com.atlassian.jira.rest.client.api.domain.Issue jiraIssue : searchResult.getIssues()) {
				try {
					Issue issue = getIssue(jiraIssue.getKey()).get();
					subscriber.onNext(issue);
				}
				catch (Exception e) {
					subscriber.onError(e);
					throw new RuntimeException(e);
				}
			}
			subscriber.onComplete();
		});
	}
}