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
				jiraConnection.fetchIssues()
					.doOnNext(i -> System.out.println("Fetched " + i.getKey()))
					.test()
					.await()
					.assertComplete()
					.valueCount()
			).isBetween(232, 250);
		}
	}
}