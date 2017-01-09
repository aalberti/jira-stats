package aa;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraStatisticsTest {
	@Test
	public void key() throws Exception {
		assertThat(JiraStatistics.get("PRIN-3046"))
			.extracting(Issue::getKey)
			.containsExactly("PRIN-3046");
	}

	@Test
	public void history() throws Exception {
		assertThat(JiraStatistics.get("PRIN-3046").getHistory())
			.hasSize(24);
	}

	@Test
	public void lastClosureDate() throws Exception {
		Issue issueClosedTwice = JiraStatistics.get("PPC-11");
		assertThat(issueClosedTwice.getClosureDate())
			.hasValueSatisfying(t -> assertThat(t.toString()).isEqualTo("2016-12-29T10:33:40Z"));
	}

	@Test
	public void lastClosureDate_isEmpty_when_openIssue() throws Exception {
		Issue openIssue = JiraStatistics.get("PRIN-2276");
		assertThat(openIssue.getClosureDate())
			.isEmpty();
	}

	@Test
	public void leadTime() throws Exception {
		Issue issueClosedTwice = JiraStatistics.get("PPC-11");
		assertThat(issueClosedTwice.getLeadTime())
			.hasValueSatisfying(t -> assertThat(t.toString()).isEqualTo("PT3051H22M35S"));
	}

	@Test
	public void leadTime_isEmpty_when_openIssue() throws Exception {
		Issue openIssue = JiraStatistics.get("PRIN-2276");
		assertThat(openIssue.getLeadTime())
			.isEmpty();
	}
}