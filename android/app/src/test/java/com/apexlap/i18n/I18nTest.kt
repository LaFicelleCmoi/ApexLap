package com.apexlap.i18n

import com.apexlap.domain.model.SessionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.Locale

class I18nTest {

    @Test
    fun `phone language picks a supported language, English otherwise`() {
        assertEquals(AppLanguage.FR, AppLanguage.fromSystem(Locale.FRANCE))
        assertEquals(AppLanguage.ES, AppLanguage.fromSystem(Locale.forLanguageTag("es-MX")))
        assertEquals(AppLanguage.EN, AppLanguage.fromSystem(Locale.GERMANY))
        assertEquals(AppLanguage.ES, LanguagePreference.ES.resolve())
    }

    @Test
    fun `the chosen language switches every string`() {
        I18n.apply(LanguagePreference.EN)
        assertEquals("Race", I18n.strings.session(SessionType.RACE))
        I18n.apply(LanguagePreference.ES)
        assertEquals("Carrera", I18n.strings.session(SessionType.RACE))
        I18n.apply(LanguagePreference.FR)
        assertEquals("Course", I18n.strings.session(SessionType.RACE))
        assertEquals("dans 3 h 05", I18n.strings.inHours(3, 5))
    }

    @Test
    fun `plurals and ordinals follow each language`() {
        assertEquals("1 victoire", I18n.stringsOf(AppLanguage.FR).wins(1))
        assertEquals("2 wins", I18n.stringsOf(AppLanguage.EN).wins(2))
        assertEquals("3 victorias", I18n.stringsOf(AppLanguage.ES).wins(3))
        assertEquals("22nd", I18n.stringsOf(AppLanguage.EN).ordinal(22))
        assertEquals("11th", I18n.stringsOf(AppLanguage.EN).ordinal(11))
        assertNotEquals(I18n.stringsOf(AppLanguage.EN).settings, I18n.stringsOf(AppLanguage.ES).settings)
    }
}
