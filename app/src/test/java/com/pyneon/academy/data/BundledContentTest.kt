package com.pyneon.academy.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BundledContentTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "courses").deleteRecursively()
        LessonRepository.invalidateCache()
    }

    @Test
    fun `all bundled packs seed on first use`() {
        val centre = ContentCenter()
        assertEquals(24, centre.seedBundledPacks(context))
        assertEquals(24, centre.installedVersions(context).size)
        assertTrue(centre.installedVersions(context).values.all { it == 2 })

        val lessons = LessonRepository.lessons(context)
        assertEquals(245, lessons.size)
        assertEquals(39, LessonRepository.mainline(context).size)
        assertEquals(22, LessonRepository.challenges(context).size)
    }

    @Test
    fun `bundled catalog works without network`() {
        val catalog = ContentCenter().bundledCatalog(context)
        assertNotNull(catalog)
        assertEquals(24, catalog!!.packs.size)
        assertEquals("2026-11-06", catalog.updatedAt)
    }

    @Test
    fun `seeding is idempotent`() {
        val centre = ContentCenter()
        assertEquals(24, centre.seedBundledPacks(context))
        assertEquals(0, centre.seedBundledPacks(context))
        assertEquals(24, centre.installedVersions(context).size)
    }

    @Test
    fun `newer downloaded pack is not overwritten by bundled version`() {
        val dir = ContentCenter().packsDir(context)
        File(dir, "js-basics.v3.json").writeText("[]")
        val centre = ContentCenter()
        centre.seedBundledPacks(context)
        assertEquals(3, centre.installedVersions(context)["js-basics"])
        assertFalse(File(dir, "js-basics.v2.json").exists())
    }

    @Test
    fun `tracks see preinstalled packs without opening language centre`() {
        val tracks = TrackRepository.tracks(context)
        assertEquals(9, tracks.size)
        assertTrue(tracks.all { it.status == TrackStatus.READY })
    }
}
