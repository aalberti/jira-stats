package aa.db;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;

import org.bson.Document;

import aa.Issue;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import io.reactivex.Observable;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static java.time.Instant.now;

public class IssueDB implements Closeable {
	private final Gson gson;
	private final String name;
	private MongoClient mongoClient;
	private MongoCollection<Document> issues;
	private MongoCollection<Document> globals;
	private Instant lastUpdateInstant;

	public IssueDB() {
		this("jira_stats");
	}

	@VisibleForTesting
	IssueDB(String name) {
		this.gson = new Gson();
		this.name = name;
	}

	public void open() {
		mongoClient = new MongoClient();
		MongoDatabase db = mongoClient
			.getDatabase(name);
		issues = db.getCollection("issues");
		globals = db.getCollection("globals");
	}

	@Override
	public void close() throws IOException {
		if (mongoClient != null) {
			mongoClient.close();
		}
		mongoClient = null;
	}

	public void drop() {
		mongoClient.dropDatabase(name);
	}

	public void startBatch() {
		startBatch(now());
	}

	public void startBatch(Instant batchEnd) {
		lastUpdateInstant = batchEnd;
	}

	public void save(Issue issue) {
		SerializedIssue si = new SerializedIssue(issue, gson.toJson(issue));
		UpdateResult result = issues.replaceOne(
			eq("key", si.issue.getKey()),
			new Document("key", si.issue.getKey())
				.append("json", si.json),
			new UpdateOptions().upsert(true));
	}

	public void batchDone() {
		globals.replaceOne(
			exists("_id"),
			new Document("lastUpdateInstant", lastUpdateInstant.toString()),
			new UpdateOptions().upsert(true));
	}

	public Optional<Instant> getLastUpdateInstant() {
		return Optional.ofNullable(globals
			.find(exists("_id"))
			.first())
			.map(doc -> doc.getString("lastUpdateInstant"))
			.map(Instant::parse);
	}

	public Observable<Issue> readAll() {
		return Observable.<Document>create(subscriber -> {
			if (subscriber.isDisposed())
				return;
			try {
				issues.find().forEach((Consumer<? super Document>) subscriber::onNext);
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
