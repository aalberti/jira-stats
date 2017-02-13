package aa

import com.gmongo.GMongo
import com.google.gson.Gson

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class Capacity {
    static void main(String[] args) {
        def db = new GMongo().getDB('jira_stats')
        def sprints = db.getCollection('sprints').find().toArray()
        def gson = new Gson()
        db.getCollection('issues').find()
                .collect { gson.fromJson(it.json as String, Issue) }
                .findAll { it.project == 'PRIN' }
                .findAll { ['Bug', 'Story'].contains(it.type) }
                .findAll { issueClosedInASprint(it, sprints) }
                .groupBy { it.closureDate.map { toMonday(it) }.orElse(null) }
                .sort()
                .each { println "${it.key}=[${it.value.collect { it.key }}]" }
    }

    private static LocalDateTime toMonday(Instant instant) {
        LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                .with(DayOfWeek.MONDAY)
                .truncatedTo(ChronoUnit.DAYS)
    }

    private static boolean issueClosedInASprint(Issue issue, sprints) {
        sprints
                .findAll { issue.sprints.contains(it.name) }
                .any { issueClosedInSprint(issue, it) }
    }

    private static boolean issueClosedInSprint(Issue issue, sprint) {
        if (sprint.state != 'closed')
            return false
        issue.closureDate.map {
            it.isBefore(parse(sprint.end)) && it.isAfter(parse(sprint.start))
        }.orElse(false)
    }

    private static Instant parse(string) {
        Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(string))
    }
}
