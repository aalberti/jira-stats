package aa.db;

import java.io.IOException;
import java.time.Instant;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import aa.Issue;
import static aa.Issue.Builder.issue;
import static aa.Transition.Builder.transition;
import static java.lang.Thread.sleep;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class IssueDBTest {
	private IssueDB db;

	@Before
	public void setUp() throws Exception {
		db = createDB();
	}

	@After
	public void tearDown() throws Exception {
		db.drop();
		db.close();
	}

	@Test
	public void saveAndRead() throws Exception {
		Instant closureDate = now().minus(12, HOURS);
		db.save(issueClosingAt(closureDate));
		db.readAll().test().await()
			.assertComplete()
			.assertValueCount(1)
			.assertValue(i -> closureDate.equals(i.getClosureDate().get()));
	}

	@Test
	public void overwrite() throws Exception {
		Instant lastClosureDate = now().minus(12, HOURS);
		db.save(issueClosingAt(lastClosureDate.minus(1, HOURS)));
		db.save(issueClosingAt(lastClosureDate));
		db.readAll().test().await()
			.assertComplete()
			.assertValueCount(1)
			.assertValue(i -> lastClosureDate.equals(i.getClosureDate().get()));
	}

	@Test
	public void drop() throws Exception {
		db.save(issueClosingAt(now().minus(12, HOURS)));
		db.drop();
		db.readAll().test().await()
			.assertComplete()
			.assertValueCount(0);
	}

	@Test
	public void updateInstant() throws Exception {
		Instant before = now();
		db.startBatch();
		db.save(anyIssue());
		db.batchDone();
		Instant after = now();
		assertThat(nextBatchStart()).isBetween(before, after);
	}

	@Test
	public void updateInstant_notUpdated_when_batchNotDone() throws Exception {
		db.startBatch();
		db.save(anyIssue());
		db.batchDone();
		sleep(1);
		Instant after = now();
		db.startBatch();
		assertThat(nextBatchStart()).isLessThan(after);
	}

	@Test
	public void updateInstant_notUpdated_when_noSave() throws Exception {
		db.startBatch();
		db.save(anyIssue());
		db.batchDone();
		sleep(1);
		Instant after = now();
		db.startBatch();
		db.batchDone();
		assertThat(nextBatchStart()).isLessThan(after);
	}

	@Test
	public void updateInstant_isLastStart() throws Exception {
		db.startBatch();
		sleep(1);
		Instant before = now();
		db.startBatch();
		db.save(anyIssue());
		db.batchDone();
		assertThat(nextBatchStart()).isGreaterThanOrEqualTo(before);
	}

	private Instant nextBatchStart() throws IOException {
		try (IssueDB anotherDB = createDB()) {
			return anotherDB.getNextBatchStart().get();
		}
	}

	private IssueDB createDB() {
		IssueDB db = new IssueDB("IssueDBTest");
		db.open();
		return db;
	}

	private Issue anyIssue() {
		return issueClosingAt(now());
	}

	private Issue issueClosingAt(Instant closureDate) {
		return issue()
			.withKey("KEY")
			.withHistory(
				singletonList(transition()
					.withAt(closureDate)
					.withField("status")
					.withTarget("Closed")
					.build()
				)
			).build();
	}
}