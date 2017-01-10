package aa.db;

import java.io.Closeable;
import java.io.IOException;

import org.bson.Document;

import aa.Issue;
import com.google.gson.Gson;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import io.reactivex.Observable;
import io.reactivex.subscribers.TestSubscriber;
import static com.mongodb.client.model.Filters.eq;

public class IssueDB implements Closeable {
	private MongoClient mongoClient = null;
	private final Gson gson;
	private MongoCollection<Document> collection;

	public IssueDB() {
		this.gson = new Gson();
	}

	public void open() {
		this.mongoClient = MongoClients.create();
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

	public Observable<Issue> readAll() {
		return Observable.<Document>create(subscriber -> collection.find().subscribe(new TestSubscriber<Document>() {
			@Override
			public void onNext(Document document) {
				subscriber.onNext(document);
			}

			@Override
			public void onError(Throwable t) {
				subscriber.onError(t);
			}

			@Override
			public void onComplete() {
				subscriber.onComplete();
			}
		})).map(d -> gson.fromJson(d.getString("json"), Issue.class));
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
