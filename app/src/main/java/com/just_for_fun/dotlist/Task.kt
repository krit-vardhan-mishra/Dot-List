package com.just_for_fun.dotlist

class Task {
    var id: Int = 0
    var title: String? = null
    var isCompleted: Boolean = false

    var details: TaskDetails? = null

    private var timestamp: Long = 0
    var position: Int = 0
    var content: String? = null

    constructor() {
        this.details = TaskDetails()
    }

    constructor(
        id: Int,
        title: String?,
        isCompleted: Boolean,
        details: TaskDetails?,
        timestamp: Long
    ) {
        this.id = id
        this.title = title
        this.isCompleted = isCompleted
        this.details = details ?: TaskDetails()
        this.timestamp = timestamp
    }
}
