package aa;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.joda.time.DateTime;

import com.atlassian.jira.rest.client.api.domain.ChangelogGroup;
import com.atlassian.jira.rest.client.api.domain.ChangelogItem;
import static aa.Issue.Builder.issue;
import static aa.Transition.Builder.transition;
import static java.util.stream.Collectors.toList;

public class Jira {
	public static void main(String[] args) throws URISyntaxException, ExecutionException, InterruptedException, IOException {
		JiraConnection jiraConnection = new JiraConnection();
		jiraConnection.open();
		try (JiraConnection ignored = jiraConnection) {
			jiraConnection.getIssue("PRIN-3047")
				.done(i -> System.out.println(i.getKey() + ": " + i.getSummary()))
				.map(Jira::toIssue)
				.get();
		}
	}

	public static Issue get(String issueKey) throws URISyntaxException, IOException, InterruptedException, ExecutionException {
		JiraConnection jiraConnection = new JiraConnection();
		jiraConnection.open();
		try (JiraConnection ignored = jiraConnection) {
			return jiraConnection.getIssue(issueKey)
				.done(i -> System.out.println(i.getKey() + ": " + i.getSummary()))
				.map(Jira::toIssue)
				.get();
		}
	}

	private static Issue toIssue(com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
		List<Transition> history = stream(jiraIssue.getChangelog())
			.flatMap(Jira::toHistory)
			.collect(toList());
		return issue()
			.withKey(jiraIssue.getKey())
			.withCreationDate(toInstant(jiraIssue.getCreationDate()))
			.withHistory(history)
			.build();
	}

	private static Stream<Transition> toHistory(ChangelogGroup changelogGroup) {
		System.out.println(changelogGroup.getCreated() + " by " + changelogGroup.getAuthor().getDisplayName());
		return stream(changelogGroup.getItems())
			.peek(i -> System.out.println("\t" + i.getField() + "(" + i.getFieldType() + ") from " + i.getFromString() + " to " + i.getToString() + " [a.k.a " + i.toString() + "]"))
			.map(i -> toTransition(i, changelogGroup));
	}

	private static Transition toTransition(ChangelogItem i, ChangelogGroup changelogGroup) {
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
