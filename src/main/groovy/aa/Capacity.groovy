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
        def closedPerWeek = db.getCollection('issues').find()
                .collect { gson.fromJson(it.json as String, Issue) }
                .findAll { it.project == 'PRIN' }
                .findAll { ['Bug', 'Story'].contains(it.type) }
                .findAll { issueClosedDuringASprint(it, sprints) }
                .groupBy { it.closureDate.map { toMonday(it) }.orElse(null) }
                .sort()
        println 'Closed tickets per week'
        closedPerWeek
                .each { println "${it.key}=[${it.value.collect { it.key }}]" }
        def openBugsPerWeek = db.getCollection('issues').find()
                .collect { gson.fromJson(it.json as String, Issue) }
                .findAll { it.project == 'PRIN' }
                .findAll { it.type == 'Bug' }
                .findAll { bugAddedDuringASprint(it, sprints) }
                .groupBy { additionToSprintInstant(it, sprints).map { toMonday(it) }.orElse(null) }
                .sort()
        println 'Added bugs per week'
        openBugsPerWeek
                .each { println "${it.key}=[${it.value.collect { it.key }}]" }

        println 'Stats'
        println openBugsPerWeek.collect { date, openBugs ->
            def closedIssues = closedPerWeek[date]
            def closed = closedIssues ? closedIssues.size() : 0
            def open = openBugs.size()
            [
                    date    : date,
                    open    : open,
                    closed  : closed,
                    capacity: closed - open
            ]
        }
    }

    private static Optional<Instant> additionToSprintInstant(Issue issue, sprints) {
        def sprintTransitions = issue.history
                .findAll { it.field == 'Sprint' }
        def res = [sprintTransitions, sprints]
                .combinations()
                .findAll { Transition transition, sprint -> isAddedSprint(transition, sprint) && isDuringSprint(transition.at, sprint) }
                .collect { Transition transition, sprint -> transition.at }
                .sort()
        if (res.isEmpty())
            return Optional.empty()
        else
            return Optional.of(res[0])
    }

    private static boolean issueClosedDuringASprint(Issue issue, sprints) {
        sprints
                .findAll { issue.sprints.contains(it.name) }
                .any { issueClosedDuringSprint(issue, it) }
    }

    private static boolean issueClosedDuringSprint(Issue issue, sprint) {
        if (sprint.state != 'closed')
            return false
        issue.closureDate.map { isDuringSprint(it, sprint) }.orElse(false)
    }

    private static boolean bugAddedDuringASprint(Issue issue, sprints) {
        def sprintTransitions = issue.history
                .findAll { it.field == 'Sprint' }
        if (!sprintTransitions)
            return false
        [sprintTransitions, sprints]
                .combinations()
                .any { Transition transition, sprint -> isAddedSprint(transition, sprint) && isDuringSprint(transition.at, sprint) }
    }

    private static boolean isAddedSprint(Transition transition, sprint) {
        transition.target.map { it.tokenize(':') }.map { it.contains(sprint.name) }.orElse(false)
    }

    private static LocalDateTime toMonday(Instant instant) {
        LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                .with(DayOfWeek.MONDAY)
                .truncatedTo(ChronoUnit.DAYS)
    }

    private static boolean isDuringSprint(Instant instant, sprint) {
        if (!sprint.start || !sprint.end)
            return false
        instant.isBefore(parse(sprint.end)) && instant.isAfter(parse(sprint.start))
    }

    private static Instant parse(string) {
        Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(string))
    }
}
