package aa;

import org.joda.time.DateTime;

public class PrimaIssueTransition {
	private DateTime date;
	private String field;
	private String to;

	PrimaIssueTransition(DateTime date, String field, String to) {
		this.date = date;
		this.field = field;
		this.to = to;
	}

	public DateTime getDate() {
		return date;
	}

	public String getField() {
		return field;
	}

	public String getTo() {
		return to;
	}

	public static class Builder {
		private DateTime date;
		private String field;
		private String to;

		private Builder() {}

		public static Builder transition() { return new Builder(); }

		public Builder withDate(DateTime date) {
			this.date = date;
			return this;
		}

		public Builder withField(String field) {
			this.field = field;
			return this;
		}

		public Builder withTarget(String to) {
			this.to = to;
			return this;
		}

		public PrimaIssueTransition build() {
			return new PrimaIssueTransition(date, field, to);
		}
	}
}
