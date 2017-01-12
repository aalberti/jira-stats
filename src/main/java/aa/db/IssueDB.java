package aa.db;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Consumer;

import org.bson.Document;

import aa.Issue;
import com.google.gson.Gson;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import io.reactivex.Observable;
import static com.mongodb.client.model.Filters.eq;

public class IssueDB implements Closeable {
	private final Gson gson;
	private com.mongodb.MongoClient mongoClient;
	private com.mongodb.client.MongoCollection<Document> collection;

	public IssueDB() {
		this.gson = new Gson();
	}

	public void open() {
		mongoClient = new com.mongodb.MongoClient();
		collection = mongoClient
			.getDatabase("jira_stats")
			.getCollection("issues");
	}

	@Override
	public void close() throws IOException {
		if (mongoClient != null) {
			mongoClient.close();
		}
		mongoClient = null;
	}

	public void save(Issue issue) {
		SerializedIssue si = new SerializedIssue(issue, gson.toJson(issue));
		UpdateResult result = collection.replaceOne(
			eq("key", si.issue.getKey()),
			new Document("key", si.issue.getKey())
				.append("json", si.json),
			new UpdateOptions().upsert(true));
		System.out.println("upserted: " + result.getUpsertedId() + ", matched: " + result.getMatchedCount() + ", modified: " + result.getModifiedCount());
	}

	public Observable<Issue> readAll() {
		return Observable.<Document>create(subscriber -> {
			if (subscriber.isDisposed())
				return;
			try {
				collection.find().forEach((Consumer<? super Document>) subscriber::onNext);
			}
			catch (Exception e) {
				subscriber.onError(e);
			}
			if (subscriber.isDisposed())
				return;
			subscriber.onComplete();
		}).map(d -> gson.fromJson(d.getString("json"), Issue.class));
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
