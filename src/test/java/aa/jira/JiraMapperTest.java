package aa.jira;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import aa.Issue;
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
		connectionMock.issue()
			.withKey("FOO-42")
			.withClosureAtDate(new DateTime("2016-11-11T00:00:00Z"))
			.withClosureAtDate(new DateTime("2016-12-12T00:00:00Z"))
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
		connectionMock.issue()
			.withKey("FOO-42")
			.withCreationDate(new DateTime("2016-12-12T00:00:00Z"))
			.withClosureAtDate(new DateTime("2016-12-13T00:00:00Z"))
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
}
