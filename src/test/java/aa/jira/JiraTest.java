package aa.jira;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import aa.Issue;
import static org.assertj.core.api.Assertions.assertThat;

public class JiraTest {
	private Jira jira;

	@Before
	public void setUp() throws Exception {
		jira = new Jira();
		jira.open();
	}

	@After
	public void tearDown() throws Exception {
		jira.close();
	}

	@Test
	public void key() throws Exception {
		assertThat(jira.fetchIssue("PRIN-3046").get())
			.extracting(Issue::getKey)
			.containsExactly("PRIN-3046");
	}

	@Test
	public void project() throws Exception {
		Issue issue = jira.fetchIssue("PRIN-3046").get();
		assertThat(issue.getProject())
			.isEqualTo("PRIN");
	}

	@Test
	public void history() throws Exception {
		Issue issue = jira.fetchIssue("PRIN-3046").get();
		assertThat(issue.getHistory())
			.hasSize(24);
	}

	@Test
	public void lastClosureDate() throws Exception {
		Issue issueClosedTwice = jira.fetchIssue("PPC-11").get();
		assertThat(issueClosedTwice.getClosureDate())
			.hasValueSatisfying(t -> assertThat(t.toString()).isEqualTo("2016-12-29T10:33:40Z"));
	}
}