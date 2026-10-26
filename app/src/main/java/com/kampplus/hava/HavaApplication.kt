package com.kampplus.hava

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.kampplus.hava.di.AppContainer

/**
 * Uygulama süreci başlarken Android'in oluşturduğu ilk nesne (Activity'den de önce).
 * Ortak nesneleri tutan [AppContainer] burada kurulur; manifestte `android:name` ile bağlanır.
 */
class HavaApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        container.notifier.ensureChannel()
    }
}

/** ViewModel fabrikalarında uygulamanın bağımlılık kabına ulaşmak için. */
fun CreationExtras.appContainer(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HavaApplication).container
