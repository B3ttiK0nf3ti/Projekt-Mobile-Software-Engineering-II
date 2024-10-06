package com.iu.boardgamerapp.ui.datamodel

data class User(
    val firebaseInstallationId: String = "", // Standardwert hinzufügen
    val name: String = "",                    // Standardwert hinzufügen
    val isHost: Boolean = false                // Standardwert hinzufügen
)
{
    // Füge einen leeren Konstruktor hinzu, falls nicht vorhanden
    constructor() : this("", "", false)
}