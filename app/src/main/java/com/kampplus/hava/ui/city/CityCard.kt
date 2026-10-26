package com.kampplus.hava.ui.city

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kampplus.hava.model.City
import com.kampplus.hava.model.Place
import com.kampplus.hava.ui.component.GlassCard
import com.kampplus.hava.ui.theme.HavaTheme
import com.kampplus.hava.ui.weather.SkyBackground
import com.kampplus.hava.ui.weather.SkyPalette

/**
 * Arama sonucundaki tek bir şehir satırı. Ayrıntılar [PlaceRow]'dadır.
 *
 * @param onClick Dokunulduğunda çağrılır. Bileşen ne olacağını bilmez, yalnızca haber verir
 *   (olay yukarı, veri aşağı).
 */
@Composable
fun CityCard(city: City, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PlaceRow(
        title = city.name,
        subtitle = city.subtitle,
        badge = city.countryCode ?: "?",
        onClick = onClick,
        modifier = modifier
    )
}

/** Favori ve geçmiş listelerindeki satır. [trailing] ile sağ tarafa düğme eklenebilir. */
@Composable
fun PlaceCard(place: Place, onClick: () -> Unit, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    PlaceRow(
        title = place.name,
        subtitle = place.subtitle,
        badge = if (place.isCurrentLocation) "📍" else place.name.take(1).uppercase(),
        onClick = onClick,
        modifier = modifier,
        trailing = trailing
    )
}

/**
 * Yapısı: `GlassCard` → `Row` → rozet + `Column(weight(1f))` + sağ ok.
 *
 * `Modifier.weight(1f)`: ortadaki sütun kalan boşluğun tamamını kaplar, sağ oku kenara iter.
 * Uzun adlar (Afyonkarahisar) rozeti ezmez, "..." ile kısaltılır.
 */
@Composable
private fun PlaceRow(
    title: String,
    subtitle: String,
    badge: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    GlassCard(modifier = modifier.fillMaxWidth(), onClick = onClick, padding = 12.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Badge(text = badge)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    // Ok yalnızca görseldir; "dokunulabilir" bilgisini kart zaten verir.
                    contentDescription = null,
                    tint = LocalContentColor.current.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun Badge(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(44.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.22f),
        contentColor = Color.White
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview
@Composable
private fun CityCardPreview() {
    HavaTheme {
        SkyBackground(SkyPalette.Calm) {
            CityCard(
                city = City(323786, "Ankara", 39.91987, 32.85427, "TR", "Türkiye", "Ankara"),
                onClick = {}
            )
        }
    }
}
