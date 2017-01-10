package aa;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

import org.bson.Document;
import org.junit.Test;

import aa.jira.JiraConnection;
import com.google.gson.Gson;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.UpdateOptions;
import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;

public class TheBigTest {
	@Test
	public void fetch_the_big_stuff() throws Exception {
		JiraConnection jiraConnection = new JiraConnection();
		jiraConnection.open();
		try (JiraConnection ignored = jiraConnection) {
			assertThat(
				jiraConnection.fetchIssues()
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
	public void save_the_big_stuff() throws Exception {
		JiraConnection jiraConnection = new JiraConnection();
		jiraConnection.open();
		IssueDB db = new IssueDB();
		db.open();
		try (JiraConnection ignored = jiraConnection;
			 IssueDB ignored2 = db
		) {
			jiraConnection.fetchIssues()
				.doOnNext(i -> System.out.println("Saving " + i.getKey()))
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
				.forEach((Consumer<Issue>) i -> System.out.println("Read " + i.getKey() + " lead time: " + i.getLeadTime().toString()));
			System.out.println("DB contains " + db.count());
		}
	}

	private static class IssueDB implements Closeable {
		private MongoClient mongoClient = null;
		private final Gson gson;
		private MongoCollection<Document> collection;

		public IssueDB() {
			this.gson = new Gson();
		}

		public void open() {
			this.mongoClient = new MongoClient();
			MongoDatabase database = this.mongoClient.getDatabase("jira-stats");
			collection = database.getCollection("issues");
		}

		@Override
		public void close() throws IOException {
			if (mongoClient != null) {
				mongoClient.close();
			}
		}

		public void save(Issue issue) {
			SerializedIssue si = new SerializedIssue(issue, gson.toJson(issue));
			collection.replaceOne(
				eq("key", si.issue.getKey()),
				new Document("key", si.issue.getKey())
					.append("json", si.json),
				new UpdateOptions().upsert(true));
		}

		public MongoIterable<Issue> readAll() {
			return collection.find()
				.map(d -> gson.fromJson(d.getString("json"), Issue.class));
		}

		public long count() {
			return collection.count();
		}

		private class SerializedIssue {
			private Issue issue;
			private String json;

			SerializedIssue(Issue issue, String json) {
				this.issue = issue;
				this.json = json;
			}
		}
	}
}