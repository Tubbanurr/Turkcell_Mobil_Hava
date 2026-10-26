package com.kampplus.hava.ui.forecast

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kampplus.hava.model.Activity
import com.kampplus.hava.model.Advice
import com.kampplus.hava.model.AirQuality
import com.kampplus.hava.model.Forecast
import com.kampplus.hava.model.TemperatureUnit
import com.kampplus.hava.model.moonPhaseOn
import com.kampplus.hava.model.nextMeteorShower
import com.kampplus.hava.model.planActivity
import com.kampplus.hava.model.stargazingTonight
import com.kampplus.hava.ui.component.GlassCard
import kotlin.math.roundToInt

/** "Ne giymeli, yanıma ne almalı?" kartı. Öneriler [com.kampplus.hava.model.buildAdvice] ile üretilir. */
@Composable
internal fun AdviceCard(advice: List<Advice>) {
    if (advice.isEmpty()) return
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle("Bugün için öneriler")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            advice.forEach { item ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Text(text = item.emoji, fontSize = 26.sp)
                    Column {
                        Text(text = item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = item.detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalContentColor.current.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

/** Önümüzdeki 24 saatin sıcaklığını yumuşak bir eğri olarak çizer (Canvas). */
@Composable
internal fun TemperatureChartCard(forecast: Forecast, unit: TemperatureUnit) {
    val hours = forecast.upcomingHours(CHART_HOURS)
    if (hours.size < MIN_CHART_POINTS) return
    val measurer = rememberTextMeasurer()
    val textColor = LocalContentColor.current
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = textColor)
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle("24 saatlik sıcaklık eğrisi")
        Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
            val topPad = 24.dp.toPx()
            val bottomPad = 20.dp.toPx()
            val sidePad = 16.dp.toPx()
            val temps = hours.map { it.temperatureC }
            val min = temps.min()
            val span = (temps.max() - min).coerceAtLeast(1.0)
            val chartHeight = size.height - topPad - bottomPad
            val stepX = (size.width - 2 * sidePad) / (hours.size - 1)
            fun xAt(index: Int) = sidePad + index * stepX
            fun yAt(temp: Double) = topPad + chartHeight * (1 - ((temp - min) / span)).toFloat()

            val line = Path().apply {
                moveTo(xAt(0), yAt(temps[0]))
                for (i in 1 until temps.size) {
                    val midX = (xAt(i - 1) + xAt(i)) / 2
                    cubicTo(midX, yAt(temps[i - 1]), midX, yAt(temps[i]), xAt(i), yAt(temps[i]))
                }
            }
            val baseline = topPad + chartHeight
            val area = Path().apply {
                addPath(line)
                lineTo(xAt(temps.lastIndex), baseline)
                lineTo(xAt(0), baseline)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(WarmOrange.copy(alpha = 0.35f), Color.Transparent), topPad, baseline))
            drawPath(line, WarmOrange, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

            hours.forEachIndexed { index, hour ->
                if (index % LABEL_EVERY != 0 && index != temps.lastIndex) return@forEachIndexed
                val x = xAt(index)
                val y = yAt(hour.temperatureC)
                drawCircle(Color.White, radius = 3.5.dp.toPx(), center = Offset(x, y))
                val temp = measurer.measure(unit.format(hour.temperatureC), labelStyle)
                drawText(
                    temp,
                    topLeft = Offset(
                        (x - temp.size.width / 2f).coerceIn(0f, size.width - temp.size.width),
                        y - temp.size.height - 6.dp.toPx()
                    )
                )
                val time = measurer.measure(if (index == 0) "Şimdi" else hour.time.hourLabel(), labelStyle)
                drawText(
                    time,
                    topLeft = Offset(
                        (x - time.size.width / 2f).coerceIn(0f, size.width - time.size.width),
                        size.height - time.size.height
                    )
                )
            }
        }
    }
}

/** Seçilen aktivite için önümüzdeki günlerin en iyi saatlerini puanlar. */
@Composable
internal fun ActivityPlannerCard(forecast: Forecast) {
    var selected by rememberSaveable { mutableStateOf(Activity.RUNNING) }
    val slots = remember(forecast, selected) { forecast.planActivity(selected) }
    val today = forecast.current.observedAt.toLocalDate()
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle("Aktivite planlayıcı")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Activity.entries.forEach { activity ->
                FilterChip(
                    selected = activity == selected,
                    onClick = { selected = activity },
                    label = { Text("${activity.emoji} ${activity.label}", maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        labelColor = Color.White,
                        selectedContainerColor = Color.White.copy(alpha = 0.30f),
                        selectedLabelColor = Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = activity == selected,
                        borderColor = Color.White.copy(alpha = 0.35f),
                        selectedBorderColor = Color.White.copy(alpha = 0.6f)
                    )
                )
            }
        }
        Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (slots.isEmpty()) {
                Text(
                    text = "Önümüzdeki günlerde ${selected.label.lowercase()} için uygun saat bulunamadı.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            slots.forEachIndexed { index, slot ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${slot.time.toLocalDate().relativeDayLabel(today)} ${slot.time.hourLabel()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal
                        )
                        val rain = slot.precipitationProbability?.let { " · Yağış %$it" }.orEmpty()
                        Text(
                            text = "${slot.verdict} · ${slot.temperatureC.roundToInt()}°$rain",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalContentColor.current.copy(alpha = 0.8f)
                        )
                    }
                    ScoreBar(score = slot.score, modifier = Modifier.width(88.dp))
                    Text(text = "${slot.score}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(30.dp))
                }
            }
        }
    }
}

@Composable
private fun ScoreBar(score: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.height(6.dp)) {
        val radius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
        drawRoundRect(Color.White.copy(alpha = 0.22f), size = size, cornerRadius = radius)
        drawRoundRect(
            color = if (score >= 60) {
                Color(0xFF7DDC8F)
            } else if (score >= 40) {
                WarmOrange
            } else {
                Color(0xFFFF7A7A)
            },
            size = androidx.compose.ui.geometry.Size(size.width * score / 100f, size.height),
            cornerRadius = radius
        )
    }
}

/** Ay evresi, bu gecenin yıldız gözlemi puanı ve yaklaşan meteor yağmuru. */
@Composable
internal fun SkyCard(forecast: Forecast) {
    val date = forecast.current.observedAt.toLocalDate()
    val stargazing = remember(forecast) { forecast.stargazingTonight() }
    val moon = stargazing?.moon ?: moonPhaseOn(date)
    val shower = remember(date) { nextMeteorShower(date) }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle("Gökyüzü ve astronomi")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(text = moon.emoji, fontSize = 44.sp)
            Column {
                Text(text = moon.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Ay %${(moon.illumination * 100).roundToInt()} aydınlık",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current.copy(alpha = 0.8f)
                )
            }
        }
        Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (stargazing != null) {
                val cloud = stargazing.avgCloudPercent?.let { " · Gece bulutluluğu %$it" }.orEmpty()
                Text("⭐ Yıldız gözlemi bu gece: ${stargazing.verdict} (${stargazing.score}/100)$cloud")
            }
            if (shower != null) {
                val whenText = if (shower.daysUntil == 0L) "bugün" else "${shower.daysUntil} gün sonra"
                Text("☄️ ${shower.shower.label} meteor yağmuru $whenText tepe yapıyor.")
            }
            Text(
                text = "Işık kirliliği verisi yok; şehir ışıklarından uzak bir yer sonucu iyileştirir.",
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current.copy(alpha = 0.7f)
            )
        }
    }
}

/** Hava kalitesi (AQI), toz ve varsa polen. Veri yoksa kart hiç gösterilmez. */
@Composable
internal fun AirQualityCard(airQuality: AirQuality?) {
    if (airQuality == null) return
    val level = airQuality.level
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle("Hava kalitesi ve polen")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = level.emoji, fontSize = 30.sp)
            Column {
                Text(
                    text = "${level.label} · AQI ${airQuality.europeanAqi}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = level.advice,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current.copy(alpha = 0.8f)
                )
            }
        }
        val particles = listOfNotNull(
            airQuality.pm25?.let { "PM2.5 ${it.roundToInt()} µg/m³" },
            airQuality.pm10?.let { "PM10 ${it.roundToInt()} µg/m³" },
            airQuality.dust?.let { "Toz ${it.roundToInt()} µg/m³" }
        )
        if (particles.isNotEmpty()) {
            Text(
                text = particles.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 10.dp)
            )
        }
        if (airQuality.pollen.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                airQuality.pollen.forEach { reading ->
                    Row {
                        Text("${reading.emoji} ${reading.name}", modifier = Modifier.weight(1f))
                        Text("${reading.level} (${reading.grains.roundToInt()} tanecik/m³)")
                    }
                }
            }
        }
    }
}

private const val CHART_HOURS = 24
private const val MIN_CHART_POINTS = 3
private const val LABEL_EVERY = 4
