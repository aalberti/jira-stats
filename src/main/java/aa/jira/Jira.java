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
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
		.ofPattern("\"yyyy-MM-dd HH:mm\"")
		.withZone(ZoneId.of("Europe/Paris"));
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

	public Observable<Issue> fetchIssuesUpdatedSince(Instant since) {
		return connection.fetchIssues("updated >= " + format(since)).map(mapper::toIssue);
	}

	private String format(Instant instant) {
		return TIME_FORMATTER.format(instant);
	}
}