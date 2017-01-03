package aa;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.joda.time.DateTime;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.ChangelogGroup;
import com.atlassian.jira.rest.client.api.domain.ChangelogItem;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.auth.BasicHttpAuthenticationHandler;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.sun.jersey.core.util.Base64;
import static aa.PrimaIssue.Builder.issue;
import static aa.PrimaIssueTransition.Builder.transition;
import static java.util.stream.Collectors.toList;

public class Jira {
	public static void main(String[] args) throws URISyntaxException, ExecutionException, InterruptedException, IOException {
		get("PRIN-3047");
	}

	public static PrimaIssue get(String issueKey) throws URISyntaxException, IOException, InterruptedException, ExecutionException {
		URI uri = new URI("https://applications.prima-solutions.com/jira/");
		try (JiraRestClient client = new AsynchronousJiraRestClientFactory().create(uri, authentication())) {
			return client.getIssueClient().getIssue(issueKey, EnumSet.of(IssueRestClient.Expandos.CHANGELOG))
				.done(i -> System.out.println(i.getKey() + ": " + i.getSummary()))
				.map(Jira::toIssue)
				.get();
		}
	}

	private static BasicHttpAuthenticationHandler authentication() {
		Properties properties = loadProperties();
		String user = (String) properties.get("user");
		String password = (String) properties.get("password");
		return new BasicHttpAuthenticationHandler(user, new String(Base64.decode(password)));
	}

	private static Properties loadProperties() {
		try {
			Properties properties = new Properties();
			properties.load(new FileInputStream(Paths.get(System.getProperty("user.home"), ".jira").toFile()));
			return properties;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static PrimaIssue toIssue(Issue jiraIssue) {
		List<PrimaIssueTransition> transitions = stream(jiraIssue.getChangelog())
			.flatMap(Jira::toTransitions)
			.collect(toList());
		return issue()
			.withKey(jiraIssue.getKey())
			.withCreationDate(toInstant(jiraIssue.getCreationDate()))
			.withTransitions(transitions)
			.build();
	}

	private static Stream<PrimaIssueTransition> toTransitions(ChangelogGroup changelogGroup) {
		System.out.println(changelogGroup.getCreated() + " by " + changelogGroup.getAuthor().getDisplayName());
		return stream(changelogGroup.getItems())
			.peek(i -> System.out.println("\t" + i.getField() + "(" + i.getFieldType() + ") from " + i.getFromString() + " to " + i.getToString() + " [a.k.a " + i.toString() + "]"))
			.map(i -> toTransition(i, changelogGroup));
	}

	private static PrimaIssueTransition toTransition(ChangelogItem i, ChangelogGroup changelogGroup) {
		return transition()
			.withAt(toInstant(changelogGroup.getCreated()))
			.withField(i.getField())
			.withTarget(i.getToString())
			.build();
	}

	private static Instant toInstant(DateTime jodaDateTime) {
		return Instant.ofEpochMilli(jodaDateTime.getMillis());
	}

	private static <T> Stream<T> stream(Iterable<T> ts) {
		return StreamSupport.stream(ts.spliterator(), false);
	}
}
