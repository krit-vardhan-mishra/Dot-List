package com.just_for_fun.dotlist

class TaskDetails {
    var notes: String? = null
    var filePath: String? = null

    constructor()

    constructor(notes: String?, filePath: String?) {
        this.notes = notes
        this.filePath = filePath
    }

    override fun toString(): String {
        return "Notes: $notes, File Path: $filePath"
    }
}
