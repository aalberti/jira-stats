package aa.jira;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.mockito.Mockito;

import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.atlassian.jira.rest.client.api.domain.BasicUser;
import com.atlassian.jira.rest.client.api.domain.ChangelogGroup;
import com.atlassian.jira.rest.client.api.domain.ChangelogItem;
import com.atlassian.jira.rest.client.api.domain.Issue;
import io.reactivex.Observable;
import static com.atlassian.util.concurrent.Promises.promise;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class JiraConnectionMock {
	private JiraConnection connection = Mockito.mock(JiraConnection.class);

	public JiraConnection getConnection() {
		return connection;
	}

	public IssueMocker issue() {
		return new IssueMocker();
	}

	public class IssueMocker {
		private final List<ChangelogGroup> changelog = new ArrayList<>();
		private DateTime creationDate = new DateTime("2016-06-06");
		private String key = "ANY_KEY";

		public IssueMocker withKey(String key) {
			this.key = key;
			return this;
		}

		public IssueMocker withCreationDate(DateTime creationDate) {
			this.creationDate = creationDate;
			return this;
		}

		public IssueMocker withClosureAtDate(DateTime date) {
			ChangelogItem changelogItem = new ChangelogItem(null, "status", null, null, null, "Closed");
			changelog.add(new ChangelogGroup(new BasicUser(null, "jdoe", "Jane Doe"), date,
				singletonList(changelogItem)));
			return this;
		}

		public void mock() {
			Issue issue = new Issue(null, null, key, 42L,
				new BasicProject(null, "FOO", 12L, null), null, null, null, null,
				null,
				null, null, null, creationDate, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
				null, changelog, null, null);
			when(connection.fetchIssue(key)).thenReturn(promise(issue));
			when(connection.fetchIssues(anyString())).thenReturn(Observable.just(issue));
		}

	}
}
