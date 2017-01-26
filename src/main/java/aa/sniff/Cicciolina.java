package aa.sniff;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import aa.Issue;
import aa.Transition;
import aa.db.IssueDB;
import aa.jira.Jira;
import static aa.jira.Jira.updatedSince;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Comparator.naturalOrder;

public class Cicciolina implements Closeable {
	private Jira jira;
	private IssueDB db;

	public static void main(String[] args) throws Exception {
		Cicciolina cicciolina = new Cicciolina();
		cicciolina.init();
		try (Cicciolina ignored = cicciolina) {
			cicciolina.fetchAll();
		}
	}

	private void init() {
		jira = new Jira();
		jira.open();
		db = new IssueDB();
		db.open();
	}

	private void fetchAll() throws IOException, InterruptedException {
		Instant start = now();
		Instant until;
		do {
			until = fetchOnce(jira, db);
		} while (until.isBefore(now()));
		System.out.println("=> All done in " + Duration.between(start, now()).toString());
	}

	private Instant fetchOnce(Jira jira, IssueDB db) throws InterruptedException {
		Instant since = db.getNextBatchStart().orElse(Instant.parse("2016-01-01T00:00:00Z"));
		Instant until = since.plus(30, DAYS);
		System.out.println("Batch from " + since.toString() + " to " + until.toString() + " starting at " + now());
		jira.fetchIssues(updatedSince(since).updatedUntil(until))
			.doOnNext(
				i -> System.out.println("  - Fetched " + toString(i)
					+ (i.getUpdateDate().isBefore(since) || i.getUpdateDate().isAfter(until)? " OUT OF INTERVAL" : "")
				)
			)
			.doOnNext(db::save)
			.test().await();
		if (until.isBefore(now()))
			db.saveNextBatchStart(until);
		else
			db.saveNextBatchStart(now().minus(1, DAYS));
		System.out.println("Batch from " + since.toString() + " to " + until.toString() + " done at " + now());
		System.out.println();
		return until;
	}

	private String toString(Issue issue) {
		Instant issueUpdateDate = issue.getUpdateDate();
		Optional<Instant> lastUpdateDate = issue.getHistory().stream().map(Transition::getAt).max(naturalOrder());
		return issue.getKey()
			+ " lead time: " + issue.getLeadTime().toString()
			+ " updated: " + issueUpdateDate.toString()
			+ " last transition: " + lastUpdateDate.toString()
			+ (lastUpdateDate.isPresent() ? (lastUpdateDate.get().equals(issueUpdateDate) ? "" : " DIFFERENCE") : "");
	}

	@Override
	public void close() throws IOException {
		jira.close();
		db.close();
	}
}
