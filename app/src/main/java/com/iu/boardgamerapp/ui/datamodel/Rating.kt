package com.iu.boardgamerapp.ui.datamodel

data class Rating (
    val hostName: String = "",  // Name des Gastgebers
    val hostRating: Int = 0,    // Bewertung für den Gastgeber (0-5)
    val foodRating: Int = 0,     // Bewertung für das Essen (0-5)
    val eveningRating: Int = 0   // Bewertung für den Abend (0-5)
)