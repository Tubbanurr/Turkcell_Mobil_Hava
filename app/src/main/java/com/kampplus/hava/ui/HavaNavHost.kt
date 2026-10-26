package com.kampplus.hava.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kampplus.hava.R
import com.kampplus.hava.model.Place
import com.kampplus.hava.ui.city.CityRoute
import com.kampplus.hava.ui.forecast.ForecastRoute
import com.kampplus.hava.ui.forecast.ForecastViewModel
import com.kampplus.hava.ui.home.HomeRoute
import com.kampplus.hava.ui.settings.SettingsRoute

/**
 * Uygulamanın gezinti haritası: üç sekme (Bugün, Şehirler, Ayarlar) ve sekmelerin üstüne
 * açılan tahmin detayı.
 *
 * Navigation Compose her ekranı bir "rota" (metin adresi) ile tanımlar. Sekmeler
 * `navigate` ile değişir, geri tuşu yığından bir önceki ekrana döner. Detay ekranında alt
 * çubuk gizlenir ki tam ekran gökyüzü görünsün.
 *
 * Detay parametreleri yol parçası yerine sorgu parametresi (`forecast?name=...`) olarak
 * taşınır; çünkü değerler boş olabilir ve boş bir yol parçası rotayı eşleşmez yapar.
 */
@Composable
fun HavaNavHost(darkTheme: Boolean, modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = Tab.entries.any { tab -> destination?.hierarchy?.any { it.route == tab.route } == true }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBottomBar) {
                HavaBottomBar(
                    isSelected = { tab -> destination?.hierarchy?.any { it.route == tab.route } == true },
                    onSelect = { tab ->
                        navController.navigate(tab.route) {
                            // Sekmeler arası geçişte yığın şişmesin, durum korunsun.
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    darkTheme = darkTheme
                )
            }
        }
    ) { innerPadding ->
        // Ekranlar kendi gökyüzünü kenardan kenara çizer; alt çubuk payını liste kaydırmasına veririz.
        val bottomPadding = innerPadding.calculateBottomPadding()
        val openPlace = { place: Place -> navController.navigate(forecastRoute(place)) }

        Box {
            NavHost(navController = navController, startDestination = Tab.Home.route) {
                composable(Tab.Home.route) {
                    HomeRoute(darkTheme = darkTheme, bottomPadding = bottomPadding)
                }
                composable(Tab.Cities.route) {
                    CityRoute(onPlaceClick = openPlace, darkTheme = darkTheme, bottomPadding = bottomPadding)
                }
                composable(Tab.Settings.route) {
                    SettingsRoute(darkTheme = darkTheme, bottomPadding = bottomPadding)
                }
                composable(
                    route = FORECAST_ROUTE,
                    arguments = listOf(
                        // Hepsi String: koordinatlar ViewModel içinde Double'a çevrilir.
                        navArgument(ForecastViewModel.ARG_NAME) {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                        navArgument(ForecastViewModel.ARG_SUBTITLE) {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                        navArgument(ForecastViewModel.ARG_LATITUDE) {
                            type = NavType.StringType
                            defaultValue = "0"
                        },
                        navArgument(ForecastViewModel.ARG_LONGITUDE) {
                            type = NavType.StringType
                            defaultValue = "0"
                        }
                    )
                ) {
                    ForecastRoute(onBack = { navController.popBackStack() }, darkTheme = darkTheme)
                }
            }
        }
    }
}

private enum class Tab(val route: String, val icon: ImageVector, val labelRes: Int) {
    Home("home", Icons.Filled.Home, R.string.tab_today),
    Cities("cities", Icons.Filled.Search, R.string.tab_cities),
    Settings("settings", Icons.Filled.Settings, R.string.tab_settings)
}

@Composable
private fun HavaBottomBar(isSelected: (Tab) -> Boolean, onSelect: (Tab) -> Unit, darkTheme: Boolean) {
    // Yarı saydam koyu zemin: arkadaki gökyüzü hafifçe sızar, "cam" etkisi verir.
    NavigationBar(containerColor = Color.Black.copy(alpha = if (darkTheme) 0.55f else 0.35f), tonalElevation = 0.dp) {
        Tab.entries.forEach { tab ->
            NavigationBarItem(
                selected = isSelected(tab),
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.labelRes)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    indicatorColor = Color.White.copy(alpha = 0.25f),
                    unselectedIconColor = Color.White.copy(alpha = 0.7f),
                    unselectedTextColor = Color.White.copy(alpha = 0.7f)
                )
            )
        }
    }
}

/** Tahmin ekranının adres şablonu; süslü parantezler çalışma anında doldurulur. */
private const val FORECAST_ROUTE =
    "forecast?name={name}&subtitle={subtitle}&latitude={latitude}&longitude={longitude}"

/**
 * Bir yer için tahmin ekranının gerçek adresini üretir.
 *
 * `Uri.encode`: ad içindeki boşluk ve Türkçe karakterler adreste sorun çıkarmasın diye
 * kaçışlanır ("Afyonkarahisar, Türkiye" → "Afyonkarahisar%2C%20T%C3%BCrkiye").
 */
private fun forecastRoute(place: Place): String = buildString {
    append("forecast")
    append("?name=").append(Uri.encode(place.name))
    append("&subtitle=").append(Uri.encode(place.subtitle))
    append("&latitude=").append(place.latitude)
    append("&longitude=").append(place.longitude)
}
