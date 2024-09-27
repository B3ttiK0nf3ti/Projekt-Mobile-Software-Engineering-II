package com.iu.boardgamerapp.ui.datamodel

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.Serializable

@Serializable
data class CalendarEvent(
    var id: String = "",
    var title: String = "",
    var startTime: Long = 0L,
    var endTime: Long = 0L,
    var location: String = ""
)