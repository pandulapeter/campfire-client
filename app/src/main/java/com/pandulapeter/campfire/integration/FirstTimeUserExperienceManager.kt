package com.pandulapeter.campfire.integration

import com.pandulapeter.campfire.data.persistence.PreferenceDatabase
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class FirstTimeUserExperienceManager(private val preferenceDatabase: PreferenceDatabase) {

    var manageDownloadsCompleted by Delegates.observable(preferenceDatabase.ftuxManageDownloadsCompleted) { _: KProperty<*>, old: Boolean, new: Boolean ->
        if (old != new) {
            preferenceDatabase.ftuxManageDownloadsCompleted = new
        }
    }

    fun resetAll() {
        manageDownloadsCompleted = false
    }
}