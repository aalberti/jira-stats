package aa;

import java.time.Instant;

public class Transition {
	private Instant at;
	private String field;
	private String to;

	Transition(Instant at, String field, String to) {
		this.at = at;
		this.field = field;
		this.to = to;
	}

	public Instant getAt() {
		return at;
	}

	public String getField() {
		return field;
	}

	public String getTo() {
		return to;
	}

	public static class Builder {
		private Instant at;
		private String field;
		private String to;

		private Builder() {}

		public static Builder transition() { return new Builder(); }

		public Builder withAt(Instant at) {
			this.at = at;
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

		public Transition build() {
			return new Transition(at, field, to);
		}
	}
}
