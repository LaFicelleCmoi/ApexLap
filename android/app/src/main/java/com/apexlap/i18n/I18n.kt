package com.apexlap.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class AppLanguage(val tag: String, val nativeName: String) {
    FR("fr", "Français"),
    EN("en", "English"),
    ES("es", "Español");

    val locale: Locale get() = Locale.forLanguageTag(tag)

    companion object {
        /** Language of the device, English when it is none of the supported ones. */
        fun fromSystem(locale: Locale = Locale.getDefault()): AppLanguage =
            entries.firstOrNull { it.tag == locale.language } ?: EN
    }
}

/** What the user picked in the settings: a language, or follow the device. */
enum class LanguagePreference(val language: AppLanguage?) {
    SYSTEM(null), FR(AppLanguage.FR), EN(AppLanguage.EN), ES(AppLanguage.ES);

    fun resolve(): AppLanguage = language ?: AppLanguage.fromSystem()
}

/** A piece of content written in the three languages. */
data class L(val fr: String, val en: String, val es: String) {
    operator fun get(language: AppLanguage): String = when (language) {
        AppLanguage.FR -> fr
        AppLanguage.EN -> en
        AppLanguage.ES -> es
    }
}

/**
 * Current language of the app. Compose reads it through [LocalStrings]; code outside composition
 * (widget, notifications, error messages, formatting) reads [strings].
 */
object I18n {
    private val current = MutableStateFlow(AppLanguage.fromSystem())
    val language: StateFlow<AppLanguage> = current.asStateFlow()

    private val cache = AppLanguage.entries.associateWith(::Strings)

    val strings: Strings get() = stringsOf(current.value)
    val locale: Locale get() = current.value.locale

    fun stringsOf(language: AppLanguage): Strings = cache.getValue(language)

    fun apply(preference: LanguagePreference) {
        current.value = preference.resolve()
    }
}

val LocalStrings = staticCompositionLocalOf { I18n.strings }
