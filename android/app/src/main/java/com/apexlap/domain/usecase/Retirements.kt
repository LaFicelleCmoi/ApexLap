package com.apexlap.domain.usecase

import com.apexlap.i18n.AppLanguage
import com.apexlap.i18n.L

enum class RetirementKind {
    RETIRED,
    /** Retired but still classified (90 % of the race distance covered), marked "*". */
    CLASSIFIED,
    /** Not classified, marked "NC". */
    NOT_CLASSIFIED,
}

data class Retirement(val grandPrix: String, val kind: RetirementKind = RetirementKind.RETIRED) {
    val label: String get() = label(AppLanguage.FR)

    fun label(language: AppLanguage): String {
        val name = grandPrixNames[grandPrix]?.get(language) ?: grandPrix
        return when (kind) {
            RetirementKind.RETIRED -> name
            RetirementKind.CLASSIFIED -> "$name*"
            RetirementKind.NOT_CLASSIFIED -> "$name (NC)"
        }
    }
}

/** The record names Grands Prix by their French country (or city) name. */
private val grandPrixNames = mapOf(
    "Australie" to L("Australie", "Australia", "Australia"),
    "Chine" to L("Chine", "China", "China"),
    "Japon" to L("Japon", "Japan", "Japón"),
    "Bahreïn" to L("Bahreïn", "Bahrain", "Baréin"),
    "Miami" to L("Miami", "Miami", "Miami"),
    "Canada" to L("Canada", "Canada", "Canadá"),
    "Monaco" to L("Monaco", "Monaco", "Mónaco"),
    "Barcelone" to L("Barcelone", "Barcelona", "Barcelona"),
    "Autriche" to L("Autriche", "Austria", "Austria"),
    "Grande-Bretagne" to L("Grande-Bretagne", "Great Britain", "Gran Bretaña"),
    "Belgique" to L("Belgique", "Belgium", "Bélgica"),
    "Hongrie" to L("Hongrie", "Hungary", "Hungría"),
    "Pays-Bas" to L("Pays-Bas", "Netherlands", "Países Bajos"),
    "Italie" to L("Italie", "Italy", "Italia"),
    "Espagne" to L("Espagne", "Spain", "España"),
    "Azerbaïdjan" to L("Azerbaïdjan", "Azerbaijan", "Azerbaiyán"),
    "Singapour" to L("Singapour", "Singapore", "Singapur"),
)

fun grandPrixName(french: String, language: AppLanguage): String = grandPrixNames[french]?.get(language) ?: french

data class DriverRetirements(val driver: String, val retirements: List<Retirement>) {
    val count: Int get() = retirements.size
}

data class NonStarts(val driver: String, val grandsPrix: List<String>)

data class SeasonRetirements(
    val season: Int,
    /** Most retirements first. */
    val drivers: List<DriverRetirements>,
    val noRetirement: List<String>,
    val nonStarters: List<NonStarts>,
) {
    val total: Int get() = drivers.sumOf { it.count }
}

/** Hand-kept retirement record, by driver. */
object SeasonRetirementsRecord {
    fun of(season: Int): SeasonRetirements? = if (season == 2026) season2026 else null

    /** "Australie (NC), Chine, Azerbaïdjan*" → retirements. */
    internal fun parse(spec: String): List<Retirement> = spec.split(",").map { it.trim() }.map { item ->
        when {
            item.endsWith("(NC)") -> Retirement(item.removeSuffix("(NC)").trim(), RetirementKind.NOT_CLASSIFIED)
            item.endsWith("*") -> Retirement(item.removeSuffix("*").trim(), RetirementKind.CLASSIFIED)
            else -> Retirement(item)
        }
    }

    private fun d(driver: String, spec: String) = DriverRetirements(driver, parse(spec))

    private val season2026 = SeasonRetirements(
        season = 2026,
        drivers = listOf(
            d("Stroll", "Australie (NC), Chine, Japon, Monaco, Barcelone, Autriche, Belgique, Pays-Bas, Italie, Espagne, Azerbaïdjan, Singapour"),
            d("Bottas", "Australie, Monaco, Barcelone, Autriche, Hongrie, Pays-Bas, Azerbaïdjan*, Bahreïn"),
            d("Albon", "Canada, Barcelone (NC), Grande-Bretagne, Pays-Bas*, Azerbaïdjan, Bahreïn, Singapour*"),
            d("Alonso", "Australie, Chine, Canada, Barcelone, Italie, Azerbaïdjan"),
            d("Pérez", "Canada, Autriche, Belgique, Hongrie, Espagne"),
            d("Verstappen", "Chine, Monaco, Grande-Bretagne*, Pays-Bas"),
            d("Bearman", "Japon, Monaco, Barcelone*, Pays-Bas"),
            d("Hülkenberg", "Miami, Barcelone, Grande-Bretagne"),
            d("Norris", "Canada, Monaco, Azerbaïdjan"),
            d("Russell", "Canada, Belgique, Bahreïn*"),
            d("Leclerc", "Monaco, Barcelone*, Italie"),
            d("Sainz", "Monaco*, Autriche, Espagne"),
            d("Hadjar", "Australie, Miami"),
            d("Gasly", "Miami, Azerbaïdjan"),
            d("Colapinto", "Azerbaïdjan, Singapour"),
            d("Antonelli", "Barcelone*"),
            d("Piastri", "Hongrie"),
            d("Lawson", "Miami"),
            d("Ocon", "Pays-Bas"),
            d("Hamilton", "Espagne"),
            d("Bortoleto", "Singapour"),
        ),
        noRetirement = listOf("Lindblad", "Tsunoda"),
        nonStarters = listOf(
            NonStarts("Piastri", listOf("Australie", "Chine")),
            NonStarts("Hülkenberg", listOf("Australie")),
            NonStarts("Norris", listOf("Chine")),
            NonStarts("Bortoleto", listOf("Chine")),
            NonStarts("Albon", listOf("Chine")),
            NonStarts("Lindblad", listOf("Canada")),
        ),
    )
}
