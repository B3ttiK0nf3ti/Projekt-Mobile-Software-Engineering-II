package com.iu.boardgamerapp.ui.datamodel

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.Serializable

@Serializable
data class CalendarEvent(
    @DocumentId
    val id: String? = null,
    val title: String,
    @PropertyName("start_time") val startTime: Long,
    @PropertyName("end_time") val endTime: Long,
    val location: String
)