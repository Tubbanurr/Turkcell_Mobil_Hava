package com.kampplus.hava.ui.forecast

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kampplus.hava.R
import com.kampplus.hava.model.AirQuality
import com.kampplus.hava.model.CurrentWeather
import com.kampplus.hava.model.DailyForecast
import com.kampplus.hava.model.Forecast
import com.kampplus.hava.model.HourlyForecast
import com.kampplus.hava.model.Place
import com.kampplus.hava.model.TemperatureUnit
import com.kampplus.hava.model.WeatherCondition
import com.kampplus.hava.model.buildAdvice
import com.kampplus.hava.ui.LoadState
import com.kampplus.hava.ui.component.ErrorView
import com.kampplus.hava.ui.component.GlassCard
import com.kampplus.hava.ui.theme.HavaTheme
import com.kampplus.hava.ui.theme.LocalTemperatureUnit
import com.kampplus.hava.ui.weather.SkyBackground
import com.kampplus.hava.ui.weather.SkyPalette
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.roundToInt

private val ScreenPadding = 20.dp

/**
 * Bir yerin hava tahmini: hero, saatlik, günlük ve ayrıntı kartları.
 *
 * Hem "Bugün" sekmesi hem de şehir detayı bu ekranı kullanır; fark yalnızca üst çubuktaki
 * düğmelerdir ([navigationIcon], [actions] slotları). Arka plan hava durumuna göre değişir.
 *
 * State tutmaz: durum yukarıdan gelir, olaylar yukarı bildirilir (state hoisting).
 *
 * @param darkTheme Koyu temada gökyüzü kısılır.
 * @param bottomPadding Alt gezinme çubuğunun kapladığı yükseklik.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastScreen(
    uiState: ForecastUiState,
    onRetry: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
    darkTheme: Boolean = false,
    bottomPadding: Dp = 0.dp,
    snackbarHostState: SnackbarHostState = SnackbarHostState(),
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    val palette = skyFor(uiState.forecast, darkTheme)
    SkyBackground(palette = palette, modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                isFavorite = uiState.isFavorite,
                showPlaceActions = uiState.forecast is LoadState.Success,
                onToggleFavorite = onToggleFavorite,
                onShare = onShare,
                navigationIcon = navigationIcon,
                actions = actions
            )
            // Dört durum: her biri için ayrı görünüm.
            when (val forecast = uiState.forecast) {
                LoadState.Loading -> Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = ScreenPadding)
                ) {
                    ForecastSkeleton()
                }
                is LoadState.Success -> PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    ForecastContent(
                        place = uiState.place,
                        forecast = forecast.data,
                        airQuality = uiState.airQuality,
                        bottomPadding = bottomPadding
                    )
                }
                LoadState.Empty -> Unit
                is LoadState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorView(message = forecast.message, onRetry = onRetry)
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding + 8.dp)
        )
    }
}

private fun skyFor(forecast: LoadState<Forecast>, dark: Boolean): SkyPalette = when (forecast) {
    is LoadState.Success -> {
        val current = forecast.data.current
        SkyPalette.of(WeatherCondition.fromWeatherCode(current.weatherCode), current.isDay, dark)
    }
    else -> SkyPalette.calm(dark)
}

@Composable
private fun TopBar(
    isFavorite: Boolean,
    showPlaceActions: Boolean,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        navigationIcon()
        Spacer(Modifier.weight(1f))
        actions()
        if (showPlaceActions) {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(
                        if (isFavorite) R.string.action_favorite_remove else R.string.action_favorite_add
                    )
                )
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.action_share))
            }
        }
    }
}

/** Üst çubuktaki yuvarlak "geri" düğmesi (detay ekranı için). */
@Composable
fun BackButton(onBack: () -> Unit) {
    IconButton(
        onClick = onBack,
        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
    }
}

@Composable
private fun ForecastContent(place: Place?, forecast: Forecast, airQuality: AirQuality?, bottomPadding: Dp, modifier: Modifier = Modifier) {
    val unit = LocalTemperatureUnit.current
    val advice = remember(forecast) { forecast.buildAdvice() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Hero(place = place, forecast = forecast, unit = unit)
        AdviceCard(advice)
        HourlyCard(forecast = forecast, unit = unit)
        TemperatureChartCard(forecast = forecast, unit = unit)
        DailyCard(daily = forecast.daily, unit = unit)
        DetailGrid(forecast = forecast)
        AirQualityCard(airQuality)
        ActivityPlannerCard(forecast)
        SkyCard(forecast)
        Spacer(Modifier.height(bottomPadding + 16.dp))
    }
}

@Composable
private fun Hero(place: Place?, forecast: Forecast, unit: TemperatureUnit) {
    val current = forecast.current
    val condition = WeatherCondition.fromWeatherCode(current.weatherCode)
    val today = forecast.daily.firstOrNull()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (place != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (place.isCurrentLocation) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(22.dp))
                }
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                text = if (place.isCurrentLocation) stringResource(R.string.my_location) else place.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalContentColor.current.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
        }
        Text(
            text = condition.emoji(current.isDay),
            fontSize = 76.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = unit.format(current.temperatureC),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 96.sp, fontWeight = FontWeight.Thin)
        )
        Text(text = condition.label, style = MaterialTheme.typography.titleLarge)
        if (today != null) {
            Text(
                text = stringResource(
                    R.string.high_low,
                    unit.format(today.maxTemperatureC),
                    unit.format(today.minTemperatureC)
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = LocalContentColor.current.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun HourlyCard(forecast: Forecast, unit: TemperatureUnit) {
    val hours = forecast.upcomingHours()
    if (hours.isEmpty()) return
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.detail_hourly))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            items(items = hours, key = { it.time.toString() }) { hour ->
                HourItem(hour = hour, isNow = hour === hours.first(), isDay = forecast.isDay(hour.time), unit = unit)
            }
        }
    }
}

@Composable
private fun HourItem(hour: HourlyForecast, isNow: Boolean, isDay: Boolean, unit: TemperatureUnit) {
    val condition = WeatherCondition.fromWeatherCode(hour.weatherCode)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = if (isNow) stringResource(R.string.now) else hour.time.hourLabel(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isNow) FontWeight.Bold else FontWeight.Normal
        )
        Text(text = condition.emoji(isDay), fontSize = 26.sp)
        Text(text = unit.format(hour.temperatureC), style = MaterialTheme.typography.titleMedium)
        val rain = hour.precipitationProbability
        Text(
            text = if (rain != null && rain >= RAIN_LABEL_MIN_PERCENT) "%$rain" else " ",
            style = MaterialTheme.typography.labelSmall,
            color = RainBlue
        )
    }
}

@Composable
private fun DailyCard(daily: List<DailyForecast>, unit: TemperatureUnit) {
    if (daily.isEmpty()) return
    val weekMin = daily.minOf { it.minTemperatureC }
    val weekMax = daily.maxOf { it.maxTemperatureC }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.detail_daily))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            daily.forEachIndexed { index, day ->
                DailyRow(day = day, isToday = index == 0, weekMin = weekMin, weekMax = weekMax, unit = unit)
            }
        }
    }
}

@Composable
private fun DailyRow(day: DailyForecast, isToday: Boolean, weekMin: Double, weekMax: Double, unit: TemperatureUnit) {
    val condition = WeatherCondition.fromWeatherCode(day.weatherCode)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = if (isToday) stringResource(R.string.today) else day.date.dayLabel(),
            modifier = Modifier.width(DAY_NAME_WIDTH),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1
        )
        Text(text = condition.emoji, fontSize = 22.sp, modifier = Modifier.width(30.dp))
        Text(
            text = unit.format(day.minTemperatureC),
            modifier = Modifier.width(38.dp),
            textAlign = TextAlign.End,
            color = LocalContentColor.current.copy(alpha = 0.7f)
        )
        RangeBar(
            min = day.minTemperatureC,
            max = day.maxTemperatureC,
            weekMin = weekMin,
            weekMax = weekMax,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = unit.format(day.maxTemperatureC),
            modifier = Modifier.width(38.dp),
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Haftalık aralık içinde o günün sıcaklık aralığını gösteren çubuk. */
@Composable
private fun RangeBar(min: Double, max: Double, weekMin: Double, weekMax: Double, modifier: Modifier = Modifier) {
    val span = (weekMax - weekMin).coerceAtLeast(1.0)
    val start = ((min - weekMin) / span).toFloat()
    val end = ((max - weekMin) / span).toFloat().coerceAtLeast(start + MIN_RANGE_FRACTION)
    Canvas(modifier = modifier.height(6.dp)) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(Color.White.copy(alpha = 0.22f), size = size, cornerRadius = radius)
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(ColdBlue, WarmOrange)),
            topLeft = Offset(size.width * start, 0f),
            size = Size(size.width * (end - start).coerceAtMost(1f - start), size.height),
            cornerRadius = radius
        )
    }
}

@Composable
private fun DetailGrid(forecast: Forecast) {
    val current = forecast.current
    val today = forecast.daily.firstOrNull()
    val unit = LocalTemperatureUnit.current
    val tiles = buildList {
        current.apparentTemperatureC?.let { add(Tile("🌡️", stringResource(R.string.detail_feels_like), unit.format(it))) }
        current.humidityPercent?.let { add(Tile("💧", stringResource(R.string.detail_humidity), "%$it")) }
        current.windSpeedKmh?.let { add(Tile("💨", stringResource(R.string.detail_wind), "${it.roundToInt()} km/sa")) }
        current.pressureHpa?.let { add(Tile("🧭", stringResource(R.string.detail_pressure), "${it.roundToInt()} hPa")) }
        today?.uvIndexMax?.let { add(Tile("🕶️", stringResource(R.string.detail_uv), "${it.roundToInt()} · ${uvLabel(it)}")) }
        today?.precipitationProbability?.let { add(Tile("☔", stringResource(R.string.detail_precipitation), "%$it")) }
        today?.sunrise?.let { add(Tile("🌅", stringResource(R.string.detail_sunrise), it.hourLabel())) }
        today?.sunset?.let { add(Tile("🌇", stringResource(R.string.detail_sunset), it.hourLabel())) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { tile -> DetailTile(tile, Modifier.weight(1f)) }
                // Tek kalan kutu yarım genişlikte kalsın diye boşluk eklenir.
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private data class Tile(val emoji: String, val label: String, val value: String)

@Composable
private fun DetailTile(tile: Tile, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = tile.emoji, fontSize = 16.sp)
            Text(
                text = tile.label,
                style = MaterialTheme.typography.labelMedium,
                color = LocalContentColor.current.copy(alpha = 0.75f)
            )
        }
        Text(
            text = tile.value,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = LocalContentColor.current.copy(alpha = 0.75f),
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

private val RainBlue = Color(0xFF9AD0FF)
private val ColdBlue = Color(0xFF7CD4FF)
internal val WarmOrange = Color(0xFFFFB347)
private val DAY_NAME_WIDTH = 88.dp
private const val RAIN_LABEL_MIN_PERCENT = 20
private const val MIN_RANGE_FRACTION = 0.06f

// ---------- Önizlemeler ----------

private fun sampleForecast(code: Int = 2, isDay: Boolean = true): Forecast {
    val now = LocalDateTime.of(2026, 7, 14, 14, 0)
    return Forecast(
        current = CurrentWeather(24.0, code, now, 25.0, 52, 14.0, isDay, 1013.0),
        hourly = List(24) { HourlyForecast(now.plusHours(it.toLong()), 24.0 - it * 0.4, code, (it * 4) % 70) },
        daily = List(7) {
            DailyForecast(
                LocalDate.of(2026, 7, 14).plusDays(it.toLong()),
                14.0 + it,
                24.0 + it % 3 * 2,
                code,
                30,
                now.withHour(5).withMinute(42),
                now.withHour(20).withMinute(31),
                6.0
            )
        }
    )
}

@Preview(heightDp = 1400)
@Composable
private fun ForecastScreenPreview() {
    HavaTheme {
        ForecastScreen(
            uiState = ForecastUiState(
                Place("Kocaeli", "Kocaeli, Türkiye", 40.76, 29.94),
                LoadState.Success(sampleForecast()),
                isFavorite = true
            ),
            onRetry = {},
            onToggleFavorite = {},
            onShare = {}
        )
    }
}

@Preview
@Composable
private fun ForecastLoadingPreview() {
    HavaTheme { ForecastScreen(ForecastUiState(), onRetry = {}, onToggleFavorite = {}, onShare = {}) }
}

@Preview
@Composable
private fun ForecastErrorPreview() {
    HavaTheme {
        ForecastScreen(
            ForecastUiState(forecast = LoadState.Error("İnternet bağlantını kontrol et.")),
            onRetry = {},
            onToggleFavorite = {},
            onShare = {}
        )
    }
}
