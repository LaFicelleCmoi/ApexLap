package com.apexlap.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.apexlap.ui.calendar.CalendarScreen
import com.apexlap.ui.constructor.ConstructorScreen
import com.apexlap.ui.driver.DriverScreen
import com.apexlap.ui.race.RaceScreen
import com.apexlap.ui.reminders.RemindersScreen
import com.apexlap.ui.results.ResultsScreen
import com.apexlap.ui.standings.StandingsScreen
import com.apexlap.ui.stats.StatsScreen
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable data object CalendarRoute
@Serializable data object ResultsRoute
@Serializable data object StandingsRoute
@Serializable data object StatsRoute
@Serializable data class RaceRoute(val season: Int, val round: Int)
@Serializable data class DriverRoute(val driverId: String)
@Serializable data class ConstructorRoute(val constructorId: String)
@Serializable data object RemindersRoute

private data class TopLevel(val route: Any, val routeClass: KClass<*>, val label: String, val icon: ImageVector)

private val topLevels = listOf(
    TopLevel(CalendarRoute, CalendarRoute::class, "Calendrier", Icons.Outlined.Flag),
    TopLevel(ResultsRoute, ResultsRoute::class, "Résultats", Icons.Outlined.FormatListNumbered),
    TopLevel(StandingsRoute, StandingsRoute::class, "Classements", Icons.Outlined.EmojiEvents),
    TopLevel(StatsRoute, StatsRoute::class, "Stats", Icons.Outlined.Insights),
)

@Composable
fun ApexLapApp(pendingRace: RaceRoute? = null, onPendingRaceOpened: () -> Unit = {}) {
    val navController = rememberNavController()
    LaunchedEffect(pendingRace) {
        if (pendingRace != null) {
            navController.navigate(pendingRace)
            onPendingRaceOpened()
        }
    }
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                topLevels.forEach { item ->
                    val selected = destination?.hierarchy?.any { it.hasRoute(item.routeClass) } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = CalendarRoute, modifier = Modifier.padding(padding)) {
            composable<CalendarRoute> {
                CalendarScreen(
                    onRaceClick = { navController.navigate(RaceRoute(it.season, it.round)) },
                    onRemindersClick = { navController.navigate(RemindersRoute) },
                )
            }
            composable<ResultsRoute> {
                ResultsScreen(onRaceClick = { navController.navigate(RaceRoute(it.season, it.round)) })
            }
            composable<StatsRoute> {
                StatsScreen(onDriverClick = { navController.navigate(DriverRoute(it)) })
            }
            composable<StandingsRoute> {
                StandingsScreen(
                    onDriverClick = { navController.navigate(DriverRoute(it)) },
                    onConstructorClick = { navController.navigate(ConstructorRoute(it)) },
                )
            }
            composable<RemindersRoute> {
                RemindersScreen(onBack = navController::popBackStack)
            }
            composable<RaceRoute> {
                RaceScreen(
                    onBack = navController::popBackStack,
                    onDriverClick = { navController.navigate(DriverRoute(it)) },
                )
            }
            composable<DriverRoute> {
                DriverScreen(
                    onBack = navController::popBackStack,
                    onConstructorClick = { navController.navigate(ConstructorRoute(it)) },
                )
            }
            composable<ConstructorRoute> {
                ConstructorScreen(
                    onBack = navController::popBackStack,
                    onDriverClick = { navController.navigate(DriverRoute(it)) },
                )
            }
        }
    }
}
