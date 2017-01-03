package aa;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

public class JiraStatistics {
	public static void main(String[] args) throws URISyntaxException, ExecutionException, InterruptedException, IOException {
		get("PRIN-3047");
	}

	public static Issue get(String issueKey) throws URISyntaxException, IOException, InterruptedException, ExecutionException {
		JiraConnection jiraConnection = new JiraConnection();
		jiraConnection.open();
		try (JiraConnection ignored = jiraConnection) {
			return jiraConnection.getIssue(issueKey).get();
		}
	}
}
