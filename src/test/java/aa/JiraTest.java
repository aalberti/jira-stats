package aa;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraTest {
	@Test
	public void get_issue() throws Exception {
		assertThat(Jira.get("PRIN-3046"))
			.extracting(PrimaIssue::getKey)
			.containsExactly("PRIN-3046");
	}

	@Test
	public void get_transitions() throws Exception {
		assertThat(Jira.get("PRIN-3046").getTransitions())
			.hasSize(24);
	}

	@Test
	public void get_last_closure_date() throws Exception {
		//PPC-11 was closed twice
		PrimaIssue issue = Jira.get("PPC-11");
		assertThat(issue.getLastTransitionToStatus("Closed"))
			.hasValueSatisfying(t -> assertThat(t.getDate().toString()).isEqualTo("2016-12-29T11:33:40.000+01:00"));
	}
}