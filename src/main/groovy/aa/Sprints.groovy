package aa

import aa.jira.Authentication
import com.gmongo.GMongo
import com.mongodb.client.model.DBCollectionUpdateOptions
import groovyx.net.http.HTTPBuilder

import static groovy.json.JsonOutput.prettyPrint
import static groovy.json.JsonOutput.toJson

class Sprints {
    static void main(String[] args) {
        Authentication authentication = Authentication.load()
        def agileJira = new HTTPBuilder('https://applications.prima-solutions.com/jira/rest/agile/1.0/')
        String basicAuth = "${authentication.user}:${authentication.password}".toString().bytes.encodeBase64()
        List sprints = null
        agileJira.get(
                path: 'board/103/sprint',
                headers: ["Authorization": "Basic $basicAuth"])
                { resp, json ->
                    sprints = json.values.collect {[
                            name: it.name,
                            start: it.startDate,
                            end: it.endDate,
                            state: it.state,
                    ]}
                }
        println "Saving:"
        println "${prettyPrint(toJson(sprints))}"

        def sprintsCollection = new GMongo().getDB('jira_stats').getCollection('sprints')
        println "Instead of"
        sprintsCollection.find().each {
            println it
        }

        sprints.forEach {
            sprintsCollection.update([name: it.name], it, new DBCollectionUpdateOptions().upsert(true))
        }
    }
}
