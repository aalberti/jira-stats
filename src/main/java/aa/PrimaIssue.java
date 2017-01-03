package aa;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static java.util.Comparator.comparing;

public class PrimaIssue {
	private String key;
	private List<PrimaIssueTransition> transitions;
	private Instant creationDate;

	private PrimaIssue(String key, Instant creationDate, List<PrimaIssueTransition> transitions) {
		this.key = key;
		this.creationDate = creationDate;
		this.transitions = transitions;
	}

	public String getKey() { return key; }

	public List<PrimaIssueTransition> getTransitions() { return transitions; }

	public Optional<PrimaIssueTransition> getLastTransitionToStatus(String status) {
		return transitions.stream()
			.filter(t -> "status".equals(t.getField()))
			.filter(t -> status.equals(t.getTo()))
			.sorted(comparing(PrimaIssueTransition::getAt).reversed())
			.findFirst();
	}

	public Optional<Duration> getLeadTime() {
		return getLastTransitionToStatus("Closed")
			.map(PrimaIssueTransition::getAt)
			.map(i -> Duration.between(getCreationDate(), i));
	}

	public Instant getCreationDate() {
		return creationDate;
	}

	public static class Builder {
		private String key;
		private Instant creationDate;
		private List<PrimaIssueTransition> transitions;

		private Builder() {}

		public static Builder issue() { return new Builder(); }

		public Builder withKey(String key) {
			this.key = key;
			return this;
		}

		public Builder withTransitions(List<PrimaIssueTransition> transitions) {
			this.transitions = transitions;
			return this;
		}

		public Builder withCreationDate(Instant creationDate) {
			this.creationDate = creationDate;
			return this;
		}

		public PrimaIssue build() {
			return new PrimaIssue(key, creationDate, transitions);
		}
	}
}
