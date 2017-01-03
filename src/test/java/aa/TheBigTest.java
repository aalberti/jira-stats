package aa;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TheBigTest {
	@Test
	public void gimme_the_big_stuff() throws Exception {
		JiraConnection jiraConnection = new JiraConnection();
		jiraConnection.open();
		try (JiraConnection ignored = jiraConnection) {
			assertThat(
				jiraConnection.fetchIssues().test()
					.await()
					.assertComplete()
					.valueCount()
			).isGreaterThan(50000);
		}
	}
}