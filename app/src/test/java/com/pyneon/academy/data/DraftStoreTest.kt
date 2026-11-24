package com.pyneon.academy.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DraftStoreTest {

    private lateinit var context: Context

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        ProgressStore.reset(context)
    }

    @Test
    fun `draft survives a new screen`() = runBlocking {
        DraftStore.save(context, "lesson_l01", "print('hello')")
        assertEquals("print('hello')", DraftStore.load(context, "lesson_l01"))
    }

    @Test
    fun `blank draft is removed instead of restored`() = runBlocking {
        DraftStore.save(context, "lesson_l01", "print(1)")
        DraftStore.save(context, "lesson_l01", "  \n")
        assertNull(DraftStore.load(context, "lesson_l01"))
    }

    @Test
    fun `reset clears drafts`() = runBlocking {
        DraftStore.save(context, "challenge_c01", "def solve(): pass")
        ProgressStore.reset(context)
        assertNull(DraftStore.load(context, "challenge_c01"))
    }
}
