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
}
