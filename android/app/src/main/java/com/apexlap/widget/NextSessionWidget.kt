package com.apexlap.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.apexlap.MainActivity
import com.apexlap.domain.model.Race
import com.apexlap.domain.usecase.currentSeason
import com.apexlap.i18n.I18n
import com.apexlap.i18n.Strings
import com.apexlap.sync.BackgroundEntryPoint
import com.apexlap.ui.components.flagOf
import com.apexlap.ui.components.formatPoints
import com.apexlap.ui.components.localDay
import com.apexlap.ui.components.localTime
import com.apexlap.ui.theme.TeamColors
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

/**
 * Home-screen widget, read from the offline cache. Three layouts depending on its size:
 * a countdown, the next session with the weekend's progress, or the full schedule with the user's favourites.
 */
class NextSessionWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val deps = EntryPointAccessors.fromApplication(context, BackgroundEntryPoint::class.java)
        val now = deps.clock().now()
        val season = deps.clock().currentSeason()
        val calendar = deps.repository().observeCalendar(season).first()
        val settings = deps.settings().settings.first()
        val favorites = if (settings.favoriteDriverId != null || settings.favoriteConstructorId != null) {
            val drivers = deps.repository().observeDriverStandings(season).first()
            val constructors = deps.repository().observeConstructorStandings(season).first()
            WidgetFavorites(
                driver = drivers.firstOrNull { it.driver.id == settings.favoriteDriverId },
                constructor = constructors.firstOrNull { it.constructor.id == settings.favoriteConstructorId },
                leader = drivers.minByOrNull { it.position },
            )
        } else {
            null
        }
        val state = buildWidgetState(calendar, now, favorites)
        // Keeps the countdown honest: the next redraw comes sooner as the session gets closer.
        WidgetRefreshWorker.schedule(context, nextRefreshDelay(state))

        val accent = TeamColors.of(settings.favoriteConstructorId).takeIf { settings.favoriteConstructorId != null } ?: RacingRed
        val open: Action = state.race
            ?.let { actionStartActivity(MainActivity.raceIntent(context, it.season, it.round)) }
            ?: actionStartActivity<MainActivity>()
        provideContent { WidgetContent(state, accent, open, I18n.strings) }
    }

    companion object {
        private val SMALL = DpSize(120.dp, 90.dp)
        private val MEDIUM = DpSize(250.dp, 110.dp)
        private val LARGE = DpSize(250.dp, 250.dp)
    }
}

class NextSessionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextSessionWidget()

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetRefreshWorker.cancel(context)
    }
}

private val Asphalt = Color(0xFF0B0B10)
private val Carbon = Color(0xFF17171F)
private val RacingRed = Color(0xFFE10600)
private val Floodlight = Color(0xFFF5F5F7)
private val Telemetry = Color(0xFF9A9AAD)
private val Dim = Color(0xFF55556A)

private fun style(color: Color, size: TextUnit, bold: Boolean = false, align: TextAlign = TextAlign.Start) =
    TextStyle(color = ColorProvider(color), fontSize = size, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, textAlign = align)

@Composable
private fun WidgetContent(state: WidgetState, accent: Color, open: Action, strings: Strings) {
    val size = LocalSize.current
    Box(
        GlanceModifier
            .fillMaxSize()
            .background(Asphalt)
            .cornerRadius(20.dp)
            .clickable(open)
            .padding(12.dp),
    ) {
        val race = state.race
        when {
            race == null -> Column {
                Header(strings.nextSession, accent)
                Spacer(GlanceModifier.height(6.dp))
                Text(if (state.hasCalendar) strings.seasonOver else strings.openAppToLoad, style = style(Telemetry, 13.sp), maxLines = 2)
            }
            size.width < 200.dp -> Small(state, race, accent, strings)
            size.height < 200.dp -> Column { Medium(state, race, accent, strings) }
            else -> Column {
                Medium(state, race, accent, strings)
                Spacer(GlanceModifier.height(10.dp))
                Schedule(state, accent, strings)
                state.favorites?.let {
                    Spacer(GlanceModifier.height(10.dp))
                    Favorites(it, strings)
                }
            }
        }
    }
}

@Composable
private fun Header(title: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(GlanceModifier.width(4.dp).height(12.dp).background(accent)) {}
        Spacer(GlanceModifier.width(6.dp))
        Text(title, style = style(Floodlight, 11.sp, bold = true), maxLines = 1)
    }
}

/** 2×1: flag, the next (or live) session and a big countdown. */
@Composable
private fun Small(state: WidgetState, race: Race, accent: Color, strings: Strings) {
    Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Header("${flagOf(race.circuit.country)} ${strings.roundShort(race.round)}", accent)
        Spacer(GlanceModifier.height(4.dp))
        val live = state.live
        if (live != null) {
            Text("● ${strings.liveNow}", style = style(RacingRed, 16.sp, bold = true), maxLines = 1)
            Text(strings.session(live.type), style = style(Telemetry, 12.sp), maxLines = 1)
        } else {
            val next = state.next
            Text(next?.let { strings.sessionShort(it.type) } ?: "", style = style(Telemetry, 12.sp), maxLines = 1)
            Text(state.minutesToNext?.let(strings::compactDuration) ?: "—", style = style(accent, 22.sp, bold = true), maxLines = 1)
        }
    }
}

/** 4×2: weekend title, next session with its time, countdown, and the weekend's sessions as chips. */
@Composable
private fun ColumnScope.Medium(state: WidgetState, race: Race, accent: Color, strings: Strings) {
    Header("${strings.round(race.round)} · ${flagOf(race.circuit.country)}", accent)
    Spacer(GlanceModifier.height(4.dp))
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(GlanceModifier.defaultWeight()) {
            Text(race.name, style = style(Floodlight, 16.sp, bold = true), maxLines = 1)
            val shown = state.live ?: state.next?.takeIf { state.nextRace?.round == race.round }
            val start = shown?.startsAt
            if (shown != null && start != null) {
                Text(
                    "${strings.session(shown.type)} · ${start.localDay()} ${start.localTime()}",
                    style = style(Telemetry, 12.sp),
                    maxLines = 1,
                )
            }
        }
        Spacer(GlanceModifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            if (state.live != null) {
                Text("● ${strings.liveNow}", style = style(RacingRed, 15.sp, bold = true, align = TextAlign.End), maxLines = 1)
            } else {
                Text(strings.startsInLabel, style = style(Telemetry, 9.sp, bold = true, align = TextAlign.End), maxLines = 1)
                Text(
                    state.minutesToNext?.let(strings::compactDuration) ?: "—",
                    style = style(accent, 20.sp, bold = true, align = TextAlign.End),
                    maxLines = 1,
                )
            }
        }
    }
    Spacer(GlanceModifier.height(8.dp))
    Row(GlanceModifier.fillMaxWidth()) {
        state.sessions.forEachIndexed { index, (session, phase) ->
            if (index > 0) Spacer(GlanceModifier.width(4.dp))
            val (background, text) = when (phase) {
                SessionPhase.DONE -> Carbon to Dim
                SessionPhase.LIVE -> RacingRed to Floodlight
                SessionPhase.NEXT -> accent to Asphalt
                SessionPhase.LATER -> Carbon to Floodlight
            }
            Box(
                GlanceModifier.defaultWeight().background(background).cornerRadius(6.dp).padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    (if (phase == SessionPhase.DONE) "✓ " else "") + strings.sessionShort(session.type),
                    style = style(text, 10.sp, bold = true, align = TextAlign.Center),
                    maxLines = 1,
                )
            }
        }
    }
}

/** Full weekend schedule in local time, the current or next session highlighted. */
@Composable
private fun Schedule(state: WidgetState, accent: Color, strings: Strings) {
    Text(strings.weekendSchedule, style = style(Telemetry, 10.sp, bold = true), maxLines = 1)
    state.sessions.forEach { (session, phase) ->
        val color = when (phase) {
            SessionPhase.DONE -> Dim
            SessionPhase.LIVE -> RacingRed
            SessionPhase.NEXT -> accent
            SessionPhase.LATER -> Floodlight
        }
        Row(GlanceModifier.fillMaxWidth().padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(strings.session(session.type), style = style(color, 12.sp, bold = phase == SessionPhase.NEXT || phase == SessionPhase.LIVE),
                maxLines = 1, modifier = GlanceModifier.defaultWeight())
            val start = session.startsAt
            Text(
                start?.let { "${it.localDay()} ${it.localTime()}" } ?: session.date.toString(),
                style = style(color, 12.sp, align = TextAlign.End),
                maxLines = 1,
            )
        }
    }
}

/** The followed driver and team with their championship position. */
@Composable
private fun Favorites(favorites: WidgetFavorites, strings: Strings) {
    Text(strings.myFavorites.uppercase(), style = style(Telemetry, 10.sp, bold = true), maxLines = 1)
    favorites.driver?.let { d ->
        val leader = favorites.leader
        val gap = if (leader != null && leader.driver.id != d.driver.id) " · " + strings.pointsBehind((leader.points - d.points).formatPoints()) else ""
        FavoriteLine(TeamColors.of(d.constructor?.id), d.driver.familyName, "P${d.position} · ${d.points.formatPoints()} ${strings.pts}$gap")
    }
    favorites.constructor?.let { c ->
        FavoriteLine(TeamColors.of(c.constructor.id), c.constructor.name, "P${c.position} · ${c.points.formatPoints()} ${strings.pts}")
    }
}

@Composable
private fun FavoriteLine(color: Color, name: String, detail: String) {
    Row(GlanceModifier.fillMaxWidth().padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(GlanceModifier.width(3.dp).height(14.dp).background(color)) {}
        Spacer(GlanceModifier.width(6.dp))
        Text(name, style = style(Floodlight, 12.sp, bold = true), maxLines = 1)
        Spacer(GlanceModifier.width(6.dp))
        Text(detail, style = style(Telemetry, 12.sp), maxLines = 1)
    }
}
