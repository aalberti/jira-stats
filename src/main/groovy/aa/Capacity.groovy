package aa

import com.gmongo.GMongo

class Capacity {
    static void main(String[] args) {
        def db = new GMongo().getDB('jira_stats')
        println 'sprints'
        db.getCollection('sprints').find()
                .each { println it }
        println 'issues'
        db.getCollection('issues').find()
                .each { println it }
        println 'globals'
        db.getCollection('globals').find()
                .each { println it }
    }
}
