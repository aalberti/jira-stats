package aa.jira;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import aa.Issue;
import com.atlassian.util.concurrent.Promise;
import com.google.common.annotations.VisibleForTesting;
import io.reactivex.Observable;

public class Jira implements Closeable {
	private IssueMapper mapper;
	private JiraConnection connection;

	public Jira() {
		this(new JiraConnection());
	}

	@VisibleForTesting
	Jira(JiraConnection connection) {
		this.connection = connection;
		mapper = new IssueMapper();
	}

	public void open() {
		connection.open();
	}

	Promise<Issue> fetchIssue(String issueKey) {
		return connection.fetchIssue(issueKey).map(mapper::toIssue);
	}

	@Override
	public void close() throws IOException {
		connection.close();
	}

	public Observable<Issue> fetchIssues() {
		return connection.fetchIssues("project in (ppc,pcom)").map(mapper::toIssue);
	}

	public Observable<Issue> fetchIssues(Filter filter) {
		return connection
			.fetchIssues(filter.toJql())
			.map(mapper::toIssue);
	}

	public static Filter updatedSince(Instant since) {
		return new Filter().updatedSince(since);
	}

	private static class Filter {
		private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
			.ofPattern("\"yyyy-MM-dd HH:mm\"")
			.withZone(ZoneId.of("Europe/Paris"));
		private Instant updatedSince;

		private Filter updatedSince(Instant since) {
			this.updatedSince = since;
			return this;
		}

		private String toJql() {
			return "updated >= " + format(updatedSince);
		}

		private String format(Instant instant) {
			return TIME_FORMATTER.format(instant);
		}
	}
}