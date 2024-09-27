package com.iu.boardgamerapp.ui.datamodel

data class User(
    val id: Int,         // ID des Benutzers
    val name: String,    // Name des Benutzers
    val isHost: Boolean  // Status, ob der Benutzer Gastgeber ist oder nicht
)