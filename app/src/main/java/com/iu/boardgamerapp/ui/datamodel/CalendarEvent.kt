package com.iu.boardgamerapp.ui.datamodel

data class CalendarEvent(
    val title: String,         // Titel des Ereignisses
    val location: String,      // Ort des Ereignisses
    val startTime: Long,       // Startzeit des Ereignisses
    val endTime: Long          // Endzeit des Ereignisses
)