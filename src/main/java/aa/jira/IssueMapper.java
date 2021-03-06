package aa.jira;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.DateTime;

import aa.Issue;
import aa.Transition;
import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.atlassian.jira.rest.client.api.domain.ChangelogGroup;
import com.atlassian.jira.rest.client.api.domain.ChangelogItem;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Status;
import com.atlassian.jira.rest.client.api.domain.User;
import com.atlassian.jira.rest.client.api.domain.Version;
import static aa.Issue.Builder.issue;
import static aa.Transition.Builder.transition;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

class IssueMapper {
	private static final Pattern SPRINT_PARSER_REGEX = Pattern.compile(".*name=(?<name>[^,\\]]*).*");

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
			.withSprints(extractSprints(jiraIssue))
			.withParentKey(extractParent(jiraIssue))
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
			.withSource(i.getFromString())
			.withTarget(i.getToString())
			.build();
	}

	private Collection<String> extractSprints(com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
		return extractField(jiraIssue, "Sprint", JSONArray.class)
			.map(this::toSprintNames)
			.orElse(emptyList());
	}

	private Collection<String> toSprintNames(JSONArray jsonSprints) {
		try {
			Collection<String> sprints = new ArrayList<>();
			for (int i = 0; i < jsonSprints.length(); i++) {
				String serializedSprint = (String) jsonSprints.get(i);
				Matcher matcher = SPRINT_PARSER_REGEX.matcher(serializedSprint);
				if (!matcher.find())
					throw new RuntimeException("Can't parse sprint " + serializedSprint);
				sprints.add(matcher.group("name"));
			}
			return sprints;
		}
		catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	private String extractParent(com.atlassian.jira.rest.client.api.domain.Issue jiraIssue) {
		return extractField(jiraIssue, "Parent", JSONObject.class)
			.map(p -> {
				try {
					return p.getString("key");
				}
				catch (JSONException e) {
					throw new RuntimeException(e);
				}
			})
			.orElseGet(() -> extractField(jiraIssue, "Epic Link", String.class)
				.orElse(null)
			);
	}

	@SuppressWarnings("unchecked")
	private <T> Optional<T> extractField(com.atlassian.jira.rest.client.api.domain.Issue jiraIssue, String name, Class<T> ignored) {
		if (jiraIssue.getFields() == null)
			return Optional.empty();
		return ofNullable(jiraIssue.getFieldByName(name)).map(IssueField::getValue).map(v -> (T) v);
	}

	public static <T> Stream<T> stream(Iterable<T> ts) {
		return ofNullable(ts).map(i -> StreamSupport.stream(i.spliterator(), false)).orElse(Stream.empty());
	}
}
