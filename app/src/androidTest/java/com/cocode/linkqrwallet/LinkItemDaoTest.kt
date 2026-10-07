package com.cocode.linkqrwallet

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cocode.linkqrwallet.data.LinkDatabase
import com.cocode.linkqrwallet.data.LinkItem
import com.cocode.linkqrwallet.data.LinkItemDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the real SQL of [LinkItemDao.replaceTitle] on a real Room database. */
@RunWith(AndroidJUnit4::class)
class LinkItemDaoTest {
    private lateinit var db: LinkDatabase
    private lateinit var dao: LinkItemDao

    @Before
    fun open() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, LinkDatabase::class.java).build()
        dao = db.linkItemDao()
    }

    @After
    fun close() {
        db.close()
    }

    private suspend fun saved(): Long = dao.insert(
        LinkItem(url = "https://example.com", title = "example.com", domain = "example.com", createdAt = 100, updatedAt = 100)
    )

    private suspend fun titleOf(id: Long) = dao.observeById(id).first()!!.title

    @Test
    fun replacesTheTitleOfALinkThatIsStillAsSaved() = runBlocking {
        val id = saved()
        dao.replaceTitle(id, expected = "example.com", savedAt = 100, title = "Example Domain")
        assertEquals("Example Domain", titleOf(id))
    }

    @Test
    fun keepsATitleThatWasRenamedAndThenRenamedBack() = runBlocking {
        val id = saved()
        // two renames, each bumping updatedAt, the second back to the domain
        dao.update(dao.observeById(id).first()!!.copy(title = "My site", updatedAt = 200))
        dao.update(dao.observeById(id).first()!!.copy(title = "example.com", updatedAt = 300))
        dao.replaceTitle(id, expected = "example.com", savedAt = 100, title = "Example Domain")
        assertEquals("example.com", titleOf(id))
    }

    @Test
    fun keepsATitleThatIsNoLongerTheDomain() = runBlocking {
        val id = saved()
        dao.update(dao.observeById(id).first()!!.copy(title = "My site", updatedAt = 200))
        dao.replaceTitle(id, expected = "example.com", savedAt = 100, title = "Example Domain")
        assertEquals("My site", titleOf(id))
    }
}
