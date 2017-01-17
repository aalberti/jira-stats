package aa;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.util.Comparator.comparing;

public class Issue {
	private final String key;
	private final String project;
	private final String status;
	private final String type;
	private final String summary;
	private final String reporter;
	private final String assignee;
	private final Collection<String> fixVersions;
	private final Instant creationDate;
	private final Instant updateDate;
	private final List<Transition> history;

	private Issue(String key, String project, String status, String type, String summary, String reporter, String assignee, Collection<String> fixVersions,
		Instant creationDate, Instant updateDate, List<Transition> history) {
		this.key = key;
		this.project = project;
		this.status = status;
		this.type = type;
		this.summary = summary;
		this.reporter = reporter;
		this.assignee = assignee;
		this.fixVersions = fixVersions;
		this.creationDate = creationDate;
		this.updateDate = updateDate;
		this.history = history;
	}

	public String getKey() { return key; }

	public String getProject() {
		return project;
	}

	public String getStatus() {
		return status;
	}

	public String getType() {
		return type;
	}

	public Collection<String> getFixVersions() {
		return fixVersions;
	}

	public String getAssignee() {
		return assignee;
	}

	public String getReporter() {
		return reporter;
	}

	public String getSummary() {
		return summary;
	}

	public Instant getCreationDate() {
		return creationDate;
	}

	public Instant getUpdateDate() {
		return updateDate;
	}

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

	public static class Builder {
		private String key;
		private String project;
		private String status;
		private String type;
		private String summary;
		private String reporter;
		private String assignee;
		private Collection<String> fixVersions;
		private Instant creationDate;
		private Instant updateDate;
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

		public Builder withStatus(String status) {
			this.status = status;
			return this;
		}

		public Builder withType(String type) {
			this.type = type;
			return this;
		}

		public Builder withCreationDate(Instant creationDate) {
			this.creationDate = creationDate;
			return this;
		}

		public Builder withUpdateDate(Instant updateDate) {
			this.updateDate = updateDate;
			return this;
		}

		public Builder withSummary(String summary) {
			this.summary = summary;
			return this;
		}

		public Builder withReporter(String reporter) {
			this.reporter = reporter;
			return this;
		}

		public Builder withAssignee(String assignee) {
			this.assignee = assignee;
			return this;
		}

		public Builder withFixVersions(Collection<String> fixVersions) {
			this.fixVersions = fixVersions;
			return this;
		}

		public Builder withHistory(List<Transition> history) {
			this.history = history;
			return this;
		}

		public Issue build() {
			return new Issue(key, project, status, type, summary, reporter, assignee, fixVersions, creationDate, updateDate, history);
		}
	}
}
