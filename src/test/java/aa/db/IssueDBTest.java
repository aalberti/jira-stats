package aa.db;

import java.time.Instant;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import aa.Issue;
import static aa.Issue.Builder.issue;
import static aa.Transition.Builder.transition;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.singletonList;

public class IssueDBTest {
	private IssueDB db;

	@Before
	public void setUp() throws Exception {
		db = new IssueDB("IssueDBTest");
		db.open();
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