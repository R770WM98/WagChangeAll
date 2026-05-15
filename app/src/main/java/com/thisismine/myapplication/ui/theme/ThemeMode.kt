package com.thisismine.myapplication.ui.theme

enum class ThemeMode {
    System,
    Light,
    Dark;

    fun displayLabel(): String = when (this) {
        System -> "System"
        Light -> "Light"
        Dark -> "Dark"
    }

    fun next(): ThemeMode = when (this) {
        System -> Light
        Light -> Dark
        Dark -> System
    }

    fun resolveDarkTheme(systemIsDark: Boolean): Boolean = when (this) {
        System -> systemIsDark
        Light -> false
        Dark -> true
    }

    companion object {
        fun fromStored(value: String?): ThemeMode {
            return entries.firstOrNull { it.name == value } ?: System
        }
    }
}

