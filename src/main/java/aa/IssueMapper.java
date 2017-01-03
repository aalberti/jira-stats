package aa;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.joda.time.DateTime;

import com.atlassian.jira.rest.client.api.domain.ChangelogGroup;
import com.atlassian.jira.rest.client.api.domain.ChangelogItem;
import static aa.Issue.Builder.issue;
import static aa.Transition.Builder.transition;
import static java.util.stream.Collectors.toList;

class IssueMapper {
	Issue toIssue(com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
		List<Transition> history = stream(jiraIssue.getChangelog())
			.flatMap(this::toHistory)
			.collect(toList());
		return issue()
			.withKey(jiraIssue.getKey())
			.withCreationDate(toInstant(jiraIssue.getCreationDate()))
			.withHistory(history)
			.build();
	}

	private Stream<Transition> toHistory(ChangelogGroup changelogGroup) {
		return stream(changelogGroup.getItems())
			.map(i -> toTransition(i, changelogGroup));
	}

	private Transition toTransition(ChangelogItem i, ChangelogGroup changelogGroup) {
		return transition()
			.withAt(toInstant(changelogGroup.getCreated()))
			.withField(i.getField())
			.withTarget(i.getToString())
			.build();
	}

	private static Instant toInstant(DateTime jodaDateTime) {
		return Instant.ofEpochMilli(jodaDateTime.getMillis());
	}

	public static <T> Stream<T> stream(Iterable<T> ts) {
		return StreamSupport.stream(ts.spliterator(), false);
	}
}
