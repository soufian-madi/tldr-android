package com.example.tldr_ai

import com.example.tldr_ai.data.model.InputMode
import com.example.tldr_ai.data.model.detectInputMode
import com.example.tldr_ai.data.model.normalizeUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class SummaryResultTest {

    @Test
    fun detectInputMode_recognizesBareAndFullUrls() {
        assertEquals(InputMode.URL, detectInputMode("example.com"))
        assertEquals(InputMode.URL, detectInputMode("https://example.com/article?x=1#frag"))
    }

    @Test
    fun detectInputMode_recognizesUrlWithPort() {
        assertEquals(InputMode.URL, detectInputMode("example.com:8080/path"))
    }

    @Test
    fun detectInputMode_treatsProseContainingAUrlAsText() {
        assertEquals(InputMode.TEXT, detectInputMode("check out example.com please"))
    }

    @Test
    fun detectInputMode_treatsEmptyOrBlankInputAsText() {
        assertEquals(InputMode.TEXT, detectInputMode(""))
        assertEquals(InputMode.TEXT, detectInputMode("   "))
    }

    @Test
    fun detectInputMode_treatsSingleWordWithoutDotAsText() {
        assertEquals(InputMode.TEXT, detectInputMode("hello"))
    }

    @Test
    fun detectInputMode_isCaseInsensitiveForTld() {
        assertEquals(InputMode.URL, detectInputMode("EXAMPLE.COM"))
    }

    @Test
    fun detectInputMode_trimsSurroundingWhitespaceBeforeMatching() {
        assertEquals(InputMode.URL, detectInputMode("  https://example.com/article  "))
    }

    @Test
    fun normalizeUrl_prependsHttpsToBareDomain() {
        assertEquals("https://example.com", normalizeUrl("example.com"))
    }

    @Test
    fun normalizeUrl_leavesExistingSchemeUntouched() {
        assertEquals("http://example.com", normalizeUrl("http://example.com"))
        assertEquals("https://example.com", normalizeUrl("https://example.com"))
        assertEquals("HTTPS://example.com", normalizeUrl("HTTPS://example.com"))
    }

    @Test
    fun normalizeUrl_trimsWhitespaceBeforeCheckingScheme() {
        assertEquals("https://example.com", normalizeUrl("  example.com  "))
        assertEquals("https://example.com", normalizeUrl("  https://example.com  "))
    }
}
