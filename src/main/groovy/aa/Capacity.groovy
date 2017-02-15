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
        def project = args.length > 0 ? args[0] : 'PRIN'
        def db = new GMongo().getDB('jira_stats')
        def sprints = db.getCollection('sprints').find().toArray()
        def gson = new Gson()
        def closedPerWeek = db.getCollection('issues').find()
                .collect { gson.fromJson(it.json as String, Issue) }
                .findAll { it.project == project }
                .findAll { ['Bug', 'Story'].contains(it.type) }
                .findAll { issueClosedDuringASprint(it, sprints) }
                .groupBy { it.closureDate.map { toMonday(it) }.orElse(null) }
                .sort()
        println 'Closed tickets per week'
        closedPerWeek
                .each { println "${it.key}=[${it.value.collect { it.key }}]" }
        def openBugsPerWeek = db.getCollection('issues').find()
                .collect { gson.fromJson(it.json as String, Issue) }
                .findAll { it.project == project }
                .findAll { it.type == 'Bug' }
                .findAll { bugAddedDuringASprint(it, sprints) }
                .groupBy { additionToSprintInstant(it, sprints).map { toMonday(it) }.orElse(null) }
                .sort()
        println 'Added bugs per week'
        openBugsPerWeek
                .each { println "${it.key}=[${it.value.collect { it.key }}]" }

        println "$project Stats"
        def stats = openBugsPerWeek.collect { date, openBugs ->
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
        stats.each { println it }
        new File("capacity-${project}.csv").withPrintWriter { out ->
            out.println 'date,open,close,capacity'
            stats.each { out.println "${it.date},${it.open},${it.closed},${it.capacity}" }
        }
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

    private static boolean bugAddedDuringASprint(Issue issue, List sprints) {
        additionToSprintInstant(issue, sprints).present
    }

    private static Optional<Instant> additionToSprintInstant(Issue issue, List sprints) {
        def sprintTransitions = issue.history
                .findAll { it.field == 'Sprint' }
        if (!sprintTransitions) {
            if (issue.sprints) {
                def sprint = sprints.find { it.name == issue.sprints[0] }
                if (isDuringSprint(issue.creationDate, sprint))
                    return Optional.of(issue.creationDate)
            }
            return Optional.empty()
        }
        def firstSprintTransition = sprintTransitions
                .toSorted { a, b -> a.at <=> b.at }
                .first()
        if (firstSprintTransition.source.isPresent()) {
            def sprint = sprints.find { it.name == firstSprintTransition.source.get() }
            if (sprint && isDuringSprint(issue.creationDate, sprint))
                return Optional.of(firstSprintTransition.at)
        }
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

    private static Instant parse(String string) {
        Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(string))
    }
}
