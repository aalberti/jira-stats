package aa.jira;

import java.io.Closeable;
import java.io.IOException;

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

	public Promise<Issue> fetchIssue(String issueKey) {
		return connection.fetchIssue(issueKey).map(mapper::toIssue);
	}

	@Override
	public void close() throws IOException {
		connection.close();
	}

	public Observable<Issue> fetchIssues() {
		return connection.fetchIssues().map(mapper::toIssue);
	}
}