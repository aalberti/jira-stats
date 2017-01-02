package aa;

import java.util.List;

public class PrimaIssueBuilder {
	private String key;
	private List<PrimaIssueTransition> transitions;

	private PrimaIssueBuilder() {}

	public static PrimaIssueBuilder issue() { return new PrimaIssueBuilder(); }

	public PrimaIssueBuilder withKey(String key) {
		this.key = key;
		return this;
	}

	public PrimaIssueBuilder withTransitions(List<PrimaIssueTransition> transitions) {
		this.transitions = transitions;
		return this;
	}

	public PrimaIssue build() {
		return new PrimaIssue(key, transitions);
	}
}