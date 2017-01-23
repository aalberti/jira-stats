package aa.jira;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import aa.Issue;
import static aa.jira.Jira.updatedSince;
import static org.assertj.core.api.Assertions.assertThat;

public class JiraMapperTest {
	private Jira jira;
	private JiraConnectionMock connectionMock;

	@Before
	public void setUp() throws Exception {
		connectionMock = new JiraConnectionMock();
		jira = new Jira(connectionMock.getConnection());
		jira.open();
	}

	@After
	public void tearDown() throws Exception {
		jira.close();
	}

	@Test
	public void lastClosureDate() throws Exception {
		DateTime lastDate = new DateTime("2016-12-12T00:00:00Z");
		connectionMock.issue()
			.withKey("FOO-42")
			.withClosureAtDate(lastDate.minusDays(1))
			.withClosureAtDate(lastDate)
			.mock();
		Issue issueClosedTwice = jira.fetchIssue("FOO-42").get();
		assertThat(issueClosedTwice.getClosureDate())
			.hasValueSatisfying(t -> assertThat(t.toString()).isEqualTo("2016-12-12T00:00:00Z"));
	}

	@Test
	public void lastClosureDate_isEmpty_when_openIssue() throws Exception {
		connectionMock.issue()
			.withKey("FOO-42")
			.mock();
		Issue openIssue = jira.fetchIssue("FOO-42").get();
		assertThat(openIssue.getClosureDate())
			.isEmpty();
	}

	@Test
	public void leadTime() throws Exception {
		DateTime creationDate = new DateTime("2016-12-12T00:00:00Z");
		connectionMock.issue()
			.withKey("FOO-42")
			.withCreationDate(creationDate)
			.withClosureAtDate(creationDate.plusHours(24))
			.mock();
		Issue issueClosedTwice = jira.fetchIssue("FOO-42").get();
		assertThat(issueClosedTwice.getLeadTime())
			.hasValueSatisfying(t -> assertThat(t.toString()).isEqualTo("PT24H"));
	}

	@Test
	public void leadTime_isEmpty_when_openIssue() throws Exception {
		connectionMock.issue()
			.withKey("FOO-42")
			.withCreationDate(new DateTime("2016-12-12T00:00:00Z"))
			.mock();
		Issue openIssue = jira.fetchIssue("FOO-42").get();
		assertThat(openIssue.getLeadTime())
			.isEmpty();
	}

	@Test
	public void updated_interval() throws Exception {
		connectionMock.issue()
			.withKey("KEY")
			.withJql("updated >= \"2016-12-12 00:00\" and updated <= \"2016-12-13 00:00\"")
			.mock();
		jira.fetchIssues(updatedSince(parisTime("2016-12-12 00:00")).updatedUntil(parisTime("2016-12-13 00:00")))
			.map(Issue::getKey)
			.test().await()
			.assertValue("KEY");
	}

	@Test
	public void devTime() throws Exception {
		DateTime creationDate = new DateTime("2016-12-12T00:00:00Z");
		connectionMock.issue()
			.withKey("KEY")
			.withCreationDate(creationDate)
			.withSprintAssignmentAtDate(creationDate.plusHours(24))
			.withClosureAtDate(creationDate.plusHours(48))
			.mock();
		Issue issue = jira.fetchIssue("KEY").get();
		assertThat(issue.getDevTime())
			.hasValueSatisfying(t -> assertThat(t.toString()).isEqualTo("PT24H"));
	}

	@Test
	public void devTime_when_severalOriginalSprintAssignments() throws Exception {
		DateTime creationDate = new DateTime("2016-12-12T00:00:00Z");
		connectionMock.issue()
			.withKey("KEY")
			.withCreationDate(creationDate)
			.withSprintAssignmentAtDate(creationDate.plusHours(24))
			.withSprintResetAtDate(creationDate.plusHours(48))
			.withSprintAssignmentAtDate(creationDate.plusHours(72))
			.withClosureAtDate(creationDate.plusHours(96))
			.mock();
		Issue issue = jira.fetchIssue("KEY").get();
		assertThat(issue.getDevTime())
			.hasValueSatisfying(t -> assertThat(t.toString()).isEqualTo("PT72H"));
	}

	@Test
	public void devTime_empty_when_notClosed() throws Exception {
		DateTime creationDate = new DateTime("2016-12-12T00:00:00Z");
		connectionMock.issue()
			.withKey("KEY")
			.withCreationDate(creationDate)
			.withSprintAssignmentAtDate(creationDate.plusHours(24))
			.mock();
		Issue issue = jira.fetchIssue("KEY").get();
		assertThat(issue.getDevTime())
			.isEmpty();
	}

	@Test
	public void devTime_empty_when_neverAssignedToSprint() throws Exception {
		DateTime creationDate = new DateTime("2016-12-12T00:00:00Z");
		connectionMock.issue()
			.withKey("KEY")
			.withCreationDate(creationDate)
			.withClosureAtDate(creationDate.plusHours(24))
			.mock();
		Issue issue = jira.fetchIssue("KEY").get();
		assertThat(issue.getDevTime())
			.isEmpty();
	}

	private Instant parisTime(String time) {
		return Instant.from(DateTimeFormatter
			.ofPattern("yyyy-MM-dd HH:mm")
			.withZone(ZoneId.of("Europe/Paris"))
			.parse(time));
	}
}
