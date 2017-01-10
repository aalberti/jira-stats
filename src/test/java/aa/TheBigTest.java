package aa;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.Test;

import aa.db.IssueDB;
import aa.jira.Jira;
import static org.assertj.core.api.Assertions.assertThat;

public class TheBigTest {
	@Test
	public void fetch_the_big_stuff() throws Exception {
		Jira jira = new Jira();
		jira.open();
		try (Jira ignored = jira) {
			assertThat(
				jira.fetchIssues()
					.doOnNext(i -> System.out.println("Fetched " + i.getKey() + " lead time: " + i.getLeadTime().toString()))
					.map(Issue::getLeadTime)
					.filter(Optional::isPresent)
					.map(Optional::get)
					.reduce(new long[] { 0, 0 }, (sumAndCount, leadTime) -> new long[] { sumAndCount[0] + leadTime.toMillis(), sumAndCount[1] + 1 })
					.map(sumAndCount -> sumAndCount[0] / sumAndCount[1])
					.map(Duration::ofMillis)
					.test().await()
					.assertComplete()
					.values())
				.hasSize(1)
				.allMatch(averageLeadTime -> averageLeadTime.toDays() > 10);
		}
	}

	@Test
	public void save_the_big_stuff() throws Exception {
		Jira jira = new Jira();
		jira.open();
		IssueDB db = new IssueDB();
		db.open();
		try (Jira ignored = jira;
			 IssueDB ignored2 = db
		) {
			jira.fetchIssues()
				.doOnNext(i -> System.out.println("Saving " + i.getKey()))
				.doOnNext(db::save)
				.test().await()
				.assertComplete();
		}
	}

	@Test
	public void read_the_big_stuff() throws Exception {
		IssueDB db = new IssueDB();
		db.open();
		try (IssueDB ignored = db) {
			db.readAll()
				.forEach((Consumer<Issue>) i -> System.out.println("Read " + i.getKey() + " lead time: " + i.getLeadTime().toString()));
			System.out.println("DB contains " + db.count());
		}
	}
}