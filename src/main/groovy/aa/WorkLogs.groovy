package aa

import aa.jira.Authentication
import groovyx.net.http.HTTPBuilder

class WorkLogs {
    static void main(String[] args) {
        def project = args.length > 0 ? args[0] : "PRIN"
        def start = new Date().parse("yyyy-MM-dd", "2016-09-01")
        def worklogs = (start..new Date()).step(7)
                .collect { fetchWorklogs(it, it + 7, project) }
        def types = worklogs.collectMany { it.type }.unique()
        def header = (['date'] + types).join(',')
        new File("workLogs-${project}.csv").withPrintWriter { out ->
            out.println header
            worklogs
                    .groupBy { it.date }
                    .collect { ([it.key[0]] + flattenLogByType(it.value[0], types)) }
                    .collect { it.join(',') }
                    .forEach { out.println it }
        }
    }

    private static List<Integer> flattenLogByType(List<Map> logByType, List<String> types) {
        types.collect { extractLogForType(logByType, it) }
    }

    private static int extractLogForType(List<Map> logByType, String type) {
        def log = logByType
                .findAll { type == it.type }
                .collect { it.timeLogged }
        if (log instanceof List) {
            if (log.isEmpty())
                return 0
            else
                return log[0] as int
        }
        return log as int
    }

    private static Object fetchWorklogs(Date start, Date end, String project) {
        println "fetching $project worklogs from $start to $end"
        Authentication authentication = Authentication.load()
        def tempo = new HTTPBuilder('https://applications.prima-solutions.com/jira/rest/tempo-timesheets/3/')
        String basicAuth = "${authentication.user}:${authentication.password}".toString().bytes.encodeBase64()
        def res = null
        tempo.get(
                path: 'worklogs',
                headers: ["Authorization": "Basic $basicAuth"],
                query: [
                        projectKey: project,
                        dateFrom  : start.format("yyyy-MM-dd"),
                        dateTo    : end.format("yyyy-MM-dd"),
                ])
                { resp, json ->
                    res = json.collect { log ->
                        [
                                type      : log.worklogAttributes.find { it.key == '_TaskType_' }['value'],
                                timeLogged: log.timeSpentSeconds,
                        ]
                    }
                    .groupBy { it.type }
                            .collect {
                        [
                                type      : it.key,
                                date      : start.format("yyyy-MM-dd"),
                                timeLogged: it.value.inject(0, { a, v -> a + v.timeLogged })
                        ]
                    }
                }
        return res
    }
}
