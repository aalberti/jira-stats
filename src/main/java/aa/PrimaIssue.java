package aa;

import java.util.List;
import java.util.Optional;

import static java.util.Comparator.comparing;

public class PrimaIssue {
	private String key;
	private List<PrimaIssueTransition> transitions;

	private PrimaIssue(String key, List<PrimaIssueTransition> transitions) {
		this.key = key;
		this.transitions = transitions;
	}

	public String getKey() { return key; }

	public List<PrimaIssueTransition> getTransitions() { return transitions; }

	public Optional<PrimaIssueTransition> getLastTransitionToStatus(String status) {
		return transitions.stream()
			.filter(t -> "status".equals(t.getField()))
			.filter(t -> status.equals(t.getTo()))
			.sorted(comparing(PrimaIssueTransition::getDate).reversed())
			.findFirst();
	}

	public static class Builder {
		private String key;
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

		public PrimaIssue build() {
			return new PrimaIssue(key, transitions);
		}
	}
}
