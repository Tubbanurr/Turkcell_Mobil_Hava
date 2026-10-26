package com.kampplus.hava.ui.city

import com.kampplus.hava.testing.FakeCityRepository
import com.kampplus.hava.testing.FakeFavoritesRepository
import com.kampplus.hava.testing.FakeHistoryRepository
import com.kampplus.hava.testing.MainDispatcherRule
import com.kampplus.hava.testing.sampleCity
import com.kampplus.hava.testing.samplePlace
import com.kampplus.hava.ui.LoadState
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Şehir arama, favori ve geçmiş davranışlarını doğrular. */
@OptIn(ExperimentalCoroutinesApi::class)
class CityViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeCityRepository()
    private val favorites = FakeFavoritesRepository()
    private val history = FakeHistoryRepository()

    private fun createViewModel() = CityViewModel(repository, favorites, history)

    @Test
    fun `baslangicta arama yapilmaz`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.results)
        assertEquals(0, repository.callCount)
    }

    @Test
    fun `yazmayi birakinca debounce sonrasi arar`() = runTest {
        repository.result = listOf(sampleCity())
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onQueryChange("Ank")
        advanceTimeBy(CityViewModel.SEARCH_DEBOUNCE_MS - 1)
        assertEquals(0, repository.callCount)

        advanceUntilIdle()
        assertEquals(1, repository.callCount)
        assertTrue(viewModel.uiState.value.results is LoadState.Success)
    }

    @Test
    fun `cok kisa metin arama baslatmaz`() = runTest {
        val viewModel = createViewModel()
        viewModel.onQueryChange("A")
        advanceUntilIdle()

        assertEquals(0, repository.callCount)
    }

    @Test
    fun `bos liste Empty durumuna cevrilir`() = runTest {
        val viewModel = createViewModel()
        viewModel.onQueryChange("Zzz")
        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.uiState.value.results)
    }

    @Test
    fun `baglanti hatasi anlasilir mesaja cevrilir`() = runTest {
        repository.error = IOException("no network")
        val viewModel = createViewModel()
        viewModel.onQueryChange("Ankara")
        advanceUntilIdle()

        val state = viewModel.uiState.value.results
        assertTrue("Error bekleniyordu, gelen: $state", state is LoadState.Error)
        assertEquals("İnternet bağlantını kontrol et.", (state as LoadState.Error).message)
    }

    @Test
    fun `search beklemeden hemen arar ve tekrar dene calisir`() = runTest {
        repository.error = IOException("no network")
        val viewModel = createViewModel()
        viewModel.onQueryChange("Rize")
        advanceUntilIdle()

        repository.error = null
        repository.result = listOf(sampleCity(name = "Rize"))
        viewModel.search()
        advanceUntilIdle()

        assertEquals("Rize", repository.lastQuery)
        assertTrue(viewModel.uiState.value.results is LoadState.Success)
    }

    @Test
    fun `kutu bosaltilinca sonuclar temizlenir`() = runTest {
        repository.result = listOf(sampleCity())
        val viewModel = createViewModel()
        viewModel.onQueryChange("Ankara")
        advanceUntilIdle()

        viewModel.onQueryChange("")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.results)
    }

    @Test
    fun `acilan yer gecmise yazilir ve temizlenebilir`() = runTest {
        val viewModel = createViewModel()
        viewModel.onPlaceOpened(samplePlace("Ankara"))
        advanceUntilIdle()
        assertEquals(listOf("Ankara"), viewModel.uiState.value.history.map { it.name })

        viewModel.onClearHistory()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.history.isEmpty())
    }

    @Test
    fun `favoriler listelenir ve kaldirilir`() = runTest {
        favorites.toggle(samplePlace("İzmir"))
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.favorites.size)

        viewModel.onRemoveFavorite(samplePlace("İzmir"))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.favorites.isEmpty())
    }

    @Test
    fun `turkce karakterli sorgu degistirilmeden aranir`() = runTest {
        repository.result = listOf(sampleCity())
        val viewModel = createViewModel()

        listOf("Eskişehir", "ŞANLIURFA", "Iğdır", "İstanbul", "çanakkale", "Öğütlü").forEach { query ->
            viewModel.onQueryChange(query)
            advanceUntilIdle()
            assertEquals(query, repository.lastQuery)
        }
    }
}
