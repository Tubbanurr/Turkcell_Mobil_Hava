package com.kampplus.hava.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.kampplus.hava.model.Place
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** Telefonun şu anki konumunu [Place] olarak verir. İzin yoksa ya da konum alınamazsa null. */
interface LocationProvider {
    suspend fun currentPlace(): Place?
}

/**
 * Google Play Services'in "fused" konum istemcisi (GPS + Wi-Fi + baz istasyonu karışımı)
 * ve adı bulmak için Android'in [Geocoder]'ı.
 */
class FusedLocationProvider(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LocationProvider {

    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    @SuppressLint("MissingPermission")
    override suspend fun currentPlace(): Place? {
        if (!hasLocationPermission()) return null
        val location = try {
            client.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token
            ).await() ?: client.lastLocation.await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Konum kapalı, Play Services yok ya da zaman aşımı: çağıran tarafa "alınamadı" deriz.
            null
        } ?: return null
        return location.toPlace()
    }

    private fun hasLocationPermission() = listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    ).any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    private suspend fun Location.toPlace(): Place {
        val address = reverseGeocode(latitude, longitude)
        val name = address?.locality ?: address?.subAdminArea ?: address?.adminArea ?: "Konumum"
        val subtitle = listOfNotNull(address?.adminArea, address?.countryName).distinct().joinToString(", ")
        return Place(
            name = name,
            subtitle = subtitle.ifEmpty { "Mevcut konum" },
            latitude = latitude,
            longitude = longitude,
            isCurrentLocation = true
        )
    }

    /** Koordinattan adres bulur; internet/servis yoksa null döner ve ad "Konumum" olur. */
    private suspend fun reverseGeocode(latitude: Double, longitude: Double): Address? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.forLanguageTag("tr"))
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) = continuation.resume(addresses.firstOrNull())

                            override fun onError(errorMessage: String?) = continuation.resume(null)
                        }
                    )
                }
            } else {
                withContext(ioDispatcher) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}
