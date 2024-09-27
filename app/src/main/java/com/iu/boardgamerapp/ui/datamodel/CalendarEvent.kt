package com.iu.boardgamerapp.ui.datamodel

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.Serializable

@Serializable
data class CalendarEvent(
    @DocumentId
    val id: String? = "",  // Standardwert für ID
    val title: String = "",  // Standardwert für title
    @PropertyName("start_time") val startTime: Long = 0L,  // Standardwert für startTime
    @PropertyName("end_time") val endTime: Long = 0L,  // Standardwert für endTime
    val location: String = ""  // Standardwert für location
)