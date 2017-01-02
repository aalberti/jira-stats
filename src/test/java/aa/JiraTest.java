package aa;

import org.assertj.core.api.Condition;
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
		PrimaIssue issue = Jira.get("PRIN-3046");
		assertThat(issue.getLastTransitionToStatus("Closed"))
			.hasValueSatisfying(new Condition<>(
				t -> "2016-12-28T12:07:37.000+01:00".equals(t.getDate().toString()),
				"closed when closed"
			));
	}
}