package com.iu.boardgamerapp.ui.datamodel

import com.google.firebase.Timestamp

data class CalendarEvent(
    var id: String = "", // Default value to satisfy no-arg constructor
    var title: String = "",
    var location: String = "",
    var startTime: Timestamp = Timestamp.now(),
    var endTime: Timestamp = Timestamp.now()
)