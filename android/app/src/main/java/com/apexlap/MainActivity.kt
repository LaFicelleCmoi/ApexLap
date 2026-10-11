package com.apexlap

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.apexlap.ui.navigation.ApexLapApp
import com.apexlap.ui.navigation.RaceRoute
import com.apexlap.ui.theme.ApexLapTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Grand Prix to open, set when the app is launched from a session reminder. */
    private val pendingRace = mutableStateOf<RaceRoute?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) pendingRace.value = intent.raceRoute()
        setContent {
            ApexLapTheme {
                ApexLapApp(pendingRace = pendingRace.value, onPendingRaceOpened = { pendingRace.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.raceRoute()?.let { pendingRace.value = it }
    }

    private fun Intent.raceRoute(): RaceRoute? {
        val season = getIntExtra(EXTRA_SEASON, 0)
        val round = getIntExtra(EXTRA_ROUND, 0)
        return if (season > 0 && round > 0) RaceRoute(season, round) else null
    }

    companion object {
        private const val EXTRA_SEASON = "race_season"
        private const val EXTRA_ROUND = "race_round"

        fun raceIntent(context: Context, season: Int, round: Int): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_SEASON, season)
                .putExtra(EXTRA_ROUND, round)
    }
}
