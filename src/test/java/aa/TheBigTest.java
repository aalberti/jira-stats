package aa;

import java.time.Duration;
import java.util.Optional;

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
					.doOnNext(i -> System.out.println("Fetched " + i.getKey() + " lead time: " + i.getLeadTime().toString()))
					.map(Issue::getLeadTime)
					.filter(Optional::isPresent)
					.map(Optional::get)
					.reduce(new long[]{0, 0}, (sumAndCount, leadTime) -> new long[]{sumAndCount[0] + leadTime.toMillis(), sumAndCount[1] + 1} )
					.map(sumAndCount -> sumAndCount[0]/sumAndCount[1])
					.map(Duration::ofMillis)
					.test()
					.await()
					.assertComplete()
					.values())
				.hasSize(1)
				.allMatch(leadTime -> leadTime.toMillis() < 0);
		}
	}
}