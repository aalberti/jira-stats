package aa.jira;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.joda.time.DateTime;

import aa.Issue;
import aa.Transition;
import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.atlassian.jira.rest.client.api.domain.ChangelogGroup;
import com.atlassian.jira.rest.client.api.domain.ChangelogItem;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Status;
import com.atlassian.jira.rest.client.api.domain.User;
import com.atlassian.jira.rest.client.api.domain.Version;
import static aa.Issue.Builder.issue;
import static aa.Transition.Builder.transition;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

class IssueMapper {
	Issue toIssue(com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
		List<Transition> history = stream(jiraIssue.getChangelog())
			.flatMap(this::toHistory)
			.collect(toList());
		return issue()
			.withKey(jiraIssue.getKey())
			.withProject(orNull(jiraIssue.getProject(), BasicProject::getKey))
			.withStatus(orNull(jiraIssue.getStatus(), Status::getName))
			.withType(orNull(jiraIssue.getIssueType(), IssueType::getName))
			.withSummary(jiraIssue.getSummary())
			.withReporter(orNull(jiraIssue.getReporter(), User::getEmailAddress))
			.withAssignee(orNull(jiraIssue.getAssignee(), User::getEmailAddress))
			.withCreationDate(toInstant(jiraIssue.getCreationDate()))
			.withUpdateDate(toInstant(jiraIssue.getUpdateDate()))
			.withFixVersions(stream(jiraIssue.getFixVersions()).map(Version::getName).collect(toList()))
			.withHistory(history)
			.build();
	}

	private <T, R> R orNull(T source, Function<T, R> mapper) {
		return ofNullable(source).map(mapper).orElse(null);
	}

	private static Instant toInstant(DateTime dateTime) {
		return ofNullable(dateTime).map(DateTime::getMillis).map(Instant::ofEpochMilli).orElse(null);
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

	public static <T> Stream<T> stream(Iterable<T> ts) {
		return ofNullable(ts).map(i -> StreamSupport.stream(i.spliterator(), false)).orElse(Stream.empty());
	}
}
