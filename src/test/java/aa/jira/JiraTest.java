package aa.jira;

import java.time.Instant;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import aa.Issue;
import static org.assertj.core.api.Assertions.assertThat;

public class JiraTest {
	private static Jira jira;
	private static Issue defaultIssue;

	@BeforeClass
	public static void setUpClass() throws Exception {
		jira = new Jira();
		jira.open();
		defaultIssue = jira.fetchIssue("PRIN-3046").get();
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		jira.close();
	}

	@Test
	public void key() throws Exception {
		assertThat(defaultIssue)
			.extracting(Issue::getKey)
			.containsExactly("PRIN-3046");
	}

	@Test
	public void project() throws Exception {
		assertThat(defaultIssue.getProject())
			.isEqualTo("PRIN");
	}

	@Test
	public void history() throws Exception {
		assertThat(defaultIssue.getHistory())
			.hasSize(25);
	}

	@Test
	public void status() throws Exception {
		assertThat(defaultIssue.getStatus())
			.isEqualTo("Closed");
	}

	@Test
	public void type() throws Exception {
		assertThat(defaultIssue.getType())
			.isEqualTo("Bug");
	}

	@Test
	public void fixVersions() throws Exception {
		assertThat(defaultIssue.getFixVersions())
			.containsExactly("Pi-8.1.1", "Pi-8.2");
	}

	@Test
	public void assignee() throws Exception {
		assertThat(defaultIssue.getAssignee())
			.isEqualTo("Antoine.Alberti@prima-solutions.com");
	}

	@Test
	public void reporter() throws Exception {
		assertThat(defaultIssue.getReporter())
			.isEqualTo("Antoine.Alberti@prima-solutions.com");
	}

	@Test
	public void summary() throws Exception {
		assertThat(defaultIssue.getSummary())
			.isEqualTo("Fix fleet tests for good");
	}

	@Test
	public void creationDate() throws Exception {
		assertThat(defaultIssue.getCreationDate())
			.isEqualTo(Instant.parse("2016-11-23T11:39:03Z"));
	}

	@Test
	public void updateDate() throws Exception {
		assertThat(defaultIssue.getUpdateDate())
			.isEqualTo(Instant.parse("2017-01-18T13:13:41Z"));
	}

	@Test
	public void sprints() throws Exception {
		assertThat(defaultIssue.getSprints())
			.containsExactly("v820.0", "v820.1");
	}

	@Test
	public void lastClosureDate() throws Exception {
		Issue issueClosedTwice = jira.fetchIssue("PPC-11").get();
		assertThat(issueClosedTwice.getClosureDate())
			.hasValueSatisfying(t -> assertThat(t.toString()).isEqualTo("2016-12-29T10:33:40Z"));
	}
}