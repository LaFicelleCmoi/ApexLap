package com.apexlap.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.apexlap.MainActivity
import com.apexlap.domain.usecase.FindNextSessionUseCase
import com.apexlap.domain.usecase.UpcomingSession
import com.apexlap.domain.usecase.currentSeason
import com.apexlap.sync.BackgroundEntryPoint
import com.apexlap.ui.components.flagOf
import com.apexlap.ui.components.localDay
import com.apexlap.ui.components.localTime
import com.apexlap.ui.components.relativeStart
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.time.Instant

/** Home-screen widget: the next session of the season, read from the offline cache. */
class NextSessionWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val deps = EntryPointAccessors.fromApplication(context, BackgroundEntryPoint::class.java)
        val now = deps.clock().now()
        val calendar = deps.repository().observeCalendar(deps.clock().currentSeason()).first()
        val next = FindNextSessionUseCase()(calendar, now)
        provideContent { WidgetContent(next, now, hasCalendar = calendar.isNotEmpty()) }
    }
}

class NextSessionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextSessionWidget()
}

private val Asphalt = Color(0xFF0B0B10)
private val RacingRed = Color(0xFFE10600)
private val Floodlight = Color(0xFFF5F5F7)
private val Telemetry = Color(0xFF9A9AAD)

@Composable
private fun WidgetContent(next: UpcomingSession?, now: Instant, hasCalendar: Boolean) {
    Box(
        GlanceModifier
            .fillMaxSize()
            .background(Asphalt)
            .cornerRadius(20.dp)
            .clickable(actionStartActivity<MainActivity>())
            .padding(14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(GlanceModifier.width(4.dp).height(12.dp).background(RacingRed)) {}
                Spacer(GlanceModifier.width(6.dp))
                Text("PROCHAINE SÉANCE", style = TextStyle(color = ColorProvider(Floodlight), fontSize = 11.sp, fontWeight = FontWeight.Bold))
            }
            Spacer(GlanceModifier.height(6.dp))
            if (next == null) {
                Text(
                    if (hasCalendar) "Saison terminée" else "Ouvrez Apex Lap pour charger le calendrier",
                    style = TextStyle(color = ColorProvider(Telemetry), fontSize = 13.sp),
                )
            } else {
                val start = requireNotNull(next.session.startsAt)
                Text(
                    "${flagOf(next.race.circuit.country)}  ${next.race.name}",
                    style = TextStyle(color = ColorProvider(Floodlight), fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
                Text(
                    "${next.session.type.label} · ${start.localDay()} ${start.localTime()}",
                    style = TextStyle(color = ColorProvider(Telemetry), fontSize = 13.sp),
                    maxLines = 1,
                )
                Spacer(GlanceModifier.height(6.dp))
                Text(
                    relativeStart(now, start).replaceFirstChar { it.uppercase() },
                    style = TextStyle(color = ColorProvider(RacingRed), fontSize = 22.sp, fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}
