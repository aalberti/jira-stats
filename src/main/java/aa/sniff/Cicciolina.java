package aa.sniff;

import java.time.Instant;

import aa.Transition;
import aa.db.IssueDB;
import aa.jira.Jira;
import static aa.jira.Jira.updatedSince;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Comparator.naturalOrder;

public class Cicciolina {
	public static void main(String[] args) throws Exception {
		Jira jira = new Jira();
		jira.open();
		IssueDB db = new IssueDB();
		db.open();
		try (Jira ignored = jira;
			 IssueDB ignored2 = db) {
			Instant until;
			do {
				until = fetchOnce(jira, db);
			}
			while (until.isBefore(now()));
		}
	}

	private static Instant fetchOnce(Jira jira, IssueDB db) throws InterruptedException {
		Instant since = db.getNextBatchStart().orElse(Instant.parse("2016-01-01T00:00:00Z"));
		Instant until = since.plus(1, DAYS);
		db.startBatch(until);
		System.out.println("\n|>>> Batch from " + since.toString() + " to " + until.toString() + " Starting at " + now());
		jira.fetchIssues(updatedSince(since).updatedUntil(until))
			.doOnNext(
				i -> System.out.println("--> Fetched " + i.getKey()
					+ " lead time: " + i.getLeadTime().toString()
					+ " updated " + i.getHistory().stream().map(Transition::getAt).max(naturalOrder()).toString()))
			.doOnNext(db::save)
			.test().await();
		db.batchDone();
		System.out.println("\n|>>> Batch done at " + now());
		return until;
	}
}
