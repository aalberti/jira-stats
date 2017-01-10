package aa;

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
import com.mongodb.client.model.UpdateOptions;
import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;

public class TheBigTest {
	@Test
	public void read_the_big_stuff() throws Exception {
		Gson gson = new Gson();
		try (MongoClient mongoClient = new MongoClient()) {
			MongoDatabase database = mongoClient.getDatabase("local");
			MongoCollection<Document> jiraTestCollection = database.getCollection("jiraTest");
			jiraTestCollection.find()
				.map(d -> gson.fromJson(d.getString("json"), Issue.class))
				.forEach((Consumer<Issue>) i -> System.out.println("Read " + i.getKey() + " lead time: " + i.getLeadTime().toString()));
			System.out.println("DB contains " + jiraTestCollection.count());
		}
	}

	@Test
	public void save_the_big_stuff() throws Exception {
		Gson gson = new Gson();
		JiraConnection jiraConnection = new JiraConnection();
		jiraConnection.open();
		try (JiraConnection ignored = jiraConnection;
			 MongoClient mongoClient = new MongoClient()
		) {
			MongoDatabase database = mongoClient.getDatabase("local");
			MongoCollection<Document> jiraTestCollection = database.getCollection("jiraTest");
			jiraConnection.fetchIssues()
				.map(i -> new SerializedIssue(i, gson.toJson(i)))
				.doOnNext(si -> System.out.println("Serialized " + si.json))
				.doOnNext(si -> jiraTestCollection.replaceOne(
					eq("key", si.issue.getKey()),
					new Document("key", si.issue.getKey())
						.append("json", si.json),
					new UpdateOptions().upsert(true))
				)
				.test().await()
				.assertComplete();
		}
	}

	private class SerializedIssue {
		private Issue issue;
		private String json;

		SerializedIssue(Issue issue, String json) {
			this.issue = issue;
			this.json = json;
		}
	}

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
}