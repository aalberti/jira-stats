package aa;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraTest {
	@Test
	public void key() throws Exception {
		assertThat(Jira.get("PRIN-3046"))
			.extracting(PrimaIssue::getKey)
			.containsExactly("PRIN-3046");
	}

	@Test
	public void transitions() throws Exception {
		assertThat(Jira.get("PRIN-3046").getTransitions())
			.hasSize(24);
	}

	@Test
	public void lastClosurDate() throws Exception {
		PrimaIssue issueClosedTwice = Jira.get("PPC-11");
		assertThat(issueClosedTwice.getLastTransitionToStatus("Closed"))
			.hasValueSatisfying(t -> assertThat(t.getAt().toString()).isEqualTo("2016-12-29T10:33:40Z"));
	}

	@Test
	public void lastClosureDate_isEmpty_when_openIssue() throws Exception {
		PrimaIssue openIssue = Jira.get("PMT-4");
		assertThat(openIssue.getLastTransitionToStatus("Closed"))
			.isEmpty();
	}

	@Test
	public void leadTime() throws Exception {
		PrimaIssue issueClosedTwice = Jira.get("PPC-11");
		assertThat(issueClosedTwice.getLeadTime())
			.hasValueSatisfying(t -> assertThat(t.toString()).isEqualTo("PT3051H22M35S"));
	}
}