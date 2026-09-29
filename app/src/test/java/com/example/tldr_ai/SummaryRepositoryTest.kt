package com.example.tldr_ai

import com.example.tldr_ai.data.repository.SummaryRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SummaryRepositoryTest {

    @Test
    fun fetch_emptyUrl_returnsFailureWithIllegalArgumentException() = runBlocking {
        val result = SummaryRepository().fetch("")

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
        assertFalse(exception?.message.isNullOrBlank())
    }

    @Test
    fun fetch_blankUrlOfSpaces_returnsFailureWithIllegalArgumentException() = runBlocking {
        val result = SummaryRepository().fetch("   ")

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
        assertFalse(exception?.message.isNullOrBlank())
    }

    @Test
    fun fetch_blankUrlOfWhitespace_returnsFailureWithIllegalArgumentException() = runBlocking {
        val result = SummaryRepository().fetch("\t\n")

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
        assertFalse(exception?.message.isNullOrBlank())
    }
}
