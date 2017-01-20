package aa;

import java.time.Instant;
import java.util.Optional;

import static java.util.Optional.ofNullable;

public class Transition {
	private Instant at;
	private String field;
	private String source;
	private String target;

	Transition(Instant at, String field, String source, String target) {
		this.at = at;
		this.field = field;
		this.source = source;
		this.target = target;
	}

	public Instant getAt() {
		return at;
	}

	public String getField() {
		return field;
	}

	public Optional<String> getSource() {
		return ofNullable(source);
	}

	public Optional<String> getTarget() {
		return ofNullable(target);
	}

	public static class Builder {
		private Instant at;
		private String field;
		private String from;
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

		public Builder withSource(String from) {
			this.from = from;
			return this;
		}

		public Builder withTarget(String to) {
			this.to = to;
			return this;
		}

		public Transition build() {
			return new Transition(at, field, from, to);
		}
	}
}
