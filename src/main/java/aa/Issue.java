package aa;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static java.util.Comparator.comparing;

public class Issue {
	private String key;
	private List<Transition> history;
	private Instant creationDate;
	private String project;

	private Issue(String key, String project, Instant creationDate, List<Transition> history) {
		this.key = key;
		this.project = project;
		this.creationDate = creationDate;
		this.history = history;
	}

	public String getKey() { return key; }

	public List<Transition> getHistory() { return history; }

	public Optional<Duration> getLeadTime() {
		return getClosureDate()
			.map(i -> Duration.between(getCreationDate(), i));
	}

	public Optional<Instant> getClosureDate() {
		return history.stream()
			.filter(t -> "status".equals(t.getField()))
			.filter(t -> "Closed".equals(t.getTo()))
			.sorted(comparing(Transition::getAt).reversed())
			.findFirst()
			.map(Transition::getAt);
	}

	public Instant getCreationDate() {
		return creationDate;
	}

	public String getProject() {
		return project;
	}

	public static class Builder {
		private String key;
		private String project;
		private Instant creationDate;
		private List<Transition> history;

		private Builder() {}

		public static Builder issue() { return new Builder(); }

		public Builder withKey(String key) {
			this.key = key;
			return this;
		}

		public Builder withProject(String project) {
			this.project = project;
			return this;
		}

		public Builder withHistory(List<Transition> history) {
			this.history = history;
			return this;
		}

		public Builder withCreationDate(Instant creationDate) {
			this.creationDate = creationDate;
			return this;
		}

		public Issue build() {
			return new Issue(key, project, creationDate, history);
		}
	}
}
