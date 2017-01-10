package aa.db;

import java.io.Closeable;
import java.io.IOException;

import org.bson.Document;

import aa.Issue;
import com.google.gson.Gson;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.UpdateOptions;
import static com.mongodb.client.model.Filters.eq;

public class IssueDB implements Closeable {
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
