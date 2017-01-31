package aa;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.Test;

import aa.db.IssueDB;
import aa.jira.Authentication;
import aa.jira.Jira;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.reactivex.observables.GroupedObservable;
import static aa.jira.Jira.updatedSince;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.*;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.*;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class TheBigTest {

	@Test
	public void fetch_the_big_stuff() throws Exception {
		Jira jira = new Jira();
		jira.open();
		try (Jira ignored = jira) {
			assertThat(
				jira.fetchIssues()
					.doOnNext(i -> System.out.println("Fetched " + i.getKey() + " lead time: " + i.getLeadTime().toString()))
					.map(Issue::getLeadTime)
					.filter(Optional::isPresent)
					.map(Optional::get)
					.reduce(new long[] { 0, 0 }, (sumAndCount, leadTime) -> new long[] { sumAndCount[0] + leadTime.toMillis(), sumAndCount[1] + 1 })
					.map(sumAndCount -> sumAndCount[0] / sumAndCount[1])
					.map(Duration::ofMillis)
					.test().await()
					.assertComplete()
					.values())
				.hasSize(1)
				.allMatch(averageLeadTime -> averageLeadTime.toDays() > 10);
		}
	}

	@Test
	public void fetch_updated_since() throws Exception {
		Jira jira = new Jira();
		jira.open();
		try (Jira ignored = jira) {
			jira.fetchIssues(updatedSince(now().minus(20, MINUTES)))
				.doOnNext(i -> System.out.println("Fetched " + i.getKey() + " lead time: " + i.getLeadTime().toString()))
				.test().await()
				.assertComplete();
		}
	}

	@Test
	public void save_the_big_stuff() throws Exception {
		Jira jira = new Jira();
		jira.open();
		IssueDB db = new IssueDB();
		db.open();
		try (Jira ignored = jira;
			 IssueDB ignored2 = db
		) {
			jira.fetchIssues()
				.doOnNext(i -> System.out.println("Saving " + i.getKey()))
				.doOnNext(db::save)
				.test().await()
				.assertComplete();
		}
	}

	@Test
	public void save_twice() throws Exception {
		Jira jira = new Jira();
		jira.open();
		IssueDB db = new IssueDB();
		db.open();
		try (Jira ignored = jira;
			 IssueDB ignored2 = db) {
			System.out.println(">>> First batch");
			Instant since = now().minus(20, MINUTES);
			jira.fetchIssues(updatedSince(since))
				.doOnNext(i -> System.out.println("Fetched " + i.getKey() + " lead time: " + i.getLeadTime().toString()))
				.doOnNext(db::save)
				.test().await()
				.assertComplete();
			db.saveNextBatchStart(since);
			System.out.println(">>> Second batch");
			jira.fetchIssues(updatedSince(db.getNextBatchStart().get()))
				.doOnNext(i -> System.out.println("Fetched " + i.getKey() + " lead time: " + i.getLeadTime().toString()))
				.doOnNext(db::save)
				.test().await()
				.assertComplete();
		}
	}

	@Test
	public void save_since_previous() throws Exception {
		Jira jira = new Jira();
		jira.open();
		IssueDB db = new IssueDB();
		db.open();
		try (Jira ignored = jira;
			 IssueDB ignored2 = db) {
			System.out.println("Batch from" + db.getNextBatchStart());
			jira.fetchIssues(updatedSince(db.getNextBatchStart().get()))
				.doOnNext(i -> System.out.println("Fetched " + i.getKey() + " lead time: " + i.getLeadTime().toString()))
				.doOnNext(db::save)
				.test().await()
				.assertComplete();
		}
	}

	@Test
	public void read_the_big_stuff() throws Exception {
		IssueDB db = new IssueDB();
		db.open();
		try (IssueDB ignored = db) {
			db.readAll()
				.doOnNext(i -> System.out.println("Read " + i.getKey() + " lead time: " + i.getLeadTime().toString()))
				.count()
				.doAfterSuccess(count -> System.out.println("DB contains " + count))
				.blockingGet();
		}
	}

	@Test
	public void distribute_leadTime() throws Exception {
		IssueDB db = new IssueDB();
		db.open();
		try (IssueDB ignored = db) {
			db.readAll()
				.filter(i -> "PRIN".equals(i.getProject()))
				.filter(i -> i.getLeadTime().isPresent())
				.groupBy(i -> i.getLeadTime().get().toDays())
				.sorted(comparing(GroupedObservable::getKey))
				.doOnNext(delay -> {
					List<String> issueKeys = delay.map(Issue::getKey).toList().blockingGet();
					System.out.println("" + delay.getKey() + ": " + issueKeys.size() + " "
						+ issueKeys.stream().collect(joining(", ", "(", ")")));
				})
				.test().await();
		}
	}

	@Test
	public void distribute_devTime() throws Exception {
		IssueDB db = new IssueDB();
		db.open();
		try (IssueDB ignored = db) {
			db.readAll()
				.filter(i -> "PRIN".equals(i.getProject()))
				.filter(i -> i.getDevTime().isPresent())
				.groupBy(i -> i.getDevTime().get().toDays())
				.sorted(comparing(GroupedObservable::getKey))
				.doOnNext(delay -> {
					List<String> issueKeys = delay.map(Issue::getKey).toList().blockingGet();
					System.out.println("" + delay.getKey() + ": " + issueKeys.size() + " "
						+ issueKeys.stream().collect(joining(", ", "(", ")")));
				})
				.test().await();
		}
	}

	@Test
	public void issues_not_originally_assigned_to_sprint() throws Exception {
		IssueDB db = new IssueDB();
		db.open();
		try (IssueDB ignored = db) {
			db.readAll()
				.filter(i -> "PRIN".equals(i.getProject()))
				.filter(i -> "PRIN-3000".compareTo(i.getKey()) < 0)
				.filter(i -> firstSprintAssignment(i)
					.map(sprintTime -> !sprintTime.equals(i.getCreationDate()))
					.orElse(false))
				.doOnNext(i -> System.out.println(i.getKey()
					+ " sprint assignment delay " + Duration.between(i.getCreationDate(), firstSprintAssignment(i).get()).toString()))
				.test().await();
		}
	}

	private Optional<Instant> firstSprintAssignment(Issue issue) {
		return issue.getHistory().stream()
			.filter(t -> "Sprint".equals(t.getField()))
			.filter(t -> !t.getSource().isPresent())
			.map(Transition::getAt)
			.sorted(reverseOrder())
			.findFirst();
	}

	@Test
	public void printOne() throws Exception {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		IssueDB db = new IssueDB();
		db.open();
		try (IssueDB ignored = db) {
			System.out.println(db.readAll()
				.filter(i -> "PRIN-2879".equals(i.getKey()))
				.map(gson::toJson)
				.blockingSingle());
		}
	}

	@Test
	public void workLog() throws Exception {
		LocalDate start = LocalDate.parse("2016-09-01");
		Period interval = Period.ofWeeks(1);
		Stream.iterate(start, d -> d.plus(interval)).limit(WEEKS.between(start, LocalDate.now()))
			.peek(s -> System.out.println("From " + s))
			.map(s -> workLog(s, s.plus(interval)))
			.map(this::toPercentage)
			.forEach(sums -> sums.entrySet()
				.forEach(l -> System.out.println("\t" + l.getKey() + ": " + l.getValue()))
			);
	}

	private Map<String, Duration> workLog(LocalDate start, LocalDate end) {
		Gson gson = new GsonBuilder().create();
		Authentication authentication = Authentication.load();
		HttpAuthenticationFeature httpAuthentication = HttpAuthenticationFeature.basic(authentication.getUser(), authentication.getPassword());
		DateTimeFormatter shortDateFormatter = DateTimeFormatter
			.ofPattern("yyyy-MM-dd")
			.withZone(ZoneId.of("Europe/Paris"));
		String startStr = shortDateFormatter.format(start);
		String endStr = shortDateFormatter.format(end);
		Response response = ClientBuilder.newClient().register(httpAuthentication)
			.target("https://applications.prima-solutions.com/jira/rest/tempo-timesheets/3")
			.path("worklogs")
			.queryParam("projectKey", "PRIN")
			.queryParam("dateFrom", startStr)
			.queryParam("dateTo", endStr)
			.request().get();
		List<WorkLog> workLogs = toWorkLogs(gson.fromJson(response.readEntity(String.class), Object.class));
		return workLogs.stream().collect(groupingBy(WorkLog::getType, summingTimes()));
	}

	private List<WorkLog> toWorkLogs(Object raw) {
		if (raw instanceof Map)
			return singletonList(toWorkLog(raw));
		else if (raw instanceof List) {
			@SuppressWarnings("unchecked")
			List<Object> rawLogs = (List) raw;
			return rawLogs.stream()
				.map(this::toWorkLog)
				.collect(toList());
		}
		else
			throw new IllegalArgumentException("What's that " + raw);
	}

	@SuppressWarnings("unchecked")
	private WorkLog toWorkLog(Object raw) {
		Map rawLog = (Map) raw;
		Duration time = Duration.of(((Double) rawLog.get("timeSpentSeconds")).longValue(), SECONDS);
		List workLogAttributes = (List) rawLog.get("worklogAttributes");
		String type = (String) workLogAttributes.stream()
			.filter(a -> "_TaskType_".equals(((Map) a).get("key")))
			.findFirst()
			.map(a -> ((Map) a).getOrDefault("value", ""))
			.orElse("");
		return new WorkLog(time, type);
	}

	private Map<String, Float> toPercentage(Map<String, Duration> logs) {
		long total = logs.entrySet().stream()
			.mapToLong(e -> e.getValue().getSeconds())
			.sum();
		return logs.entrySet().stream()
			.collect(toMap(Map.Entry::getKey, e -> (float) e.getValue().getSeconds() / total));
	}

	private static Collector<WorkLog, ?, Duration> summingTimes() {
		return new Collector<WorkLog, long[], Duration>() {
			@Override
			public Supplier<long[]> supplier() {
				return () -> new long[1];
			}

			@Override
			public BiConsumer<long[], WorkLog> accumulator() {
				return (a, l) -> a[0] += l.timeLogged.getSeconds();
			}

			@Override
			public BinaryOperator<long[]> combiner() {
				return (a, b) -> {
					a[0] += b[0];
					return a;
				};
			}

			@Override
			public Function<long[], Duration> finisher() {
				return a -> Duration.ofSeconds(a[0]);
			}

			@Override
			public Set<Characteristics> characteristics() {
				return EnumSet.of(Characteristics.UNORDERED);
			}
		};
	}

	private static class WorkLog {
		Duration timeLogged;
		String type;

		WorkLog(Duration timeLogged, String type) {
			this.timeLogged = timeLogged;
			this.type = type;
		}

		String getType() {
			return type;
		}
	}

	@Test
	public void secondsToInstant() throws Exception {
		System.out.println(Instant.ofEpochSecond(1464090827).toString());
	}
}