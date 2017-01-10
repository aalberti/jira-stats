package aa.jira;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import aa.Issue;
import static org.assertj.core.api.Assertions.assertThat;

public class JiraTest {

	private Jira jira = null;

	@Before
	public void setUp() throws Exception {
		jira = new Jira();
		jira.open();
	}

	@After
	public void tearDown() throws Exception {
		if (jira != null) {
			jira.close();
		}
		jira = null;
	}

	@Test
	public void key() throws Exception {
		assertThat(jira.fetchIssue("PRIN-3046").get())
			.extracting(Issue::getKey)
			.containsExactly("PRIN-3046");
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

	@Test
	public void lastClosureDate_isEmpty_when_openIssue() throws Exception {
		Issue openIssue = jira.fetchIssue("PRIN-2276").get();
		assertThat(openIssue.getClosureDate())
			.isEmpty();
	}

	@Test
	public void leadTime() throws Exception {
		Issue issueClosedTwice = jira.fetchIssue("PPC-11").get();
		assertThat(issueClosedTwice.getLeadTime())
			.hasValueSatisfying(t -> assertThat(t.toString()).isEqualTo("PT3051H22M35S"));
	}

	@Test
	public void leadTime_isEmpty_when_openIssue() throws Exception {
		Issue openIssue = jira.fetchIssue("PRIN-2276").get();
		assertThat(openIssue.getLeadTime())
			.isEmpty();
	}
}