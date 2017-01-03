package aa;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Properties;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.auth.BasicHttpAuthenticationHandler;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.util.concurrent.Promise;

public class JiraConnection implements Closeable {

	private JiraRestClient jiraRestClient;

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

	@Override
	public void close() throws IOException {
		jiraRestClient.close();
	}

	public Promise<Issue> getIssue(String issueKey) {
		return jiraRestClient.getIssueClient().getIssue(issueKey, EnumSet.of(IssueRestClient.Expandos.CHANGELOG));
	}
}