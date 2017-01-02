package aa;

import org.joda.time.DateTime;

public class PrimaIssueTransitionBuilder {
	private DateTime date;
	private String field;
	private String to;

	private PrimaIssueTransitionBuilder() {}

	public static PrimaIssueTransitionBuilder transition() { return new PrimaIssueTransitionBuilder(); }

	public PrimaIssueTransitionBuilder withDate(DateTime date) {
		this.date = date;
		return this;
	}

	public PrimaIssueTransitionBuilder withField(String field) {
		this.field = field;
		return this;
	}

	public PrimaIssueTransitionBuilder withTarget(String to) {
		this.to = to;
		return this;
	}

	public PrimaIssueTransition createPrimaIssueTransition() {
		return new PrimaIssueTransition(date, field, to);
	}
}