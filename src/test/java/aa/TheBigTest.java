package aa;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import aa.db.IssueDB;
import aa.jira.Jira;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.reactivex.observables.GroupedObservable;
import static aa.jira.Jira.updatedSince;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("OptionalGetWithoutIsPresent")
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
	public void fetch_updated_since() throws Exception {
		Jira jira = new Jira();
		jira.open();
		try (Jira ignored = jira) {
			jira.fetchIssues(updatedSince(now().minus(20, MINUTES)))
				.doOnNext(i -> System.out.println("Fetched " + i.getKey() + " lead time: " + i.getLeadTime().toString()))
				.test().await()
				.assertComplete();
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
	public void save_twice() throws Exception {
		Jira jira = new Jira();
		jira.open();
		IssueDB db = new IssueDB();
		db.open();
		try (Jira ignored = jira;
			 IssueDB ignored2 = db) {
			System.out.println(">>> First batch");
			db.startBatch();
			jira.fetchIssues(updatedSince(now().minus(20, MINUTES)))
				.doOnNext(i -> System.out.println("Fetched " + i.getKey() + " lead time: " + i.getLeadTime().toString()))
				.doOnNext(db::save)
				.test().await()
				.assertComplete();
			db.batchDone();
			System.out.println(">>> Second batch");
			jira.fetchIssues(updatedSince(db.getNextBatchStart().get()))
				.doOnNext(i -> System.out.println("Fetched " + i.getKey() + " lead time: " + i.getLeadTime().toString()))
				.doOnNext(db::save)
				.test().await()
				.assertComplete();
		}
	}

	@Test
	public void save_since_previous() throws Exception {
		Jira jira = new Jira();
		jira.open();
		IssueDB db = new IssueDB();
		db.open();
		try (Jira ignored = jira;
			 IssueDB ignored2 = db) {
			System.out.println("Batch from" + db.getNextBatchStart());
			jira.fetchIssues(updatedSince(db.getNextBatchStart().get()))
				.doOnNext(i -> System.out.println("Fetched " + i.getKey() + " lead time: " + i.getLeadTime().toString()))
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
				.doOnNext(i -> System.out.println("Read " + i.getKey() + " lead time: " + i.getLeadTime().toString()))
				.count()
				.doAfterSuccess(count -> System.out.println("DB contains " + count))
				.blockingGet();
		}
	}

	@Test
	public void distribute_the_big_stuff() throws Exception {
		IssueDB db = new IssueDB();
		db.open();
		try (IssueDB ignored = db) {
			db.readAll()
				.filter(i -> i.getLeadTime().isPresent())
				.groupBy(i -> i.getLeadTime().get().toDays())
				.sorted(comparing(GroupedObservable::getKey))
				.doOnNext(delay -> {
					List<String> issueKeys = delay.map(Issue::getKey).toList().blockingGet();
					System.out.println("" + delay.getKey() + ": " + issueKeys.size() + " " + issueKeys.stream().collect(joining(", ", "(", ")")));
				})
				.test().await();
		}
	}

	@Test
	public void printOne() throws Exception {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		IssueDB db = new IssueDB();
		db.open();
		try (IssueDB ignored = db) {
			System.out.println(db.readAll()
				.filter(i -> "ASC-1650".equals(i.getKey()))
				.map(gson::toJson)
				.blockingSingle());
		}
	}
}