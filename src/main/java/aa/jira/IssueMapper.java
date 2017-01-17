package aa.jira;

import java.time.Instant;
import java.util.List;
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
			.withProject(toKey(jiraIssue.getProject()))
			.withStatus(toName(jiraIssue.getStatus()))
			.withType(toName(jiraIssue.getIssueType()))
			.withSummary(jiraIssue.getSummary())
			.withReporter(toEmailAddress(jiraIssue.getReporter()))
			.withAssignee(toEmailAddress(jiraIssue.getAssignee()))
			.withCreationDate(toInstant(jiraIssue.getCreationDate()))
			.withUpdateDate(toInstant(jiraIssue.getUpdateDate()))
			.withFixVersions(stream(jiraIssue.getFixVersions()).map(Version::getName).collect(toList()))
			.withHistory(history)
			.build();
	}

	private String toName(IssueType issueType) {
		return ofNullable(issueType).map(IssueType::getName).orElse(null);
	}

	private String toName(Status status) {
		return ofNullable(status).map(Status::getName).orElse(null);
	}

	private String toKey(BasicProject project) {
		return ofNullable(project).map(BasicProject::getKey).orElse(null);
	}

	private String toEmailAddress(User user) {
		return ofNullable(user).map(User::getEmailAddress).orElse(null);
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

	private static Instant toInstant(DateTime dateTime) {
		return ofNullable(dateTime).map(t -> Instant.ofEpochMilli(t.getMillis())).orElse(null);
	}

	public static <T> Stream<T> stream(Iterable<T> ts) {
		return ofNullable(ts).map(i -> StreamSupport.stream(i.spliterator(), false)).orElse(Stream.empty());
	}
}
