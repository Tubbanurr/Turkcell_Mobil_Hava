package com.kampplus.hava.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Uygulamanın Material renk paleti (gökyüzü mavisi + güneş sarısı).
 *
 * Renkler burada **isimlendirilir**, [Theme] dosyasında açık/koyu temaya dağıtılır.
 * Bileşenler sabitleri doğrudan değil `MaterialTheme.colorScheme` üzerinden okur.
 * Ekranların hava durumuna göre değişen arka planı ise ayrıca
 * [com.kampplus.hava.ui.weather.SkyPalette] içinde tanımlıdır.
 *
 * İsimlerdeki sayı tonu belirtir: 10 en koyu, 99 en açık.
 */
internal val Sky10 = Color(0xFF001B3E)
internal val Sky20 = Color(0xFF002F65)
internal val Sky30 = Color(0xFF00458F)
internal val Sky40 = Color(0xFF1E6FD9)
internal val Sky80 = Color(0xFFA8C8FF)
internal val Sky90 = Color(0xFFD6E3FF)
internal val Sun40 = Color(0xFF7D5700)
internal val Sun80 = Color(0xFFFFBA3B)
internal val Sun90 = Color(0xFFFFDEA8)
internal val Neutral10 = Color(0xFF191C20)
internal val Neutral90 = Color(0xFFE2E2E9)
internal val Neutral95 = Color(0xFFF0F0F7)
internal val Neutral99 = Color(0xFFFDFBFF)
internal val NeutralVariant30 = Color(0xFF44474F)
internal val NeutralVariant90 = Color(0xFFE0E2EC)
internal val Red40 = Color(0xFFBA1A1A)
internal val Red80 = Color(0xFFFFB4AB)
