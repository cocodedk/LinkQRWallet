package com.cocode.linkqrwallet

import com.cocode.linkqrwallet.data.LinkItem
import com.cocode.linkqrwallet.data.LinkRepository
import com.cocode.linkqrwallet.ui.viewmodel.AddLinkViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The page title is read only after a save. Until then the app must contact nothing. */
@OptIn(ExperimentalCoroutinesApi::class)
class AddLinkViewModelTest {
    private val scheduler = TestCoroutineScheduler()
    private val dao = FakeLinkDao()
    private val noop: (Long) -> Unit = {}
    private var savedId: Long? = null

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(fetcher: RecordingTitleFetcher) = AddLinkViewModel(
        LinkRepository(dao),
        fetcher,
        TestScope(StandardTestDispatcher(scheduler))
    )

    private fun runAllWork() {
        scheduler.advanceTimeBy(60_000)
        scheduler.runCurrent()
    }

    @Test
    fun typingAnAddressContactsNothing() {
        val fetcher = RecordingTitleFetcher()
        val model = viewModel(fetcher)
        val address = "https://example.com/page"
        for (length in 1..address.length) {
            model.updateUrl(address.take(length))
            scheduler.advanceTimeBy(500)
        }
        runAllWork()
        assertTrue(fetcher.asked.isEmpty())
        assertTrue(dao.items.isEmpty())
    }

    @Test
    fun aScannedOrSharedAddressContactsNothingBeforeSave() {
        val fetcher = RecordingTitleFetcher()
        val model = viewModel(fetcher)
        model.updateUrl("https://example.com/from-a-qr-code")
        runAllWork()
        assertEquals("example.com", model.uiState.value.domain)
        assertTrue(fetcher.asked.isEmpty())
    }

    @Test
    fun savesAtOnceWithTheDomainThenReadsTheTitle() {
        val gate = CompletableDeferred<Unit>()
        val fetcher = RecordingTitleFetcher("Example Domain", gate)
        val model = viewModel(fetcher)
        model.updateUrl("https://www.example.com/page")
        model.validateAndSave({ savedId = it }, {})
        scheduler.runCurrent()

        assertNotNull(savedId)
        assertEquals("example.com", dao.items.single().title)
        assertEquals(listOf("https://www.example.com/page"), fetcher.asked)

        gate.complete(Unit)
        runAllWork()
        assertEquals("Example Domain", dao.items.single().title)
    }

    @Test
    fun aTitleTheUserTypedIsNeverReplacedOrFetched() {
        val fetcher = RecordingTitleFetcher("Example Domain")
        val model = viewModel(fetcher)
        model.updateUrl("https://example.com")
        model.updateTitle("My bank")
        model.validateAndSave(noop, {})
        runAllWork()

        assertEquals("My bank", dao.items.single().title)
        assertTrue(fetcher.asked.isEmpty())
    }

    @Test
    fun aTitleChangedWhileTheFetchRunsIsKept() {
        val gate = CompletableDeferred<Unit>()
        val model = viewModel(RecordingTitleFetcher("Example Domain", gate))
        model.updateUrl("https://example.com")
        model.validateAndSave(noop, {})
        scheduler.runCurrent()
        dao.items[0] = dao.items[0].copy(title = "Renamed by me")

        gate.complete(Unit)
        runAllWork()
        assertEquals("Renamed by me", dao.items.single().title)
    }

    @Test
    fun aFailedFetchKeepsTheDomainWithoutAnError() {
        val model = viewModel(RecordingTitleFetcher(answer = null))
        model.updateUrl("https://example.com")
        model.validateAndSave(noop, {})
        runAllWork()

        assertEquals("example.com", dao.items.single().title)
        assertNull(model.uiState.value.errorMessage)
    }

    @Test
    fun anUnsafeAddressIsNeitherSavedNorFetched() {
        val fetcher = RecordingTitleFetcher()
        val model = viewModel(fetcher)
        model.updateUrl("http://192.168.1.10/admin")
        model.validateAndSave(noop, {})
        model.saveDuplicateAllowed(noop)
        runAllWork()

        assertNotNull(model.uiState.value.errorMessage)
        assertTrue(dao.items.isEmpty())
        assertTrue(fetcher.asked.isEmpty())
    }

    @Test
    fun aDuplicateIsFetchedOnlyOnceTheUserConfirmsIt() {
        dao.items.add(
            LinkItem(id = 1, url = "https://example.com", title = "Kept", domain = "example.com", createdAt = 1, updatedAt = 1)
        )
        val fetcher = RecordingTitleFetcher("Example Domain")
        val model = viewModel(fetcher)
        model.updateUrl("https://example.com")
        model.validateAndSave(noop, {})
        runAllWork()
        assertTrue(fetcher.asked.isEmpty())

        model.saveDuplicateAllowed(noop)
        runAllWork()
        assertEquals(listOf("https://example.com"), fetcher.asked)
        assertEquals(listOf("Kept", "Example Domain"), dao.items.map { it.title })
    }
}
