package com.shamil.cloudvault

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shamil.cloudvault.data.local.GalleryDao
import com.shamil.cloudvault.data.local.GalleryDatabase
import com.shamil.cloudvault.data.local.MediaEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class GalleryDaoTest {
    private lateinit var galleryDao: GalleryDao
    private lateinit var db: GalleryDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, GalleryDatabase::class.java).build()
        galleryDao = db.galleryDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun syncMedia_preservesFavoriteStatus() = runBlocking {
        val item1 = MediaEntity(
            id = 1L,
            name = "test1.jpg",
            uri = "content://media/external/images/media/1",
            folder = "Pictures",
            date = 1000L,
            isVideo = false,
            size = 100L,
            path = "/storage/emulated/0/Pictures/test1.jpg",
            mimeType = "image/jpeg",
            width = 100,
            height = 100,
            isFavorite = true
        )
        
        // Initial insert
        galleryDao.insertMedia(listOf(item1))
        
        // Verify it is favorite
        var items = galleryDao.getFavoriteIds()
        assertTrue("Item 1 should be favorite", items.contains(1L))

        // New sync with the same item, but coming from MediaStore (which doesn't have isFavorite)
        val item1FromMediaStore = item1.copy(isFavorite = false)
        galleryDao.syncMedia(listOf(item1FromMediaStore))
        
        // Verify it remains favorite
        items = galleryDao.getFavoriteIds()
        assertTrue("Item 1 should still be favorite after sync", items.contains(1L))
    }
}
